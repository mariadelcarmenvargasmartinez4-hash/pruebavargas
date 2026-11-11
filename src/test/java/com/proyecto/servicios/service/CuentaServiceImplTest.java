package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.cliente.CuentaBancariaDto;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.model.cuenta.CuentaActualizacionRequestDto;
import com.proyecto.servicios.model.cuenta.CuentaConsultaFiltroRecord;
import com.proyecto.servicios.model.cuenta.CuentaCreacionRequestDto;
import com.proyecto.servicios.model.cuenta.SaldoCuentaDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.service.impl.CuentaServiceImpl;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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

/**
 * Pruebas unitarias completas para CuentaServiceImpl.
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
    @DisplayName("Obtener cuenta por número exitoso")
    void testObtenerCuentaPorNumero_Exitoso() {
        when(cuentaRepository.findByNumeroCuenta("4815162342")).thenReturn(Optional.of(cuentaMock));

        CuentaBancariaDto dto = cuentaService.obtenerCuentaPorNumero("4815162342");
        assertNotNull(dto);
        assertEquals("4815162342", dto.getNumeroCuenta());
    }

    @Test
    @DisplayName("Obtener cuenta por número inexistente lanza CuentaNoEncontradaException")
    void testObtenerCuentaPorNumero_NoExiste() {
        when(cuentaRepository.findByNumeroCuenta("9999999999")).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> cuentaService.obtenerCuentaPorNumero("9999999999"));
    }

    @Test
    @DisplayName("Buscar cuentas paginadas con filtros")
    void testBuscarCuentasPaginadas() {
        CuentaConsultaFiltroRecord filtro = new CuentaConsultaFiltroRecord("4815162342", 1L, "ACTIVA", true, 0, 10, "id", "ASC", 0);
        Page<CuentaEntity> page = new PageImpl<>(List.of(cuentaMock));

        org.mockito.ArgumentCaptor<Specification<CuentaEntity>> captor = org.mockito.ArgumentCaptor.forClass(Specification.class);
        when(cuentaRepository.findAll(captor.capture(), any(Pageable.class))).thenReturn(page);

        PaginaResponseRecord<CuentaBancariaDto> res = cuentaService.buscarCuentasPaginadas(filtro);
        assertNotNull(res);
        assertEquals(1, res.totalElementos());

        Specification<CuentaEntity> spec = captor.getValue();
        jakarta.persistence.criteria.Root<CuentaEntity> root = org.mockito.Mockito.mock(jakarta.persistence.criteria.Root.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        jakarta.persistence.criteria.CriteriaQuery<?> query = org.mockito.Mockito.mock(jakarta.persistence.criteria.CriteriaQuery.class);
        jakarta.persistence.criteria.CriteriaBuilder cb = org.mockito.Mockito.mock(jakarta.persistence.criteria.CriteriaBuilder.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);

        spec.toPredicate(root, query, cb);
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
                .estatus("ACTIVA")
                .build();

        when(cuentaRepository.findByNumeroCuenta("4815162342")).thenReturn(Optional.of(cuentaMock));
        when(cuentaRepository.save(any(CuentaEntity.class))).thenReturn(cuentaMock);

        CuentaBancariaDto resultado = cuentaService.actualizarCuenta("4815162342", actualizacion);

        assertNotNull(resultado);
        verify(cuentaRepository).save(any(CuentaEntity.class));
    }

    @Test
    @DisplayName("Actualizar cuenta de cliente inactivo lanza excepcion")
    void testActualizarCuenta_ClienteInactivo_LanzaExcepcion() {
        cuentaMock.setCliente(clienteInactivo);
        when(cuentaRepository.findByNumeroCuenta("4815162342")).thenReturn(Optional.of(cuentaMock));

        CuentaActualizacionRequestDto actualizacion = CuentaActualizacionRequestDto.builder().saldo(new BigDecimal("100.00")).build();
        assertThrows(ReglaNegocioException.class, () -> cuentaService.actualizarCuenta("4815162342", actualizacion));
    }

    @Test
    @DisplayName("Actualizar cuenta cancelada lanza excepcion")
    void testActualizarCuenta_Cancelada_LanzaExcepcion() {
        cuentaMock.setEstatus("CANCELADA");
        when(cuentaRepository.findByNumeroCuenta("4815162342")).thenReturn(Optional.of(cuentaMock));

        CuentaActualizacionRequestDto actualizacion = CuentaActualizacionRequestDto.builder().saldo(new BigDecimal("100.00")).build();
        assertThrows(ReglaNegocioException.class, () -> cuentaService.actualizarCuenta("4815162342", actualizacion));
    }

    @Test
    @DisplayName("Desactivar cuenta exitosamente")
    void testDesactivarCuenta_Exitoso() {
        when(cuentaRepository.findByNumeroCuenta("4815162342")).thenReturn(Optional.of(cuentaMock));

        cuentaService.desactivarCuenta("4815162342");

        assertEquals("CANCELADA", cuentaMock.getEstatus());
        verify(cuentaRepository).save(cuentaMock);
    }

    @Test
    @DisplayName("Desactivar cuenta ya cancelada lanza excepcion")
    void testDesactivarCuenta_YaCancelada_LanzaExcepcion() {
        cuentaMock.setEstatus("CANCELADA");
        when(cuentaRepository.findByNumeroCuenta("4815162342")).thenReturn(Optional.of(cuentaMock));

        assertThrows(ReglaNegocioException.class, () -> cuentaService.desactivarCuenta("4815162342"));
    }

    @Test
    @DisplayName("Reactivar cuenta exitosamente")
    void testReactivarCuenta_Exitoso() {
        cuentaMock.setEstatus("CANCELADA");
        when(cuentaRepository.findByNumeroCuenta("4815162342")).thenReturn(Optional.of(cuentaMock));
        when(cuentaRepository.save(any(CuentaEntity.class))).thenAnswer(i -> i.getArgument(0));

        CuentaBancariaDto dto = cuentaService.reactivarCuenta("4815162342");

        assertNotNull(dto);
        assertEquals("ACTIVA", cuentaMock.getEstatus());
    }

    @Test
    @DisplayName("Reactivar cuenta de cliente inactivo lanza excepcion")
    void testReactivarCuenta_ClienteInactivo_LanzaExcepcion() {
        cuentaMock.setCliente(clienteInactivo);
        cuentaMock.setEstatus("CANCELADA");
        when(cuentaRepository.findByNumeroCuenta("4815162342")).thenReturn(Optional.of(cuentaMock));

        assertThrows(ReglaNegocioException.class, () -> cuentaService.reactivarCuenta("4815162342"));
    }

    @Test
    @DisplayName("Reactivar cuenta ya activa lanza excepcion")
    void testReactivarCuenta_YaActiva_LanzaExcepcion() {
        cuentaMock.setEstatus("ACTIVA");
        when(cuentaRepository.findByNumeroCuenta("4815162342")).thenReturn(Optional.of(cuentaMock));

        assertThrows(ReglaNegocioException.class, () -> cuentaService.reactivarCuenta("4815162342"));
    }
}
