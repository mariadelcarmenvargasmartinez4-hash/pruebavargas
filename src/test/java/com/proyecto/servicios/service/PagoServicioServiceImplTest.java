package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.client.GestoPagoSendTxClient;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.ProductoDocument;
import com.proyecto.servicios.entity.pago.PagoServicioEntity;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.ProveedorPagoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxResponse;
import com.proyecto.servicios.model.pago.PagoConciliacionRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioResponseDto;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.mongo.ProductoMongoRepository;
import com.proyecto.servicios.repositorys.pago.PagoServicioRepository;
import com.proyecto.servicios.service.Impl.PagoServicioServiceImpl;
import com.proyecto.servicios.util.AesEncryptionUtil;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServicioServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private PagoServicioRepository pagoServicioRepository;

    @Mock
    private GestoPagoSendTxClient gestoPagoSendTxClient;

    @Mock
    private GestoPagoAuthClient gestoPagoAuthClient;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;

    @Mock
    private AesEncryptionUtil aesEncryptionUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ProductoMongoRepository productoMongoRepository;

    @InjectMocks
    private PagoServicioServiceImpl pagoServicioService;

    private static final String PIN_HASH = "$2a$10$hashedPin1234Bcrypt";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pagoServicioService, "claveSecretaAes", "ClaveTestSeguraDe32BytesParaPruebas!");
        ReflectionTestUtils.setField(pagoServicioService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(pagoServicioService, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(pagoServicioService, "passwordDistribuidor", "testpass123");
        ReflectionTestUtils.setField(pagoServicioService, "productoMongoRepository", productoMongoRepository);
    }

    @Test
    @DisplayName("Paso 4: Si proveedor retorna código 01, se descuenta el saldo en la base de datos")
    void procesarPagoExitosoCodigo01DescuentaSaldo() {
        CuentaEntity cuenta = CuentaEntity.builder()
                .id(1L)
                .numeroCuenta("1234567890123456")
                .saldo(new BigDecimal("500.00"))
                .estatus("ACTIVA")
                .pinHash(PIN_HASH)
                .build();

        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("150.00"))
                .upe("1234567890")
                .bandera(true)
                .telefono("5598765432")
                .pin("1234")
                .build();

        GestoPagoSendTxResponse responseProveedor = GestoPagoSendTxResponse.builder()
                .codigo("01")
                .mensaje("Transacción Exitosa")
                .folioTransaccion("GP-TX-999")
                .autorizacion("AUTH-12345")
                .build();

        GestoPagoToken tokenActivo = GestoPagoToken.builder()
                .token("VALID_TOKEN")
                .activo(true)
                .expiresIn(3600L)
                .fechaActualizacion(LocalDateTime.now())
                .build();

        when(cuentaRepository.findByNumeroCuentaWithLock("1234567890123456")).thenReturn(Optional.of(cuenta));
        when(passwordEncoder.matches("1234", PIN_HASH)).thenReturn(true);
        when(gestoPagoTokenService.obtenerTokenActivo(any(), any())).thenReturn(Optional.of(tokenActivo));
        when(aesEncryptionUtil.encriptar(anyString(), any())).thenReturn("ENCRYPTED_AES");
        when(aesEncryptionUtil.firmarPayload(anyString(), any())).thenReturn("SIGNATURE_HMAC");
        when(gestoPagoSendTxClient.sendTx(anyString(), any(GestoPagoSendTxRequest.class))).thenReturn(responseProveedor);
        when(pagoServicioRepository.save(any(PagoServicioEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PagoServicioResponseDto response = pagoServicioService.procesarPago(request);

        assertNotNull(response);
        assertEquals("1234567890123456", response.getNumCuenta());
        assertEquals(new BigDecimal("150.00"), response.getMonto());
        assertEquals(new BigDecimal("500.00"), response.getSaldoAnterior());
        assertEquals(new BigDecimal("350.00"), response.getSaldoActual());
        assertEquals("GP-TX-999", response.getFolioTransaccion());
        assertEquals("APLICADO", response.getEstatus());

        // Verificación crítica: el saldo se descontó y se persistió
        verify(cuentaRepository).save(cuenta);
        verify(pagoServicioRepository, times(2)).save(any(PagoServicioEntity.class));
    }

    @Test
    @DisplayName("Paso 4: Si proveedor retorna código distinto de 01, NO se descuenta saldo")
    void procesarPagoCodigoDistintoDe01NoDescuentaSaldo() {
        CuentaEntity cuenta = CuentaEntity.builder()
                .id(1L)
                .numeroCuenta("1234567890123456")
                .saldo(new BigDecimal("500.00"))
                .estatus("ACTIVA")
                .pinHash(PIN_HASH)
                .build();

        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("150.00"))
                .upe("1234567890")
                .bandera(false)
                .referencia("REF-12345")
                .pin("1234")
                .build();

        GestoPagoSendTxResponse responseProveedor = GestoPagoSendTxResponse.builder()
                .codigo("02")
                .mensaje("Rechazada por saldo insuficiente del distribuidor")
                .build();

        GestoPagoToken tokenActivo = GestoPagoToken.builder()
                .token("VALID_TOKEN")
                .activo(true)
                .expiresIn(3600L)
                .fechaActualizacion(LocalDateTime.now())
                .build();

        when(cuentaRepository.findByNumeroCuentaWithLock("1234567890123456")).thenReturn(Optional.of(cuenta));
        when(passwordEncoder.matches("1234", PIN_HASH)).thenReturn(true);
        when(gestoPagoTokenService.obtenerTokenActivo(any(), any())).thenReturn(Optional.of(tokenActivo));
        when(aesEncryptionUtil.encriptar(anyString(), any())).thenReturn("ENCRYPTED_AES");
        when(aesEncryptionUtil.firmarPayload(anyString(), any())).thenReturn("SIGNATURE_HMAC");
        when(gestoPagoSendTxClient.sendTx(anyString(), any(GestoPagoSendTxRequest.class))).thenReturn(responseProveedor);

        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(request));

        // El saldo permanece intacto y la cuenta NO fue actualizada
        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
        verify(cuentaRepository, never()).save(cuenta);
    }

    @Test
    @DisplayName("Paso 4: Si ocurre timeout, se marca EN_CONCILIACION, sendTx fue invocado y no se descuenta saldo")
    void procesarPagoTimeoutMarcaEnConciliacionYNoDescuentaSaldo() {
        CuentaEntity cuenta = CuentaEntity.builder()
                .id(1L)
                .numeroCuenta("1234567890123456")
                .saldo(new BigDecimal("500.00"))
                .estatus("ACTIVA")
                .pinHash(PIN_HASH)
                .build();

        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("100.00"))
                .upe("1234567890")
                .bandera(true)
                .telefono("5512345678")
                .pin("1234")
                .build();

        GestoPagoToken tokenActivo = GestoPagoToken.builder()
                .token("VALID_TOKEN")
                .activo(true)
                .expiresIn(3600L)
                .fechaActualizacion(LocalDateTime.now())
                .build();

        when(cuentaRepository.findByNumeroCuentaWithLock("1234567890123456")).thenReturn(Optional.of(cuenta));
        when(passwordEncoder.matches("1234", PIN_HASH)).thenReturn(true);
        when(gestoPagoTokenService.obtenerTokenActivo(any(), any())).thenReturn(Optional.of(tokenActivo));
        when(aesEncryptionUtil.encriptar(anyString(), any())).thenReturn("ENCRYPTED_AES");
        when(aesEncryptionUtil.firmarPayload(anyString(), any())).thenReturn("SIGNATURE_HMAC");
        when(gestoPagoSendTxClient.sendTx(anyString(), any(GestoPagoSendTxRequest.class)))
                .thenThrow(FeignException.GatewayTimeout.class);

        ProveedorPagoException ex = assertThrows(ProveedorPagoException.class, () -> pagoServicioService.procesarPago(request));
        assertTrue(ex.getMessage().contains("EN_CONCILIACION"));

        // Verificación de que sendTx fue llamado
        verify(gestoPagoSendTxClient, times(1)).sendTx(anyString(), any(GestoPagoSendTxRequest.class));

        // Saldo permanece intacto
        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
        verify(cuentaRepository, never()).save(cuenta);
    }

    @Test
    @DisplayName("Catálogo: Validación rechaza producto que no coincide con el servicio")
    void validarProductoEnCatalogoServicioIncorrectoLanzaExcepcion() {
        ProductoDocument prod = ProductoDocument.builder()
                .idProducto(101)
                .idServicio(99) // Diferente al servicio 1 de la petición
                .producto("PAQUETE INTERNET 100")
                .precio("150.00")
                .build();

        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("150.00"))
                .upe("1234567890")
                .bandera(true)
                .telefono("5512345678")
                .pin("1234")
                .build();

        when(pagoServicioRepository.findByUpe("1234567890")).thenReturn(Optional.empty());
        when(productoMongoRepository.findByIdProducto(101)).thenReturn(Optional.of(prod));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(request));
        assertTrue(ex.getMessage().contains("no corresponde al servicio"));

        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), any());
    }

    @Test
    @DisplayName("Catálogo: Validación rechaza monto manipulado distinto al precio fijo en catálogo")
    void validarProductoEnCatalogoPrecioManipuladoLanzaExcepcion() {
        ProductoDocument prod = ProductoDocument.builder()
                .idProducto(101)
                .idServicio(1)
                .producto("PAQUETE FIJO")
                .precio("200.00") // Precio fijo en catálogo $200.00
                .build();

        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("150.00")) // Cliente envía $150.00 (manipulado)
                .upe("1234567890")
                .bandera(true)
                .telefono("5512345678")
                .pin("1234")
                .build();

        when(pagoServicioRepository.findByUpe("1234567890")).thenReturn(Optional.empty());
        when(productoMongoRepository.findByIdProducto(101)).thenReturn(Optional.of(prod));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(request));
        assertTrue(ex.getMessage().contains("no coincide con el precio autorizado en catálogo"));

        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), any());
    }

    @Test
    @DisplayName("Conciliación: Transacción en estado EN_CONCILIACION impide reenvío ciego a GestoPago")
    void transaccionEnConciliacionImpideReenvioCiego() {
        PagoServicioEntity transaccionConcilia = PagoServicioEntity.builder()
                .folioTransaccion("GP-TX-TIMEOUT-001")
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("100.00"))
                .upe("1234567890")
                .esRecargaTelefonica(true)
                .telefono("5512345678")
                .estatus("EN_CONCILIACION")
                .fechaOperacion(LocalDateTime.now())
                .build();

        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("100.00"))
                .upe("1234567890")
                .bandera(true)
                .telefono("5512345678")
                .pin("1234")
                .build();

        when(pagoServicioRepository.findByUpe("1234567890")).thenReturn(Optional.of(transaccionConcilia));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(request));
        assertTrue(ex.getMessage().contains("EN_CONCILIACION"));

        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), any());
    }

    @Test
    @DisplayName("Conciliación: Endpoint de consulta retorna estado actual de la transacción")
    void consultarOConciliarPagoRetornaEstado() {
        PagoServicioEntity transaccion = PagoServicioEntity.builder()
                .folioTransaccion("GP-TX-APLICADA-001")
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("100.00"))
                .upe("1234567890")
                .esRecargaTelefonica(true)
                .telefono("5512345678")
                .estatus("APLICADO")
                .fechaOperacion(LocalDateTime.now())
                .build();

        when(pagoServicioRepository.findByUpe("1234567890")).thenReturn(Optional.of(transaccion));

        PagoServicioResponseDto response = pagoServicioService.consultarOConciliarPago(
                PagoConciliacionRequestDto.builder().upe("1234567890").build());

        assertNotNull(response);
        assertEquals("GP-TX-APLICADA-001", response.getFolioTransaccion());
        assertEquals("APLICADO", response.getEstatus());
    }

    @Test
    @DisplayName("Comment 1: Cuenta sin PIN configurado rechaza pago y no permite bypass")
    void procesarPagoCuentaSinPinConfiguradoLanzaExcepcion() {
        CuentaEntity cuentaSinPin = CuentaEntity.builder()
                .id(1L)
                .numeroCuenta("1234567890123456")
                .saldo(new BigDecimal("500.00"))
                .estatus("ACTIVA")
                .pinHash(null)
                .build();

        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("150.00"))
                .upe("1234567890")
                .bandera(true)
                .telefono("5512345678")
                .pin("1234")
                .build();

        when(cuentaRepository.findByNumeroCuentaWithLock("1234567890123456")).thenReturn(Optional.of(cuentaSinPin));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(request));
        assertTrue(ex.getMessage().contains("no tiene un PIN de autorización configurado"));

        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), any());
    }

    @Test
    @DisplayName("Comment 5: Petición repetida con mismo UPE es idempotente y no duplica débito")
    void procesarPagoReintentoIdempotenteNoDuplicaCargo() {
        PagoServicioEntity pagoPrevio = PagoServicioEntity.builder()
                .folioTransaccion("GP-TX-PREVIO-001")
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("100.00"))
                .upe("1234567890")
                .esRecargaTelefonica(true)
                .telefono("5512345678")
                .estatus("APLICADO")
                .fechaOperacion(LocalDateTime.now())
                .build();

        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("100.00"))
                .upe("1234567890")
                .bandera(true)
                .telefono("5512345678")
                .pin("1234")
                .build();

        when(pagoServicioRepository.findByUpe("1234567890")).thenReturn(Optional.of(pagoPrevio));

        PagoServicioResponseDto response = pagoServicioService.procesarPago(request);

        assertNotNull(response);
        assertEquals("GP-TX-PREVIO-001", response.getFolioTransaccion());
        assertEquals("APLICADO", response.getEstatus());

        // Asegurar que no se debitó cuenta ni se llamó a GestoPago
        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), any());
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Paso 2: Validación de PIN incorrecto lanza ReglaNegocioException y no llama a GestoPago")
    void procesarPagoPinIncorrectoLanzaExcepcion() {
        CuentaEntity cuenta = CuentaEntity.builder()
                .id(1L)
                .numeroCuenta("1234567890123456")
                .saldo(new BigDecimal("500.00"))
                .estatus("ACTIVA")
                .pinHash(PIN_HASH)
                .build();

        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("150.00"))
                .upe("1234567890")
                .bandera(true)
                .telefono("5512345678")
                .pin("9999")
                .build();

        when(cuentaRepository.findByNumeroCuentaWithLock("1234567890123456")).thenReturn(Optional.of(cuenta));
        when(passwordEncoder.matches(eq("9999"), eq(PIN_HASH))).thenReturn(false);

        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(request));

        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), any());
        verify(cuentaRepository, never()).save(cuenta);
    }

    @Test
    @DisplayName("Paso 2: Validación de Saldo insuficiente lanza ReglaNegocioException")
    void procesarPagoSaldoInsuficienteLanzaExcepcion() {
        CuentaEntity cuenta = CuentaEntity.builder()
                .id(1L)
                .numeroCuenta("1234567890123456")
                .saldo(new BigDecimal("50.00"))
                .estatus("ACTIVA")
                .pinHash(PIN_HASH)
                .build();

        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("150.00"))
                .upe("1234567890")
                .bandera(true)
                .telefono("5512345678")
                .pin("1234")
                .build();

        when(cuentaRepository.findByNumeroCuentaWithLock("1234567890123456")).thenReturn(Optional.of(cuenta));
        when(passwordEncoder.matches("1234", PIN_HASH)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(request));
        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), any());
    }

    @Test
    @DisplayName("Paso 2: Validación de Cuenta inactiva lanza ReglaNegocioException")
    void procesarPagoCuentaInactivaLanzaExcepcion() {
        CuentaEntity cuenta = CuentaEntity.builder()
                .id(1L)
                .numeroCuenta("1234567890123456")
                .saldo(new BigDecimal("500.00"))
                .estatus("BLOQUEADA")
                .build();

        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("100.00"))
                .upe("1234567890")
                .bandera(true)
                .telefono("5512345678")
                .pin("1234")
                .build();

        when(cuentaRepository.findByNumeroCuentaWithLock("1234567890123456")).thenReturn(Optional.of(cuenta));

        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(request));
        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), any());
    }

    @Test
    @DisplayName("Paso 2: Cuenta inexistente lanza CuentaNoEncontradaException")
    void procesarPagoCuentaInexistenteLanzaExcepcion() {
        PagoServicioRequestDto request = PagoServicioRequestDto.builder()
                .numCuenta("9999999999999999")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("100.00"))
                .upe("1234567890")
                .bandera(false)
                .referencia("REF-12345")
                .pin("1234")
                .build();

        when(cuentaRepository.findByNumeroCuentaWithLock("9999999999999999")).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> pagoServicioService.procesarPago(request));
        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), any());
    }
}
