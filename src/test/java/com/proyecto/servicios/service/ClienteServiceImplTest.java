package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.CuentaBancariaEntity;
import com.proyecto.servicios.model.cliente.ClienteActualizacionRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroResponseDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaBancariaRepository;
import com.proyecto.servicios.service.Impl.ClienteServiceImpl;
import com.proyecto.servicios.util.GeneradorCuentaBancariaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pruebas unitarias para ClienteServiceImpl cubriendo registro, consultas especializadas y actualizacion
@ExtendWith(MockitoExtension.class)
public class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CuentaBancariaRepository cuentaBancariaRepository;

    @Mock
    private GeneradorCuentaBancariaUtil generadorCuentaUtil;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private ClienteRegistroRequestDto requestValido;
    private ClienteEntity clienteMock;
    private CuentaBancariaEntity cuentaMock;

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
                .calle("Av. Reforma")
                .numeroExterior("123")
                .colonia("Juarez")
                .municipio("Cuauhtemoc")
                .estado("Ciudad de Mexico")
                .codigoPostal("06600")
                .pais("Mexico")
                .ocupacion("Ingeniero de Software")
                .empresa("Tech Solutions SA")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .ingresoMensual(new BigDecimal("35000.00"))
                .build();

        cuentaMock = CuentaBancariaEntity.builder()
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

    // Valida registro exitoso de cliente y creacion de cuenta bancaria
    @Test
    void testRegistrarCliente_Exitoso() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronico(anyString())).thenReturn(false);
        when(cuentaBancariaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(generadorCuentaUtil.generarNumeroCuenta()).thenReturn("4815162342");
        when(generadorCuentaUtil.generarClabe("4815162342")).thenReturn("012180004815162342");
        when(clienteRepository.save(any(ClienteEntity.class))).thenReturn(clienteMock);
        when(cuentaBancariaRepository.save(any(CuentaBancariaEntity.class))).thenReturn(cuentaMock);

        ClienteRegistroResponseDto respuesta = clienteService.registrarCliente(requestValido);

        assertNotNull(respuesta);
        assertEquals(0, respuesta.getCodigo());
        assertEquals("Juan Carlos Perez Lopez", respuesta.getNombreCompleto());
        assertEquals("PELJ900515HDFRPR09", respuesta.getCurp());
        assertNotNull(respuesta.getCuentaBancaria());
        assertEquals("4815162342", respuesta.getCuentaBancaria().getNumeroCuenta());
        assertEquals(new BigDecimal("1500.00"), respuesta.getCuentaBancaria().getSaldo());
        assertEquals("ACTIVA", respuesta.getCuentaBancaria().getEstatus());

        verify(clienteRepository).save(any(ClienteEntity.class));
        verify(cuentaBancariaRepository).save(any(CuentaBancariaEntity.class));
    }

    // Valida rechazo de registro si el cliente es menor de edad
    @Test
    void testRegistrarCliente_MenorDeEdad() {
        requestValido.setFechaNacimiento(LocalDate.now().minusYears(17));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                clienteService.registrarCliente(requestValido));

        assertEquals("El cliente debe ser mayor de edad (minimo 18 anos)", ex.getReason());
    }

    // Valida rechazo de registro si el CURP ya existe
    @Test
    void testRegistrarCliente_CurpDuplicado() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                clienteService.registrarCliente(requestValido));

        assertEquals(409, ex.getStatusCode().value());
    }

    // Valida rechazo de registro si el RFC ya existe
    @Test
    void testRegistrarCliente_RfcDuplicado() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                clienteService.registrarCliente(requestValido));

        assertEquals(409, ex.getStatusCode().value());
    }

    // Valida consulta de todos los clientes
    @Test
    void testListarClientes_Exitoso() {
        when(clienteRepository.findAll()).thenReturn(List.of(clienteMock));
        when(cuentaBancariaRepository.findByClienteId(1L)).thenReturn(List.of(cuentaMock));

        List<ClienteRegistroResponseDto> lista = clienteService.listarClientes();

        assertNotNull(lista);
        assertEquals(1, lista.size());
        assertEquals("Juan Carlos Perez Lopez", lista.get(0).getNombreCompleto());
    }

    // Valida consulta de cliente por ID
    @Test
    void testObtenerClientePorId_Exitoso() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(cuentaBancariaRepository.findByClienteId(1L)).thenReturn(List.of(cuentaMock));

        ClienteRegistroResponseDto respuesta = clienteService.obtenerClientePorId(1L);

        assertNotNull(respuesta);
        assertEquals(1L, respuesta.getIdCliente());
        assertEquals("Juan Carlos Perez Lopez", respuesta.getNombreCompleto());
        assertEquals("4815162342", respuesta.getCuentaBancaria().getNumeroCuenta());
    }

    // Valida consulta de cliente por CURP
    @Test
    void testObtenerClientePorCurp_Exitoso() {
        when(clienteRepository.findByCurp("PELJ900515HDFRPR09")).thenReturn(Optional.of(clienteMock));
        when(cuentaBancariaRepository.findByClienteId(1L)).thenReturn(List.of(cuentaMock));

        ClienteRegistroResponseDto respuesta = clienteService.obtenerClientePorCurp("PELJ900515HDFRPR09");

        assertNotNull(respuesta);
        assertEquals("PELJ900515HDFRPR09", respuesta.getCurp());
        assertEquals("4815162342", respuesta.getCuentaBancaria().getNumeroCuenta());
    }

    // Valida consulta de cliente por RFC
    @Test
    void testObtenerClientePorRfc_Exitoso() {
        when(clienteRepository.findByRfc("PELJ9005151A2")).thenReturn(Optional.of(clienteMock));
        when(cuentaBancariaRepository.findByClienteId(1L)).thenReturn(List.of(cuentaMock));

        ClienteRegistroResponseDto respuesta = clienteService.obtenerClientePorRfc("PELJ9005151A2");

        assertNotNull(respuesta);
        assertEquals("PELJ9005151A2", respuesta.getRfc());
        assertEquals("4815162342", respuesta.getCuentaBancaria().getNumeroCuenta());
    }

    // Valida consulta de cliente por numero de cuenta
    @Test
    void testObtenerClientePorNumeroCuenta_Exitoso() {
        when(cuentaBancariaRepository.findByNumeroCuenta("4815162342")).thenReturn(Optional.of(cuentaMock));

        ClienteRegistroResponseDto respuesta = clienteService.obtenerClientePorNumeroCuenta("4815162342");

        assertNotNull(respuesta);
        assertEquals("4815162342", respuesta.getCuentaBancaria().getNumeroCuenta());
        assertEquals("PELJ900515HDFRPR09", respuesta.getCurp());
    }

    // Valida actualizacion exitosa respetando inmutabilidad de CURP y RFC
    @Test
    void testActualizarCliente_Exitoso() {
        ClienteActualizacionRequestDto actualizacion = ClienteActualizacionRequestDto.builder()
                .nombre("Juan")
                .segundoNombre("Carlos")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Lopez")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .sexo("MASCULINO")
                .nacionalidad("Mexicana")
                .estadoCivil("CASADO")
                .correoElectronico("juan.perez.nuevo@example.com")
                .telefonoMovil("5599887766")
                .calle("Av. Insurgentes Sur")
                .numeroExterior("456")
                .colonia("Del Valle")
                .municipio("Benito Juarez")
                .estado("Ciudad de Mexico")
                .codigoPostal("03100")
                .pais("Mexico")
                .ocupacion("Líder Técnico")
                .empresa("Banco Innovador SA")
                .ingresoMensual(new BigDecimal("45000.00"))
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(clienteRepository.existsByCorreoElectronico("juan.perez.nuevo@example.com")).thenReturn(false);
        when(clienteRepository.save(any(ClienteEntity.class))).thenReturn(clienteMock);
        when(cuentaBancariaRepository.findByClienteId(1L)).thenReturn(List.of(cuentaMock));

        ClienteRegistroResponseDto respuesta = clienteService.actualizarCliente(1L, actualizacion);

        assertNotNull(respuesta);
        assertEquals("PELJ900515HDFRPR09", respuesta.getCurp()); // CURP intacto
        assertEquals("PELJ9005151A2", respuesta.getRfc()); // RFC intacto
        assertEquals("4815162342", respuesta.getCuentaBancaria().getNumeroCuenta()); // Cuenta intacta
        verify(clienteRepository).save(any(ClienteEntity.class));
    }
}
