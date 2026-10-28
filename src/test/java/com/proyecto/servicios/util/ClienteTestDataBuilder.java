package com.proyecto.servicios.util;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.DomicilioEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequestDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Test Data Builder para crear entidades y DTOs de Cliente de forma limpia,
 * modular, reutilizable y dinámica para pruebas unitarias.
 */
public final class ClienteTestDataBuilder {

    private ClienteTestDataBuilder() {
    }

    /**
     * Retorna un builder preconfigurado con datos válidos por defecto para ClienteRegistroRequestDto.
     * Permite sobreescribir únicamente los campos necesarios en cada prueba.
     */
    public static ClienteRegistroRequestDto.ClienteRegistroRequestDtoBuilder unClienteRequest() {
        long sufijo = ThreadLocalRandom.current().nextLong(1000, 9999);
        return ClienteRegistroRequestDto.builder()
                .nombre("Juan")
                .segundoNombre("Carlos")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Lopez")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .curp("PELJ900515HDFRPR" + String.format("%02d", sufijo % 100))
                .rfc("PELJ9005151A" + (sufijo % 10))
                .sexo("MASCULINO")
                .nacionalidad("MEXICANA")
                .estadoCivil("SOLTERO")
                .correoElectronico("juan.perez" + sufijo + "@example.com")
                .telefonoMovil("55" + String.format("%08d", ThreadLocalRandom.current().nextLong(10000000, 99999999)))
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
                .password(TestCredentials.validPass());
    }

    /**
     * Instancia un ClienteRegistroRequestDto válido con datos canónicos estándar.
     */
    public static ClienteRegistroRequestDto requestValido() {
        return unClienteRequest()
                .curp("PELJ900515HDFRPR09")
                .rfc("PELJ9005151A2")
                .correoElectronico("juan.perez@example.com")
                .telefonoMovil("5512345678")
                .build();
    }

    /**
     * Builder preconfigurado para ClienteEntity con datos válidos por defecto.
     */
    public static ClienteEntity.ClienteEntityBuilder unClienteEntity() {
        return ClienteEntity.builder()
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
                .fechaActualizacion(LocalDateTime.now());
    }

    /**
     * Instancia una entidad ClienteEntity válida.
     */
    public static ClienteEntity clienteValido() {
        return unClienteEntity().build();
    }

    /**
     * Retorna una entidad DomicilioEntity válida asociada al cliente proporcionado.
     */
    public static DomicilioEntity domicilioValido(ClienteEntity cliente) {
        return DomicilioEntity.builder()
                .id(1L)
                .cliente(cliente != null ? cliente : clienteValido())
                .calle("Av. Reforma")
                .numeroExterior("123")
                .numeroInterior("Piso 4")
                .colonia("Juarez")
                .municipio("Cuauhtemoc")
                .estado("Ciudad de Mexico")
                .codigoPostal("06600")
                .pais("Mexico")
                .build();
    }

    /**
     * Retorna un UsuarioAccesoEntity válido asociado al cliente proporcionado.
     */
    public static UsuarioAccesoEntity usuarioValido(ClienteEntity cliente) {
        return UsuarioAccesoEntity.builder()
                .id(1L)
                .cliente(cliente != null ? cliente : clienteValido())
                .username(cliente != null && cliente.getCorreoElectronico() != null ? cliente.getCorreoElectronico() : "juan.perez@example.com")
                .passwordHash(TestCredentials.testPassHash())
                .rol("CLIENTE")
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }
}
