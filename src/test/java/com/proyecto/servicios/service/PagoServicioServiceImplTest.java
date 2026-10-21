package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.pago.PagoServicioEntity;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.pago.PagoServicioRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioResponseDto;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.pago.PagoServicioRepository;
import com.proyecto.servicios.service.Impl.PagoServicioServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServicioServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private PagoServicioRepository pagoServicioRepository;

    @InjectMocks
    private PagoServicioServiceImpl pagoServicioService;

    @Test
    @DisplayName("procesarPago - Pago exitoso debita de la cuenta y persiste registro")
    void procesarPagoExitoso() {
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

        when(cuentaRepository.findByNumeroCuenta("1234567890123456")).thenReturn(Optional.of(cuenta));
        when(pagoServicioRepository.save(any(PagoServicioEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PagoServicioResponseDto response = pagoServicioService.procesarPago(request);

        assertNotNull(response);
        assertEquals("1234567890123456", response.getNumCuenta());
        assertEquals(new BigDecimal("150.00"), response.getMonto());
        assertEquals(new BigDecimal("500.00"), response.getSaldoAnterior());
        assertEquals(new BigDecimal("350.00"), response.getSaldoActual());
        assertEquals("1234567890", response.getUpe());
        assertEquals("RECARGA_TELEFONICA", response.getTipoOperacion());
        assertEquals("5598765432", response.getDestinoPago());
        assertEquals("APLICADO", response.getEstatus());

        verify(cuentaRepository).save(cuenta);
        verify(pagoServicioRepository).save(any(PagoServicioEntity.class));
    }

    @Test
    @DisplayName("procesarPago - Cuenta no encontrada lanza CuentaNoEncontradaException")
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
    }

    @Test
    @DisplayName("procesarPago - Saldo insuficiente lanza ReglaNegocioException")
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
    }

    @Test
    @DisplayName("procesarPago - Cuenta inactiva lanza ReglaNegocioException")
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
    }
}
