package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.DomicilioEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.model.cliente.ClienteActualizacionRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroResponseDto;
import com.proyecto.servicios.model.util.MetricasTextoDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.DomicilioRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import com.proyecto.servicios.service.Impl.ClienteServiceImpl;
import com.proyecto.servicios.util.GeneradorCuentaBancariaUtil;
import com.proyecto.servicios.util.MetricasTextoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private UsuarioAccesoRepository usuarioAccesoRepository;

    @Mock
    private GeneradorCuentaBancariaUtil generadorCuentaUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SexoService sexoService;

    @Mock
    private NacionalidadService nacionalidadService;

    @Mock
    private EstadoCivilService estadoCivilService;

    @Mock
    private MetricasTextoUtil metricasTextoUtil;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private ClienteRegistroRequestDto requestValido;
    private ClienteEntity clienteMock;
    private DomicilioEntity domicilioMock;
    private CuentaEntity cuentaMock;
    private UsuarioAccesoEntity usuarioMock;

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
                .nacionalidad("MEXICANA")
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
                .nacionalidad("MEXICANA")
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

        usuarioMock = UsuarioAccesoEntity.builder()
                .id(1L)
                .cliente(clienteMock)
                .username("juan.perez@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    // Valida registro exitoso en clientes, domicilios, cuentas y usuario de acceso
    @Test
    void testRegistrarCliente_Exitoso() {
        requestValido.setPassword("Segura123#");
        when(sexoService.esValidoYActivo(anyString())).thenReturn(true);
        when(nacionalidadService.esValidoYActivo(anyString())).thenReturn(true);
        when(estadoCivilService.esValidoYActivo(anyString())).thenReturn(true);
        when(metricasTextoUtil.analizar(any(), any())).thenReturn(MetricasTextoDto.builder().build());

        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronico(anyString())).thenReturn(false);
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(generadorCuentaUtil.generarNumeroCuenta()).thenReturn("4815162342");
        when(generadorCuentaUtil.generarClabe("4815162342")).thenReturn("012180004815162342");
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedpassword");
        when(clienteRepository.save(any(ClienteEntity.class))).thenReturn(clienteMock);
        when(domicilioRepository.save(any(DomicilioEntity.class))).thenReturn(domicilioMock);
        when(cuentaRepository.save(any(CuentaEntity.class))).thenReturn(cuentaMock);
        when(usuarioAccesoRepository.save(any(UsuarioAccesoEntity.class))).thenReturn(usuarioMock);

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
        assertNotNull(respuesta.getUsuarioAcceso());
        assertEquals("juan.perez@example.com", respuesta.getUsuarioAcceso().getUsername());
        assertEquals(true, respuesta.getUsuarioAcceso().getActivo());

        verify(clienteRepository).save(any(ClienteEntity.class));
        verify(domicilioRepository).save(any(DomicilioEntity.class));
        verify(cuentaRepository).save(any(CuentaEntity.class));
        verify(usuarioAccesoRepository).save(any(UsuarioAccesoEntity.class));
    }

    // Valida que no se permita registrar clientes menores de 18 anos
    @Test
    void testRegistrarCliente_MenorDeEdad_LanzaExcepcion() {
        requestValido.setFechaNacimiento(LocalDate.now().minusYears(17));

        ReglaNegocioException excepcion = assertThrows(
                ReglaNegocioException.class,
                () -> clienteService.registrarCliente(requestValido)
        );

        assertEquals("El cliente debe ser mayor de edad (18 años o más)", excepcion.getMessage());
    }

    // Valida que no se permita registrar clientes mayores a 100 anos
    @Test
    void testRegistrarCliente_MayorDe100Anos_LanzaExcepcion() {
        requestValido.setFechaNacimiento(LocalDate.now().minusYears(101));

        ReglaNegocioException excepcion = assertThrows(
                ReglaNegocioException.class,
                () -> clienteService.registrarCliente(requestValido)
        );

        assertEquals("La fecha de nacimiento no debe exceder los 100 años", excepcion.getMessage());
    }

    // Valida error de negocio si el sexo no existe en catálogo
    @Test
    void testRegistrarCliente_SexoInvalido_LanzaExcepcion() {
        when(sexoService.esValidoYActivo(anyString())).thenReturn(false);

        ReglaNegocioException excepcion = assertThrows(
                ReglaNegocioException.class,
                () -> clienteService.registrarCliente(requestValido)
        );

        assertNotNull(excepcion.getMessage());
    }

    // Valida unicidad de CURP
    @Test
    void testRegistrarCliente_CurpDuplicada_LanzaExcepcion() {
        when(sexoService.esValidoYActivo(anyString())).thenReturn(true);
        when(nacionalidadService.esValidoYActivo(anyString())).thenReturn(true);
        when(estadoCivilService.esValidoYActivo(anyString())).thenReturn(true);
        when(clienteRepository.existsByCurp(anyString())).thenReturn(true);

        assertThrows(CurpDuplicadaException.class, () -> clienteService.registrarCliente(requestValido));
    }

    // Valida unicidad de RFC
    @Test
    void testRegistrarCliente_RfcDuplicado_LanzaExcepcion() {
        when(sexoService.esValidoYActivo(anyString())).thenReturn(true);
        when(nacionalidadService.esValidoYActivo(anyString())).thenReturn(true);
        when(estadoCivilService.esValidoYActivo(anyString())).thenReturn(true);
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(true);

        assertThrows(RfcDuplicadoException.class, () -> clienteService.registrarCliente(requestValido));
    }

    // Valida consulta por CURP exitosa
    @Test
    void testObtenerClientePorCurp_Exitoso() {
        when(clienteRepository.findByCurp("PELJ900515HDFRPR09")).thenReturn(Optional.of(clienteMock));
        when(domicilioRepository.findByClienteId(1L)).thenReturn(Optional.of(domicilioMock));
        when(cuentaRepository.findByClienteId(1L)).thenReturn(List.of(cuentaMock));

        ClienteRegistroResponseDto respuesta = clienteService.obtenerClientePorCurp("PELJ900515HDFRPR09");

        assertNotNull(respuesta);
        assertEquals("PELJ900515HDFRPR09", respuesta.getCurp());
        assertEquals("Juan Carlos Perez Lopez", respuesta.getNombreCompleto());
    }

    // Valida baja logica (cliente inactivo y cuenta inactiva)
    @Test
    void testDesactivarCliente_BajaLogica() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(cuentaRepository.findByClienteId(1L)).thenReturn(List.of(cuentaMock));
        when(usuarioAccesoRepository.findByClienteId(1L)).thenReturn(Optional.of(usuarioMock));

        clienteService.desactivarCliente(1L);

        assertFalse(clienteMock.getActivo());
        assertEquals("INACTIVA", cuentaMock.getEstatus());
        assertFalse(usuarioMock.getActivo());

        verify(clienteRepository).save(clienteMock);
        verify(cuentaRepository).save(cuentaMock);
        verify(usuarioAccesoRepository).save(usuarioMock);
    }

    // Valida reactivacion de cliente
    @Test
    void testReactivarCliente_Exitoso() {
        clienteMock.setActivo(false);
        usuarioMock.setActivo(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(usuarioAccesoRepository.findByClienteId(1L)).thenReturn(Optional.of(usuarioMock));
        when(domicilioRepository.findByClienteId(1L)).thenReturn(Optional.of(domicilioMock));
        when(cuentaRepository.findByClienteId(1L)).thenReturn(List.of(cuentaMock));

        ClienteRegistroResponseDto respuesta = clienteService.reactivarCliente(1L);

        assertNotNull(respuesta);
        assertEquals(true, clienteMock.getActivo());
        assertEquals(true, usuarioMock.getActivo());
        verify(clienteRepository).save(clienteMock);
        verify(usuarioAccesoRepository).save(usuarioMock);
    }

    // Valida excepcion cuando el cliente no existe
    @Test
    void testObtenerClientePorId_NoEncontrado() {
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.obtenerClientePorId(999L));
    }
}
