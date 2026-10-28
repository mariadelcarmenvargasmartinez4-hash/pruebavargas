package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.cliente.CuentaBancariaDto;
import com.proyecto.servicios.model.cuenta.CuentaActualizacionRequestDto;
import com.proyecto.servicios.model.cuenta.CuentaCreacionRequestDto;
import com.proyecto.servicios.model.cuenta.SaldoCuentaDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.service.Impl.CuentaServiceImpl;
import com.proyecto.servicios.util.ClienteTestDataBuilder;
import com.proyecto.servicios.util.CuentaTestDataBuilder;
import com.proyecto.servicios.util.GeneradorCuentaBancariaUtil;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para CuentaServiceImpl.
 * Utiliza Test Data Builders para eliminar redundancias y mantener independencia de base de datos con Mocks.
 */
@ExtendWith(MockitoExtension.class)
class CuentaServiceImplTest {

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
        clienteActivo = ClienteTestDataBuilder.clienteValido();
        clienteInactivo = ClienteTestDataBuilder.unClienteEntity().id(2L).activo(false).build();
        cuentaMock = CuentaTestDataBuilder.cuentaValida(clienteActivo);
    }

    @Test
    @DisplayName("Crear cuenta exitosa para cliente activo")
    void testCrearCuenta_Exitoso() {
        CuentaCreacionRequestDto request = CuentaTestDataBuilder.unaCuentaCreacionRequest()
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

    @Test
    @DisplayName("Crear cuenta rechaza cliente inactivo")
    void testCrearCuenta_ClienteInactivo() {
        CuentaCreacionRequestDto request = CuentaTestDataBuilder.unaCuentaCreacionRequest()
                .clienteId(2L)
                .build();

        when(clienteRepository.findById(2L)).thenReturn(Optional.of(clienteInactivo));

        assertThrows(ReglaNegocioException.class, () ->
                cuentaService.crearCuenta(request));
    }

    @Test
    @DisplayName("Consultar saldo de cuenta existente")
    void testObtenerSaldoCuenta() {
        when(cuentaRepository.findByNumeroCuenta("4815162342")).thenReturn(Optional.of(cuentaMock));

        SaldoCuentaDto saldoDto = cuentaService.obtenerSaldoCuenta("4815162342");

        assertNotNull(saldoDto);
        assertEquals("4815162342", saldoDto.getNumeroCuenta());
        assertEquals(new BigDecimal("2000.00"), saldoDto.getSaldo());
        assertEquals("ACTIVA", saldoDto.getEstatus());
    }

    @Test
    @DisplayName("Actualizar cuenta parcialmente")
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
