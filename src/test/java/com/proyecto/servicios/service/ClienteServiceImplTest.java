package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.DomicilioEntity;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.model.cliente.ClienteActualizacionRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroResponseDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.DomicilioRepository;
import com.proyecto.servicios.service.Impl.ClienteServiceImpl;
import com.proyecto.servicios.util.GeneradorCuentaBancariaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pruebas unitarias para ClienteServiceImpl cubriendo reglas de negocio, validaciones, baja logica y consultas
@ExtendWith(MockitoExtension.class)
public class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private DomicilioRepository domicilioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private GeneradorCuentaBancariaUtil generadorCuentaUtil;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private ClienteRegistroRequestDto requestValido;
    private ClienteEntity clienteMock;
    private DomicilioEntity domicilioMock;
    private CuentaEntity cuentaMock;

    @BeforeEach
    void setUp() {
        requestValido = ClienteRegistroRequestDto.builder()
                .nombre("Juan")
                .segundoNombre("Carlos")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Lopez")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .curp("PELJ900515HDFRPR09")
                .rfc("PELJ9005151A2")
                .sexo("MASCULINO")
                .nacionalidad("Mexicana")
                .estadoCivil("SOLTERO")
                .correoElectronico("juan.perez@example.com")
                .telefonoMovil("5512345678")
                .telefonoAlternativo("5587654321")
                .calle("Av. Reforma")
                .numeroExterior("123")
                .numeroInterior("Piso 4")
                .colonia("Juarez")
                .municipio("Cuauhtemoc")
                .estado("Ciudad de Mexico")
                .codigoPostal("06600")
                .pais("Mexico")
                .ocupacion("Ingeniero de Software")
                .empresa("Tech Solutions SA")
                .ingresoMensual(new BigDecimal("35000.00"))
                .saldoInicial(new BigDecimal("1500.00"))
                .tipoCuenta("DEBITO")
                .build();

        clienteMock = ClienteEntity.builder()
                .id(1L)
                .nombre("Juan")
                .segundoNombre("Carlos")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Lopez")
                .curp("PELJ900515HDFRPR09")
                .rfc("PELJ9005151A2")
                .sexo("MASCULINO")
                .nacionalidad("Mexicana")
                .estadoCivil("SOLTERO")
                .correoElectronico("juan.perez@example.com")
                .telefonoMovil("5512345678")
                .ocupacion("Ingeniero de Software")
                .empresa("Tech Solutions SA")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .ingresoMensual(new BigDecimal("35000.00"))
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        domicilioMock = DomicilioEntity.builder()
                .id(1L)
                .cliente(clienteMock)
                .calle("Av. Reforma")
                .numeroExterior("123")
                .colonia("Juarez")
                .municipio("Cuauhtemoc")
                .estado("Ciudad de Mexico")
                .codigoPostal("06600")
                .pais("Mexico")
                .build();

        cuentaMock = CuentaEntity.builder()
                .id(10L)
                .cliente(clienteMock)
                .numeroCuenta("4815162342")
                .clabe("012180004815162342")
                .tipoCuenta("DEBITO")
                .saldo(new BigDecimal("1500.00"))
                .moneda("MXN")
                .estatus("ACTIVA")
                .build();
    }

    // Valida registro exitoso en clientes, domicilios y cuentas
    @Test
    void testRegistrarCliente_Exitoso() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronico(anyString())).thenReturn(false);
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(generadorCuentaUtil.generarNumeroCuenta()).thenReturn("4815162342");
        when(generadorCuentaUtil.generarClabe("4815162342")).thenReturn("012180004815162342");
        when(clienteRepository.save(any(ClienteEntity.class))).thenReturn(clienteMock);
        when(domicilioRepository.save(any(DomicilioEntity.class))).thenReturn(domicilioMock);
        when(cuentaRepository.save(any(CuentaEntity.class))).thenReturn(cuentaMock);

        ClienteRegistroResponseDto respuesta = clienteService.registrarCliente(requestValido);

        assertNotNull(respuesta);
        assertEquals(0, respuesta.getCodigo());
        assertEquals("Juan Carlos Perez Lopez", respuesta.getNombreCompleto());
        assertEquals("PELJ900515HDFRPR09", respuesta.getCurp());
        assertNotNull(respuesta.getDomicilio());
        assertEquals("Av. Reforma", respuesta.getDomicilio().getCalle());
        assertNotNull(respuesta.getCuentaBancaria());
        assertEquals("4815162342", respuesta.getCuentaBancaria().getNumeroCuenta());
        assertEquals("ACTIVA", respuesta.getCuentaBancaria().getEstatus());

        verify(clienteRepository).save(any(ClienteEntity.class));
        verify(domicilioRepository).save(any(DomicilioEntity.class));
        verify(cuentaRepository).save(any(CuentaEntity.class));
    }

    // Valida rechazo si el cliente es menor de 18 anos
    @Test
    void testRegistrarCliente_MenorDeEdad() {
        requestValido.setFechaNacimiento(LocalDate.now().minusYears(17));

        assertThrows(ReglaNegocioException.class, () ->
                clienteService.registrarCliente(requestValido));
    }

    // Valida rechazo si el CURP ya existe
    @Test
    void testRegistrarCliente_CurpDuplicado() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(true);

        assertThrows(CurpDuplicadaException.class, () ->
                clienteService.registrarCliente(requestValido));
    }

    // Valida rechazo si el RFC ya existe
    @Test
    void testRegistrarCliente_RfcDuplicado() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(true);

        assertThrows(RfcDuplicadoException.class, () ->
                clienteService.registrarCliente(requestValido));
    }

    // Valida rechazo si el ingreso mensual es menor o igual a cero
    @Test
    void testRegistrarCliente_IngresoMenorOIgualACero() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronico(anyString())).thenReturn(false);
        requestValido.setIngresoMensual(BigDecimal.ZERO);

        assertThrows(ReglaNegocioException.class, () ->
                clienteService.registrarCliente(requestValido));
    }

    // Valida consulta de clientes activos
    @Test
    void testListarClientesActivos() {
        when(clienteRepository.findByActivoTrue()).thenReturn(List.of(clienteMock));
        when(domicilioRepository.findByClienteId(1L)).thenReturn(Optional.of(domicilioMock));
        when(cuentaRepository.findByClienteId(1L)).thenReturn(List.of(cuentaMock));

        List<ClienteRegistroResponseDto> activos = clienteService.listarClientesActivos();

        assertNotNull(activos);
        assertEquals(1, activos.size());
        assertEquals(true, activos.get(0).getActivo());
    }

    // Valida baja logica de cliente y desactivacion de cuentas
    @Test
    void testDesactivarCliente_BajaLogica() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(cuentaRepository.findByClienteId(1L)).thenReturn(List.of(cuentaMock));

        clienteService.desactivarCliente(1L);

        assertFalse(clienteMock.getActivo());
        assertEquals("INACTIVA", cuentaMock.getEstatus());
        verify(clienteRepository).save(clienteMock);
        verify(cuentaRepository).save(cuentaMock);
    }

    // Valida busqueda por nombre
    @Test
    void testBuscarClientesPorNombre() {
        when(clienteRepository.findByNombreContainingIgnoreCase("Juan")).thenReturn(List.of(clienteMock));
        when(domicilioRepository.findByClienteId(1L)).thenReturn(Optional.of(domicilioMock));
        when(cuentaRepository.findByClienteId(1L)).thenReturn(List.of(cuentaMock));

        List<ClienteRegistroResponseDto> resultado = clienteService.buscarClientesPorNombre("Juan");

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Juan", resultado.get(0).getNombre());
    }

    // Valida actualizacion parcial respetando CURP y RFC
    @Test
    void testActualizarCliente_Parcial() {
        ClienteActualizacionRequestDto actualizacion = ClienteActualizacionRequestDto.builder()
                .nombre("Juan")
                .ocupacion("Director de Tecnología")
                .ingresoMensual(new BigDecimal("50000.00"))
                .calle("Av. Reforma Sur")
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(clienteRepository.save(any(ClienteEntity.class))).thenReturn(clienteMock);
        when(domicilioRepository.findByClienteId(1L)).thenReturn(Optional.of(domicilioMock));
        when(domicilioRepository.save(any(DomicilioEntity.class))).thenReturn(domicilioMock);
        when(cuentaRepository.findByClienteId(1L)).thenReturn(List.of(cuentaMock));

        ClienteRegistroResponseDto respuesta = clienteService.actualizarCliente(1L, actualizacion);

        assertNotNull(respuesta);
        assertEquals("PELJ900515HDFRPR09", respuesta.getCurp());
        assertEquals("PELJ9005151A2", respuesta.getRfc());
        verify(clienteRepository).save(any(ClienteEntity.class));
    }
}
