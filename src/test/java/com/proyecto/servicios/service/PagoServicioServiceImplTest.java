package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.client.GestoPagoSendTxClient;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.pago.PagoServicioEntity;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxResponse;
import com.proyecto.servicios.model.pago.PagoServicioRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioResponseDto;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.pago.PagoServicioRepository;
import com.proyecto.servicios.service.Impl.PagoServicioServiceImpl;
import com.proyecto.servicios.util.AesEncryptionUtil;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

    @InjectMocks
    private PagoServicioServiceImpl pagoServicioService;

    @Test
    @DisplayName("Paso 4: Si proveedor retorna código 01, se descuenta el saldo en la base de datos")
    void procesarPagoExitosoCodigo01DescuentaSaldo() {
        CuentaEntity cuenta = CuentaEntity.builder()
                .id(1L)
                .numeroCuenta("1234567890123456")
                .saldo(new BigDecimal("500.00"))
                .estatus("ACTIVA")
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

        when(cuentaRepository.findByNumeroCuenta("1234567890123456")).thenReturn(Optional.of(cuenta));
        when(gestoPagoTokenService.obtenerTokenActivo(any(), any())).thenReturn(Optional.empty());
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
        verify(pagoServicioRepository).save(any(PagoServicioEntity.class));
    }

    @Test
    @DisplayName("Paso 4: Si proveedor retorna código distinto de 01, NO se descuenta saldo")
    void procesarPagoCodigoDistintoDe01NoDescuentaSaldo() {
        CuentaEntity cuenta = CuentaEntity.builder()
                .id(1L)
                .numeroCuenta("1234567890123456")
                .saldo(new BigDecimal("500.00"))
                .estatus("ACTIVA")
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

        when(cuentaRepository.findByNumeroCuenta("1234567890123456")).thenReturn(Optional.of(cuenta));
        when(gestoPagoTokenService.obtenerTokenActivo(any(), any())).thenReturn(Optional.empty());
        when(aesEncryptionUtil.encriptar(anyString(), any())).thenReturn("ENCRYPTED_AES");
        when(aesEncryptionUtil.firmarPayload(anyString(), any())).thenReturn("SIGNATURE_HMAC");
        when(gestoPagoSendTxClient.sendTx(anyString(), any(GestoPagoSendTxRequest.class))).thenReturn(responseProveedor);

        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(request));

        // El saldo permanece intacto y la cuenta NO fue actualizada
        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
        verify(cuentaRepository, never()).save(cuenta);
    }

    @Test
    @DisplayName("Paso 4: Si ocurre error de comunicación o Timeout, NO se descuenta saldo")
    void procesarPagoTimeoutNoDescuentaSaldo() {
        CuentaEntity cuenta = CuentaEntity.builder()
                .id(1L)
                .numeroCuenta("1234567890123456")
                .saldo(new BigDecimal("500.00"))
                .estatus("ACTIVA")
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

        when(cuentaRepository.findByNumeroCuenta("1234567890123456")).thenReturn(Optional.of(cuenta));
        when(gestoPagoTokenService.obtenerTokenActivo(any(), any())).thenReturn(Optional.empty());
        when(aesEncryptionUtil.encriptar(anyString(), any())).thenReturn("ENCRYPTED_AES");
        when(aesEncryptionUtil.firmarPayload(anyString(), any())).thenReturn("SIGNATURE_HMAC");
        when(gestoPagoSendTxClient.sendTx(anyString(), any(GestoPagoSendTxRequest.class)))
                .thenThrow(FeignException.GatewayTimeout.class);

        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(request));

        // Saldo permanece intacto
        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
        verify(cuentaRepository, never()).save(cuenta);
    }

    @Test
    @DisplayName("Paso 2: Validación de PIN incorrecto lanza ReglaNegocioException y no llama a GestoPago")
    void procesarPagoPinIncorrectoLanzaExcepcion() {
        CuentaEntity cuenta = CuentaEntity.builder()
                .id(1L)
                .numeroCuenta("1234567890123456")
                .saldo(new BigDecimal("500.00"))
                .estatus("ACTIVA")
                .pinHash("hashed_pin_bcrypt")
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

        when(cuentaRepository.findByNumeroCuenta("1234567890123456")).thenReturn(Optional.of(cuenta));
        when(passwordEncoder.matches(eq("9999"), eq("hashed_pin_bcrypt"))).thenReturn(false);

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

        when(cuentaRepository.findByNumeroCuenta("1234567890123456")).thenReturn(Optional.of(cuenta));

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

        when(cuentaRepository.findByNumeroCuenta("1234567890123456")).thenReturn(Optional.of(cuenta));

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

        when(cuentaRepository.findByNumeroCuenta("9999999999999999")).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> pagoServicioService.procesarPago(request));
        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), any());
    }
}
