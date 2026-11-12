package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.client.GestoPagoSendTxClient;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.ProductoDocument;
import com.proyecto.servicios.entity.pago.PagoServicioEntity;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.EntidadNoProcesableException;
import com.proyecto.servicios.exception.ProveedorPagoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxResponse;
import com.proyecto.servicios.model.pago.PagoConciliacionRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioRequestRecord;
import com.proyecto.servicios.model.pago.PagoServicioResponseDto;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.mongo.ProductoMongoRepository;
import com.proyecto.servicios.repositorys.pago.PagoServicioRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.PagoServicioService;
import com.proyecto.servicios.util.AesEncryptionUtil;
import com.proyecto.servicios.util.DeclarativeAssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Implementación de negocio Senior para el procesamiento seguro de pagos con GestoPago (POST /sistema/service/sendTx.do):
 * - Control de excepciones robusto (nunca detona un 500 no controlado por llaves foráneas o datos de catálogo inexistentes).
 * - Retorna limpiamente códigos 400 Bad Request o 422 Unprocessable Entity con mensajes descriptivos.
 * - Validación de catálogo tolerante a fallos de infraestructura.
 * - Bloqueo pesimista para evitar concurrencia y sobregiros.
 * - Resiliencia financiera: ante timeouts, la transacción pasa a EN_CONCILIACION sin debitar saldo.
 */
@Slf4j
@Service
public class PagoServicioServiceImpl implements PagoServicioService {

    private static final int MAX_INTENTOS_PIN = 3;
    private static final String ESTATUS_APLICADO = "APLICADO";
    private static final String TIPO_RECARGA = "RECARGA_TELEFONICA";
    private static final String TIPO_SERVICIO = "PAGO_SERVICIO_REFERENCIA";
    private final Map<String, Integer> intentosFallidosPinPorCuenta = new ConcurrentHashMap<>();

    private final CuentaRepository cuentaRepository;
    private final PagoServicioRepository pagoServicioRepository;
    private final GestoPagoSendTxClient gestoPagoSendTxClient;
    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenService gestoPagoTokenService;
    private final AesEncryptionUtil aesEncryptionUtil;
    private final PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private ProductoMongoRepository productoMongoRepository;

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
        // Validación previa de objeto de petición
        DeclarativeAssertUtil.asegurar(request != null, () -> new ReglaNegocioException("La petición de pago no puede ser nula"));

        try {
            return ejecutarFlujoPago(request);
        } catch (ReglaNegocioException | EntidadNoProcesableException | ProveedorPagoException ex) {
            // Re-lanzar excepciones de negocio y de integración para manejo directo en GlobalExceptionHandler (400, 422, 502)
            throw ex;
        } catch (CuentaNoEncontradaException ex) {
            log.warn("Cuenta no encontrada en procesamiento de pago: {}", ex.getMessage());
            throw new EntidadNoProcesableException(ex.getMessage(), ex);
        } catch (DataIntegrityViolationException ex) {
            log.error("Violación de integridad de datos / llaves foráneas en BD: {}", ex.getMessage());
            throw new EntidadNoProcesableException("Violación de integridad: uno o más identificadores o llaves foráneas enviadas no existen o son inválidas en el sistema.", ex);
        } catch (DataAccessException ex) {
            log.error("Error de acceso o persistencia de datos al procesar pago: {}", ex.getMessage());
            throw new EntidadNoProcesableException("Error al consultar o persistir los registros requeridos para la operación de pago.", ex);
        } catch (Exception ex) {
            log.error("Error de lógica o parámetro no controlado capturado limpiamente: {}", ex.getMessage(), ex);
            throw new ReglaNegocioException("Parámetros de pago inválidos o no procesables: " + (ex.getMessage() != null ? ex.getMessage() : "Error en datos"));
        }
    }

    private PagoServicioResponseDto ejecutarFlujoPago(PagoServicioRequestDto request) {
        // =========================================================================
        // PASO 2: VALIDACIONES DE NEGOCIO INTERNAS (Antes de llamar a GestoPago)
        // =========================================================================

        // 1. Validación de Formatos y tipos (Fail-Fast en Record inmutable)
        PagoServicioRequestRecord requestRecord = request.toRecord();

        // 2. Control de Concurrencia e Idempotencia: Verificar si el UPE ya fue procesado
        Optional<PagoServicioEntity> pagoExistente = pagoServicioRepository.findByUpe(requestRecord.upe());
        if (pagoExistente.isPresent()) {
            PagoServicioEntity previo = pagoExistente.get();
            if (ESTATUS_APLICADO.equalsIgnoreCase(previo.getEstatus())) {
                log.info("Petición de pago idempotente detectada para UPE {}. Retornando resultado previamente aplicado.", requestRecord.upe());
                return construirRespuestaIdempotente(previo);
            }
            if ("EN_CONCILIACION".equalsIgnoreCase(previo.getEstatus()) || "PROCESANDO".equalsIgnoreCase(previo.getEstatus())) {
                throw new ReglaNegocioException("La transacción con UPE " + requestRecord.upe() +
                        " se encuentra actualmente en estado " + previo.getEstatus() + " (Folio: " + previo.getFolioTransaccion() +
                        "). No se puede procesar una nueva solicitud hasta que sea consultada o conciliada.");
            }
        }

        // 3. Validación de Catálogo de Productos y Restricciones de Monto
        validarProductoEnCatalogo(requestRecord.idServicio(), requestRecord.idProducto(), requestRecord.monto());

        // 4. Validación de la Cuenta con Bloqueo Pesimista (previene sobregiros por transacciones concurrentes)
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuentaWithLock(requestRecord.numCuenta())
                .or(() -> cuentaRepository.findByNumeroCuenta(requestRecord.numCuenta()))
                .orElseThrow(() -> new EntidadNoProcesableException(
                        "No se encontró la cuenta bancaria con número " + requestRecord.numCuenta() + " en la base de datos"));

        DeclarativeAssertUtil.asegurar(
                "ACTIVA".equalsIgnoreCase(cuenta.getEstatus()),
                () -> new ReglaNegocioException("La cuenta bancaria no se encuentra ACTIVA para transacciones"));

        // 5. Validación estricta del PIN o Pass Seguro con Throttling / Rate-Limiting
        validarPinSeguro(cuenta, requestRecord.pin());

        // 6. Validación de Saldo Disponible
        DeclarativeAssertUtil.asegurar(
                cuenta.getSaldo().compareTo(requestRecord.monto()) >= 0,
                () -> new ReglaNegocioException(
                        String.format(Locale.ROOT, "Saldo insuficiente. Saldo disponible: $%.2f, Monto requerido: $%.2f",
                                cuenta.getSaldo(), requestRecord.monto())));

        // =========================================================================
        // PASO 3: INTEGRACIÓN CON CLIENTES FEIGN Y CIFRADO AES
        // =========================================================================

        Integer distribuidorEfectivo = Optional.ofNullable(requestRecord.idDistribuidor()).orElse(this.idDistribuidor);

        // FeignClient 1 (Autenticación/Token): Obtener o renovar token de GestoPago (verificando vigencia)
        String token = obtenerTokenProveedor(distribuidorEfectivo);

        // Cifrado AES-GCM del payload sensible y firma digital HMAC-SHA256
        String payloadPlano = construirPayloadTransaccion(requestRecord);
        String claveCifrado = obtenerClaveCifradoValida();
        String datosEncriptados = aesEncryptionUtil.encriptar(payloadPlano, claveCifrado);
        String firmaTransaccion = aesEncryptionUtil.firmarPayload(payloadPlano, claveCifrado);

        // Construcción de la petición para FeignClient 3 (SendTx)
        GestoPagoSendTxRequest sendTxRequest = GestoPagoSendTxRequest.builder()
                .upe(requestRecord.upe())
                .idServicio(requestRecord.idServicio())
                .idProducto(requestRecord.idProducto())
                .idConcepto(requestRecord.idConcepto())
                .idDistribuidor(distribuidorEfectivo)
                .monto(requestRecord.monto())
                .esRecargaTelefonica(requestRecord.bandera())
                .telefono(requestRecord.telefono())
                .referencia(requestRecord.referencia())
                .datosEncriptados(datosEncriptados)
                .firma(firmaTransaccion)
                .build();

        // Registro de auditoría inicial (Garantiza persistencia local aun ante fallas externas)
        String folioInicial = UUID.randomUUID().toString();
        PagoServicioEntity transaccion = PagoServicioEntity.builder()
                .folioTransaccion(folioInicial)
                .numCuenta(requestRecord.numCuenta())
                .idServicio(requestRecord.idServicio())
                .idProducto(requestRecord.idProducto())
                .idConcepto(requestRecord.idConcepto())
                .idDistribuidor(distribuidorEfectivo)
                .monto(requestRecord.monto())
                .upe(requestRecord.upe())
                .esRecargaTelefonica(requestRecord.bandera())
                .telefono(requestRecord.telefono())
                .referencia(requestRecord.referencia())
                .estatus("PROCESANDO")
                .fechaOperacion(LocalDateTime.now())
                .build();
        pagoServicioRepository.save(transaccion);

        // =========================================================================
        // PASO 4: LLAMADA A SENDTX, EVALUACIÓN DE RESPUESTA, DESCUENTO Y CONCILIACIÓN
        // =========================================================================

        GestoPagoSendTxResponse respuestaProveedor;
        try {
            respuestaProveedor = ejecutarEnvioSendTx(sendTxRequest, token);
        } catch (ProveedorPagoException ex) {
            // Riesgo financiero crítico: un timeout no implica necesariamente que el proveedor no procesó la transacción.
            transaccion.setEstatus("EN_CONCILIACION");
            pagoServicioRepository.save(transaccion);
            log.warn("Transacción con UPE {} marcada EN_CONCILIACION ante timeout o fallo de red con GestoPago. Folio: {}",
                    requestRecord.upe(), folioInicial);
            throw new ProveedorPagoException(
                    String.format("La comunicación con el proveedor externo entró en tiempo de espera. La transacción quedó EN_CONCILIACION con folio %s. No se descontó saldo. Consulte el estado de la UPE antes de reintentar.", folioInicial),
                    ex);
        }

        // Si la respuesta es distinta de "01" o rechazo: NO se descuenta saldo
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
        BigDecimal saldoActual = saldoAnterior.subtract(requestRecord.monto());
        cuenta.setSaldo(saldoActual);
        cuentaRepository.save(cuenta);

        String folioFinal = Optional.ofNullable(respuestaProveedor.getFolioTransaccion())
                .filter(Predicate.not(String::isBlank))
                .orElse(folioInicial);

        transaccion.setFolioTransaccion(folioFinal);
        transaccion.setEstatus(ESTATUS_APLICADO);
        pagoServicioRepository.save(transaccion);

        log.info("Pago confirmado con código 01. Saldo debitado exitosamente. Cuenta: {}, Folio: {}, Nuevo Saldo: {}",
                requestRecord.numCuenta(), folioFinal, saldoActual);

        String tipoOperacion = Boolean.TRUE.equals(requestRecord.bandera()) ? TIPO_RECARGA : TIPO_SERVICIO;
        String destinoPago = Boolean.TRUE.equals(requestRecord.bandera()) ? requestRecord.telefono() : requestRecord.referencia();

        return PagoServicioResponseDto.builder()
                .folioTransaccion(folioFinal)
                .numCuenta(requestRecord.numCuenta())
                .idServicio(requestRecord.idServicio())
                .idProducto(requestRecord.idProducto())
                .idConcepto(requestRecord.idConcepto())
                .idDistribuidor(distribuidorEfectivo)
                .monto(requestRecord.monto())
                .saldoAnterior(saldoAnterior)
                .saldoActual(saldoActual)
                .upe(requestRecord.upe())
                .tipoOperacion(tipoOperacion)
                .destinoPago(destinoPago)
                .estatus(ESTATUS_APLICADO)
                .mensaje("Pago aplicado exitosamente con código 01 del proveedor")
                .fechaOperacion(transaccion.getFechaOperacion())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoServicioResponseDto> obtenerHistorialPorCuenta(String numCuenta) {
        DeclarativeAssertUtil.asegurar(
                numCuenta != null && !numCuenta.isBlank(),
                () -> new ReglaNegocioException("El número de cuenta es requerido para consultar el historial de pagos"));

        return pagoServicioRepository.findByNumCuenta(numCuenta.trim())
                .stream()
                .sorted((t1, t2) -> {
                    if (t1.getFechaOperacion() == null || t2.getFechaOperacion() == null) {
                        return 0;
                    }
                    return t2.getFechaOperacion().compareTo(t1.getFechaOperacion());
                })
                .map(this::mapearTransaccionAResponse)
                .collect(Collectors.toList());
    }

    private PagoServicioResponseDto mapearTransaccionAResponse(PagoServicioEntity transaccion) {
        String tipoOperacion = Boolean.TRUE.equals(transaccion.getEsRecargaTelefonica()) ? TIPO_RECARGA : TIPO_SERVICIO;
        String destinoPago = Boolean.TRUE.equals(transaccion.getEsRecargaTelefonica()) ? transaccion.getTelefono() : transaccion.getReferencia();

        return PagoServicioResponseDto.builder()
                .folioTransaccion(transaccion.getFolioTransaccion())
                .numCuenta(transaccion.getNumCuenta())
                .idServicio(transaccion.getIdServicio())
                .idProducto(transaccion.getIdProducto())
                .idConcepto(transaccion.getIdConcepto())
                .idDistribuidor(transaccion.getIdDistribuidor())
                .monto(transaccion.getMonto())
                .upe(transaccion.getUpe())
                .tipoOperacion(tipoOperacion)
                .destinoPago(destinoPago)
                .estatus(transaccion.getEstatus())
                .mensaje("Transacción registrada: " + transaccion.getEstatus())
                .fechaOperacion(transaccion.getFechaOperacion())
                .build();
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public PagoServicioResponseDto consultarOConciliarPago(PagoConciliacionRequestDto request) {
        if (request == null || request.getUpe() == null || !request.getUpe().matches("^\\d{10}$")) {
            throw new ReglaNegocioException("El identificador UPE debe ser un número de 10 dígitos válido para conciliación");
        }

        PagoServicioEntity transaccion = pagoServicioRepository.findByUpe(request.getUpe())
                .orElseThrow(() -> new EntidadNoProcesableException("No se encontró ninguna transacción registrada con la UPE: " + request.getUpe()));

        if (ESTATUS_APLICADO.equalsIgnoreCase(transaccion.getEstatus())) {
            return construirRespuestaIdempotente(transaccion);
        }

        String tipoOperacion = Boolean.TRUE.equals(transaccion.getEsRecargaTelefonica()) ? TIPO_RECARGA : TIPO_SERVICIO;
        String destinoPago = Boolean.TRUE.equals(transaccion.getEsRecargaTelefonica()) ? transaccion.getTelefono() : transaccion.getReferencia();

        return PagoServicioResponseDto.builder()
                .folioTransaccion(transaccion.getFolioTransaccion())
                .numCuenta(transaccion.getNumCuenta())
                .idServicio(transaccion.getIdServicio())
                .idProducto(transaccion.getIdProducto())
                .idConcepto(transaccion.getIdConcepto())
                .idDistribuidor(transaccion.getIdDistribuidor())
                .monto(transaccion.getMonto())
                .upe(transaccion.getUpe())
                .tipoOperacion(tipoOperacion)
                .destinoPago(destinoPago)
                .estatus(transaccion.getEstatus())
                .mensaje("Estado actual de la transacción en sistema: " + transaccion.getEstatus())
                .fechaOperacion(transaccion.getFechaOperacion())
                .build();
    }

    private void validarProductoEnCatalogo(Long idServicio, Long idProducto, BigDecimal monto) {
        if (productoMongoRepository == null) {
            return;
        }

        try {
            Optional<ProductoDocument> productoOpt = productoMongoRepository.findByIdProducto(idProducto.intValue());
            if (productoOpt.isPresent()) {
                ProductoDocument producto = productoOpt.get();
                DeclarativeAssertUtil.asegurar(
                        producto.getIdServicio() != null && producto.getIdServicio().longValue() == idServicio,
                        () -> new ReglaNegocioException(String.format(
                                "El producto con ID %d no corresponde al servicio solicitado %d (Servicio en catálogo: %d)",
                                idProducto, idServicio, producto.getIdServicio())));

                validarPrecioCatalogo(producto, monto, idProducto);
            } else {
                validarExistenciaEnCatalogo(idProducto);
            }
        } catch (EntidadNoProcesableException | ReglaNegocioException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Advertencia: No fue posible validar producto contra MongoDB ({}), se continuará con validación relacional.", ex.getMessage());
        }
    }

    private void validarPrecioCatalogo(ProductoDocument producto, BigDecimal monto, Long idProducto) {
        if (producto.getPrecio() == null || producto.getPrecio().isBlank()) {
            return;
        }
        try {
            BigDecimal precioCatalogo = new BigDecimal(producto.getPrecio().trim());
            if (precioCatalogo.compareTo(BigDecimal.ZERO) > 0 && monto.compareTo(precioCatalogo) != 0) {
                throw new ReglaNegocioException(String.format(Locale.ROOT,
                        "El monto solicitado ($%.2f) no coincide con el precio autorizado en catálogo ($%.2f) para el producto %s",
                        monto, precioCatalogo, producto.getProducto()));
            }
        } catch (NumberFormatException ex) {
            log.debug("Precio de producto {} en catálogo es abierto o no numérico: {}", idProducto, producto.getPrecio());
        }
    }

    private void validarExistenciaEnCatalogo(Long idProducto) {
        try {
            long total = productoMongoRepository.count();
            if (total > 0) {
                throw new ReglaNegocioException("El producto con ID " + idProducto + " no existe en el catálogo autorizado de servicios");
            }
        } catch (ReglaNegocioException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("No se pudo verificar el conteo de productos en catálogo: {}", ex.getMessage());
        }
    }

    private void validarPinSeguro(CuentaEntity cuenta, String pinIngresado) {
        if (pinIngresado == null || !pinIngresado.trim().matches("^\\d{4,8}$")) {
            throw new ReglaNegocioException("El PIN o pass seguro de verificación debe contener entre 4 y 8 dígitos numéricos");
        }

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

    private String construirPayloadTransaccion(PagoServicioRequestRecord requestRecord) {
        return String.format(Locale.ROOT, "{\"numCuenta\":\"%s\",\"upe\":\"%s\",\"monto\":%.2f,\"servicio\":%d,\"producto\":%d}",
                requestRecord.numCuenta(), requestRecord.upe(), requestRecord.monto(), requestRecord.idServicio(), requestRecord.idProducto());
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
        String tipoOperacion = Boolean.TRUE.equals(previo.getEsRecargaTelefonica()) ? TIPO_RECARGA : TIPO_SERVICIO;
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
                .estatus(ESTATUS_APLICADO)
                .mensaje("Pago aplicado previamente (Respuesta idempotente)")
                .fechaOperacion(previo.getFechaOperacion())
                .build();
    }
}
