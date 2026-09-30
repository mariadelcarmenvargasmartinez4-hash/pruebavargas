package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.CuentaBancariaEntity;
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

// Pruebas unitarias para ClienteServiceImpl
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

        ClienteEntity clienteGuardado = ClienteEntity.builder()
                .id(1L)
                .nombre("Juan")
                .segundoNombre("Carlos")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Lopez")
                .curp("PELJ900515HDFRPR09")
                .rfc("PELJ9005151A2")
                .correoElectronico("juan.perez@example.com")
                .telefonoMovil("5512345678")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .ingresoMensual(new BigDecimal("35000.00"))
                .build();

        CuentaBancariaEntity cuentaGuardada = CuentaBancariaEntity.builder()
                .id(10L)
                .cliente(clienteGuardado)
                .numeroCuenta("4815162342")
                .clabe("012180004815162342")
                .tipoCuenta("DEBITO")
                .saldo(new BigDecimal("1500.00"))
                .moneda("MXN")
                .estatus("ACTIVA")
                .build();

        when(clienteRepository.save(any(ClienteEntity.class))).thenReturn(clienteGuardado);
        when(cuentaBancariaRepository.save(any(CuentaBancariaEntity.class))).thenReturn(cuentaGuardada);

        ClienteRegistroResponseDto respuesta = clienteService.registrarCliente(requestValido);

        assertNotNull(respuesta);
        assertEquals(0, respuesta.getCodigo());
        assertEquals("Juan Carlos Perez Lopez", respuesta.getNombreCompleto());
        assertEquals("PELJ900515HDFRPR09", respuesta.getCurp());
        assertNotNull(respuesta.getCuentaBancaria());
        assertEquals("4815162342", respuesta.getCuentaBancaria().getNumeroCuenta());
        assertEquals(new BigDecimal("1500.00"), respuesta.getCuentaBancaria().getSaldo());

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

    // Valida rechazo si el saldo inicial es negativo
    @Test
    void testRegistrarCliente_SaldoInicialNegativo() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronico(anyString())).thenReturn(false);
        requestValido.setSaldoInicial(new BigDecimal("-10.00"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                clienteService.registrarCliente(requestValido));

        assertEquals(400, ex.getStatusCode().value());
    }

    // Valida consulta de cliente por ID
    @Test
    void testObtenerClientePorId_Exitoso() {
        ClienteEntity cliente = ClienteEntity.builder()
                .id(1L)
                .nombre("Juan")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Lopez")
                .curp("PELJ900515HDFRPR09")
                .rfc("PELJ9005151A2")
                .correoElectronico("juan.perez@example.com")
                .telefonoMovil("5512345678")
                .build();

        CuentaBancariaEntity cuenta = CuentaBancariaEntity.builder()
                .id(10L)
                .cliente(cliente)
                .numeroCuenta("4815162342")
                .clabe("012180004815162342")
                .saldo(new BigDecimal("500.00"))
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(cuentaBancariaRepository.findByClienteId(1L)).thenReturn(List.of(cuenta));

        ClienteRegistroResponseDto respuesta = clienteService.obtenerClientePorId(1L);

        assertNotNull(respuesta);
        assertEquals(1L, respuesta.getIdCliente());
        assertEquals("Juan Perez Lopez", respuesta.getNombreCompleto());
        assertEquals("4815162342", respuesta.getCuentaBancaria().getNumeroCuenta());
    }
}
