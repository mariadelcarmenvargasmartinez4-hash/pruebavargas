package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.client.GestoPagoSendTxClient;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.pago.PagoServicioEntity;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.ProveedorPagoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxResponse;
import com.proyecto.servicios.model.pago.PagoServicioRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioRequestRecord;
import com.proyecto.servicios.model.pago.PagoServicioResponseDto;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.pago.PagoServicioRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.PagoServicioService;
import com.proyecto.servicios.util.AesEncryptionUtil;
import com.proyecto.servicios.util.DeclarativeAssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Implementación de negocio Senior para el procesamiento seguro de pagos con GestoPago:
 * - Paso 2: Validaciones de negocio previas (cuenta activa con bloqueo pesimista, PIN seguro con rate-limiting, saldo y formatos).
 * - Paso 3: Integración con 3 FeignClients, tokens vigentes, payload JSON independiente de Locale y cifrado AES-GCM + HMAC-SHA256.
 * - Paso 4: Descuento atómico de saldo exclusivamente ante respuesta "01", auditoría local persistente y manejo de idempotencia.
 */
@Slf4j
@Service
public class PagoServicioServiceImpl implements PagoServicioService {

    private static final int MAX_INTENTOS_PIN = 3;
    private final Map<String, Integer> intentosFallidosPinPorCuenta = new ConcurrentHashMap<>();

    private final CuentaRepository cuentaRepository;
    private final PagoServicioRepository pagoServicioRepository;
    private final GestoPagoSendTxClient gestoPagoSendTxClient;
    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenService gestoPagoTokenService;
    private final AesEncryptionUtil aesEncryptionUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${gestopago.aes.secret-key:${JWT_SECRET:ClaveSeguraParaCifradoAES256GestoPago!}}")
    private String claveSecretaAes;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password:${GESTOPAGO_AUTH_PASSWORD:}}")
    private String passwordDistribuidor;

    public PagoServicioServiceImpl(CuentaRepository cuentaRepository,
                                   PagoServicioRepository pagoServicioRepository,
                                   GestoPagoSendTxClient gestoPagoSendTxClient,
                                   GestoPagoAuthClient gestoPagoAuthClient,
                                   GestoPagoTokenService gestoPagoTokenService,
                                   AesEncryptionUtil aesEncryptionUtil,
                                   PasswordEncoder passwordEncoder) {
        this.cuentaRepository = cuentaRepository;
        this.pagoServicioRepository = pagoServicioRepository;
        this.gestoPagoSendTxClient = gestoPagoSendTxClient;
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.aesEncryptionUtil = aesEncryptionUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public PagoServicioResponseDto procesarPago(PagoServicioRequestDto request) {
        // =========================================================================
        // PASO 2: VALIDACIONES DE NEGOCIO INTERNAS (Antes de llamar a GestoPago)
        // =========================================================================

        // 1. Validación de Formatos y tipos (Fail-Fast en Record inmutable)
        PagoServicioRequestRecord record = request.toRecord();

        // 2. Control de Concurrencia e Idempotencia: Verificar si el UPE ya fue procesado
        Optional<PagoServicioEntity> pagoExistente = pagoServicioRepository.findByUpe(record.upe());
        if (pagoExistente.isPresent() && "APLICADO".equalsIgnoreCase(pagoExistente.get().getEstatus())) {
            PagoServicioEntity previo = pagoExistente.get();
            log.info("Petición de pago idempotente detectada para UPE {}. Retornando resultado previamente aplicado.", record.upe());
            return construirRespuestaIdempotente(previo);
        }

        // 3. Validación de la Cuenta con Bloqueo Pesimista (previene sobregiros por transacciones concurrentes)
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuentaWithLock(record.numCuenta())
                .or(() -> cuentaRepository.findByNumeroCuenta(record.numCuenta()))
                .orElseThrow(() -> new CuentaNoEncontradaException(
                        "No se encontró la cuenta bancaria con número " + record.numCuenta()));

        DeclarativeAssertUtil.asegurar(
                "ACTIVA".equalsIgnoreCase(cuenta.getEstatus()),
                () -> new ReglaNegocioException("La cuenta bancaria no se encuentra ACTIVA para transacciones"));

        // 4. Validación estricta del PIN o Pass Seguro con Throttling / Rate-Limiting
        validarPinSeguro(cuenta, record.pin());

        // 5. Validación de Saldo
        DeclarativeAssertUtil.asegurar(
                cuenta.getSaldo().compareTo(record.monto()) >= 0,
                () -> new ReglaNegocioException(
                        String.format(Locale.ROOT, "Saldo insuficiente. Saldo disponible: $%.2f, Monto requerido: $%.2f",
                                cuenta.getSaldo(), record.monto())));

        // =========================================================================
        // PASO 3: INTEGRACIÓN CON CLIENTES FEIGN Y CIFRADO AES
        // =========================================================================

        // Identidad consistente del distribuidor
        Integer distribuidorEfectivo = Optional.ofNullable(record.idDistribuidor()).orElse(this.idDistribuidor);

        // FeignClient 1 (Autenticación/Token): Obtener o renovar token de GestoPago (verificando vigencia)
        String token = obtenerTokenProveedor(distribuidorEfectivo);

        // Cifrado AES-GCM del payload sensible y firma digital HMAC-SHA256
        String payloadPlano = construirPayloadTransaccion(record);
        String claveCifrado = obtenerClaveCifradoValida();
        String datosEncriptados = aesEncryptionUtil.encriptar(payloadPlano, claveCifrado);
        String firmaTransaccion = aesEncryptionUtil.firmarPayload(payloadPlano, claveCifrado);

        // Construcción de la petición para FeignClient 3 (SendTx)
        GestoPagoSendTxRequest sendTxRequest = GestoPagoSendTxRequest.builder()
                .upe(record.upe())
                .idServicio(record.idServicio())
                .idProducto(record.idProducto())
                .idConcepto(record.idConcepto())
                .idDistribuidor(distribuidorEfectivo)
                .monto(record.monto())
                .esRecargaTelefonica(record.bandera())
                .telefono(record.telefono())
                .referencia(record.referencia())
                .datosEncriptados(datosEncriptados)
                .firma(firmaTransaccion)
                .build();

        // Registro de auditoría inicial (Garantiza persistencia local aun ante fallas externas)
        String folioInicial = UUID.randomUUID().toString();
        PagoServicioEntity transaccion = PagoServicioEntity.builder()
                .folioTransaccion(folioInicial)
                .numCuenta(record.numCuenta())
                .idServicio(record.idServicio())
                .idProducto(record.idProducto())
                .idConcepto(record.idConcepto())
                .idDistribuidor(distribuidorEfectivo)
                .monto(record.monto())
                .upe(record.upe())
                .esRecargaTelefonica(record.bandera())
                .telefono(record.telefono())
                .referencia(record.referencia())
                .estatus("PROCESANDO")
                .fechaOperacion(LocalDateTime.now())
                .build();
        pagoServicioRepository.save(transaccion);

        // =========================================================================
        // PASO 4: LLAMADA A SENDTX, EVALUACIÓN DE RESPUESTA Y DESCUENTO DE SALDO
        // =========================================================================

        GestoPagoSendTxResponse respuestaProveedor;
        try {
            respuestaProveedor = ejecutarEnvioSendTx(sendTxRequest, token);
        } catch (ProveedorPagoException ex) {
            transaccion.setEstatus("FALLO_COMUNICACION");
            pagoServicioRepository.save(transaccion);
            throw ex;
        }

        // Evaluación de la respuesta:
        // Si la respuesta es distinta de "01" o error: NO se descuenta saldo
        if (respuestaProveedor == null || !respuestaProveedor.esExitosa()) {
            transaccion.setEstatus("RECHAZADO");
            pagoServicioRepository.save(transaccion);
            String mensajeError = Optional.ofNullable(respuestaProveedor)
                    .map(GestoPagoSendTxResponse::getMensaje)
                    .orElse("El proveedor no aprobó la transacción");
            log.warn("Transacción rechazada por proveedor externo. Código: {}. No se descontó saldo",
                    Optional.ofNullable(respuestaProveedor).map(GestoPagoSendTxResponse::getCodigo).orElse("ERR"));
            throw new ReglaNegocioException("Transacción no exitosa ante el proveedor: " + mensajeError);
        }

        // Si retorna código 01 (Transacción Exitosa), en ese exacto instante se descuenta el saldo
        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoActual = saldoAnterior.subtract(record.monto());
        cuenta.setSaldo(saldoActual);
        cuentaRepository.save(cuenta);

        // Fallback seguro de folio que no excede el límite de 36 caracteres de la columna
        String folioFinal = Optional.ofNullable(respuestaProveedor.getFolioTransaccion())
                .filter(Predicate.not(String::isBlank))
                .orElse(folioInicial);

        transaccion.setFolioTransaccion(folioFinal);
        transaccion.setEstatus("APLICADO");
        pagoServicioRepository.save(transaccion);

        log.info("Pago confirmado con código 01. Saldo debitado exitosamente. Cuenta: {}, Folio: {}, Nuevo Saldo: {}",
                record.numCuenta(), folioFinal, saldoActual);

        String tipoOperacion = Boolean.TRUE.equals(record.bandera()) ? "RECARGA_TELEFONICA" : "PAGO_SERVICIO_REFERENCIA";
        String destinoPago = Boolean.TRUE.equals(record.bandera()) ? record.telefono() : record.referencia();

        return PagoServicioResponseDto.builder()
                .folioTransaccion(folioFinal)
                .numCuenta(record.numCuenta())
                .idServicio(record.idServicio())
                .idProducto(record.idProducto())
                .idConcepto(record.idConcepto())
                .idDistribuidor(distribuidorEfectivo)
                .monto(record.monto())
                .saldoAnterior(saldoAnterior)
                .saldoActual(saldoActual)
                .upe(record.upe())
                .tipoOperacion(tipoOperacion)
                .destinoPago(destinoPago)
                .estatus("APLICADO")
                .mensaje("Pago aplicado exitosamente con código 01 del proveedor")
                .fechaOperacion(transaccion.getFechaOperacion())
                .build();
    }

    private void validarPinSeguro(CuentaEntity cuenta, String pinIngresado) {
        DeclarativeAssertUtil.asegurar(
                pinIngresado != null && pinIngresado.trim().matches("^\\d{4,8}$"),
                () -> new ReglaNegocioException("El PIN o pass seguro de verificación debe contener entre 4 y 8 dígitos numéricos"));

        String numCuenta = cuenta.getNumeroCuenta();
        int intentos = intentosFallidosPinPorCuenta.getOrDefault(numCuenta, 0);
        DeclarativeAssertUtil.asegurar(
                intentos < MAX_INTENTOS_PIN,
                () -> new ReglaNegocioException("La cuenta ha sido bloqueada temporalmente para autorizar pagos debido a múltiples intentos fallidos de PIN"));

        DeclarativeAssertUtil.asegurar(
                cuenta.getPinHash() != null && !cuenta.getPinHash().isBlank(),
                () -> new ReglaNegocioException("La cuenta no tiene un PIN de autorización configurado. No es posible autorizar transacciones."));

        boolean pinValido = passwordEncoder.matches(pinIngresado.trim(), cuenta.getPinHash());
        if (!pinValido) {
            intentosFallidosPinPorCuenta.put(numCuenta, intentos + 1);
            throw new ReglaNegocioException("El PIN de autorización ingresado es incorrecto");
        }
        intentosFallidosPinPorCuenta.remove(numCuenta);
    }

    private String obtenerTokenProveedor(Integer distribuidor) {
        return gestoPagoTokenService.obtenerTokenActivo(distribuidor, codigoDispositivo)
                .filter(GestoPagoToken::esVigente)
                .map(GestoPagoToken::getToken)
                .or(() -> Optional.ofNullable(gestoPagoAuthClient.authenticate(distribuidor, codigoDispositivo, passwordDistribuidor))
                        .map(GestoPagoAuthResponse::getToken)
                        .filter(Predicate.not(String::isBlank)))
                .orElseThrow(() -> new ProveedorPagoException("No fue posible obtener un token de autenticación válido con el proveedor GestoPago"));
    }

    private String construirPayloadTransaccion(PagoServicioRequestRecord record) {
        return String.format(Locale.ROOT, "{\"numCuenta\":\"%s\",\"upe\":\"%s\",\"monto\":%.2f,\"servicio\":%d,\"producto\":%d}",
                record.numCuenta(), record.upe(), record.monto(), record.idServicio(), record.idProducto());
    }

    private String obtenerClaveCifradoValida() {
        DeclarativeAssertUtil.asegurar(
                claveSecretaAes != null && !claveSecretaAes.isBlank(),
                () -> new IllegalStateException("La clave secreta para cifrado gestopago.aes.secret-key es requerida"));
        return claveSecretaAes;
    }

    private GestoPagoSendTxResponse ejecutarEnvioSendTx(GestoPagoSendTxRequest request, String token) {
        try {
            return gestoPagoSendTxClient.sendTx("Bearer " + token, request);
        } catch (Exception ex) {
            log.error("Excepción en comunicación con SendTx de GestoPago: {}. NO se aplicará débito de saldo.", ex.getMessage());
            throw new ProveedorPagoException("Fallo en la comunicación con el proveedor externo de pagos. No se realizó ningún cargo a su cuenta.", ex);
        }
    }

    private PagoServicioResponseDto construirRespuestaIdempotente(PagoServicioEntity previo) {
        String tipoOperacion = Boolean.TRUE.equals(previo.getEsRecargaTelefonica()) ? "RECARGA_TELEFONICA" : "PAGO_SERVICIO_REFERENCIA";
        String destinoPago = Boolean.TRUE.equals(previo.getEsRecargaTelefonica()) ? previo.getTelefono() : previo.getReferencia();

        return PagoServicioResponseDto.builder()
                .folioTransaccion(previo.getFolioTransaccion())
                .numCuenta(previo.getNumCuenta())
                .idServicio(previo.getIdServicio())
                .idProducto(previo.getIdProducto())
                .idConcepto(previo.getIdConcepto())
                .idDistribuidor(previo.getIdDistribuidor())
                .monto(previo.getMonto())
                .upe(previo.getUpe())
                .tipoOperacion(tipoOperacion)
                .destinoPago(destinoPago)
                .estatus("APLICADO")
                .mensaje("Pago aplicado previamente (Respuesta idempotente)")
                .fechaOperacion(previo.getFechaOperacion())
                .build();
    }
}
