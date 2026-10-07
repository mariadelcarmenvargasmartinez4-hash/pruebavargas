package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.cliente.CuentaBancariaDto;
import com.proyecto.servicios.model.cuenta.CuentaActualizacionRequestDto;
import com.proyecto.servicios.model.cuenta.CuentaCreacionRequestDto;
import com.proyecto.servicios.model.cuenta.SaldoCuentaDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.service.Impl.CuentaServiceImpl;
import com.proyecto.servicios.util.GeneradorCuentaBancariaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pruebas unitarias para CuentaServiceImpl
@ExtendWith(MockitoExtension.class)
public class CuentaServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private GeneradorCuentaBancariaUtil generadorCuentaUtil;

    @InjectMocks
    private CuentaServiceImpl cuentaService;

    private ClienteEntity clienteActivo;
    private ClienteEntity clienteInactivo;
    private CuentaEntity cuentaMock;

    @BeforeEach
    void setUp() {
        clienteActivo = ClienteEntity.builder()
                .id(1L)
                .nombre("Juan")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Lopez")
                .activo(true)
                .build();

        clienteInactivo = ClienteEntity.builder()
                .id(2L)
                .nombre("Maria")
                .apellidoPaterno("Gomez")
                .apellidoMaterno("Ruiz")
                .activo(false)
                .build();

        cuentaMock = CuentaEntity.builder()
                .id(10L)
                .cliente(clienteActivo)
                .numeroCuenta("4815162342")
                .clabe("012180004815162342")
                .tipoCuenta("DEBITO")
                .saldo(new BigDecimal("2000.00"))
                .moneda("MXN")
                .estatus("ACTIVA")
                .build();
    }

    // Valida creacion de cuenta exitosa para cliente activo
    @Test
    void testCrearCuenta_Exitoso() {
        CuentaCreacionRequestDto request = CuentaCreacionRequestDto.builder()
                .clienteId(1L)
                .saldoInicial(new BigDecimal("1000.00"))
                .tipoCuenta("AHORRO")
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteActivo));
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(generadorCuentaUtil.generarNumeroCuenta()).thenReturn("4815162342");
        when(generadorCuentaUtil.generarClabe("4815162342")).thenReturn("012180004815162342");
        when(cuentaRepository.save(any(CuentaEntity.class))).thenReturn(cuentaMock);

        CuentaBancariaDto resultado = cuentaService.crearCuenta(request);

        assertNotNull(resultado);
        assertEquals("4815162342", resultado.getNumeroCuenta());
        assertEquals("ACTIVA", resultado.getEstatus());
        verify(cuentaRepository).save(any(CuentaEntity.class));
    }

    // Valida rechazo si el cliente esta inactivo
    @Test
    void testCrearCuenta_ClienteInactivo() {
        CuentaCreacionRequestDto request = CuentaCreacionRequestDto.builder()
                .clienteId(2L)
                .saldoInicial(new BigDecimal("1000.00"))
                .build();

        when(clienteRepository.findById(2L)).thenReturn(Optional.of(clienteInactivo));

        assertThrows(ReglaNegocioException.class, () ->
                cuentaService.crearCuenta(request));
    }

    // Valida consulta de saldo
    @Test
    void testObtenerSaldoCuenta() {
        when(cuentaRepository.findByNumeroCuenta("4815162342")).thenReturn(Optional.of(cuentaMock));

        SaldoCuentaDto saldoDto = cuentaService.obtenerSaldoCuenta("4815162342");

        assertNotNull(saldoDto);
        assertEquals("4815162342", saldoDto.getNumeroCuenta());
        assertEquals(new BigDecimal("2000.00"), saldoDto.getSaldo());
        assertEquals("ACTIVA", saldoDto.getEstatus());
    }

    // Valida actualizacion parcial de cuenta
    @Test
    void testActualizarCuenta_Parcial() {
        CuentaActualizacionRequestDto actualizacion = CuentaActualizacionRequestDto.builder()
                .saldo(new BigDecimal("3500.00"))
                .tipoCuenta("NOMINA")
                .build();

        when(cuentaRepository.findByNumeroCuenta("4815162342")).thenReturn(Optional.of(cuentaMock));
        when(cuentaRepository.save(any(CuentaEntity.class))).thenReturn(cuentaMock);

        CuentaBancariaDto resultado = cuentaService.actualizarCuenta("4815162342", actualizacion);

        assertNotNull(resultado);
        verify(cuentaRepository).save(any(CuentaEntity.class));
    }
}
