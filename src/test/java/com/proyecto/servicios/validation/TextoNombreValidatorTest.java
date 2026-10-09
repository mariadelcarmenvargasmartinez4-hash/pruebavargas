package com.proyecto.servicios.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TextoNombreValidatorTest {

    private Validator validator;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testNombreConEspaciosAlInicio() {
        ClienteRegistroRequestDto dto = new ClienteRegistroRequestDto();
        dto.setNombre(" Ana");
        Set<ConstraintViolation<ClienteRegistroRequestDto>> violations = validator.validateProperty(dto, "nombre");
        assertFalse(violations.isEmpty(), "Debe fallar si inicia con espacio");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("no puede iniciar con espacios")));
    }

    @Test
    public void testNombreConEspaciosAlFinal() {
        ClienteRegistroRequestDto dto = new ClienteRegistroRequestDto();
        dto.setNombre("Ana   ");
        Set<ConstraintViolation<ClienteRegistroRequestDto>> violations = validator.validateProperty(dto, "nombre");
        assertFalse(violations.isEmpty(), "Debe fallar si termina con espacio");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("no puede terminar con espacios")));
    }

    @Test
    public void testSegundoNombreConEspaciosAlInicio() {
        ClienteRegistroRequestDto dto = new ClienteRegistroRequestDto();
        dto.setSegundoNombre(" Carlos");
        Set<ConstraintViolation<ClienteRegistroRequestDto>> violations = validator.validateProperty(dto, "segundoNombre");
        assertFalse(violations.isEmpty(), "Debe fallar si segundoNombre inicia con espacios");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("no puede iniciar con espacios")));
    }

    @Test
    public void testSegundoNombreConMuchosEspaciosAlFinal() {
        ClienteRegistroRequestDto dto = new ClienteRegistroRequestDto();
        dto.setSegundoNombre("Carlos    ");
        Set<ConstraintViolation<ClienteRegistroRequestDto>> violations = validator.validateProperty(dto, "segundoNombre");
        assertFalse(violations.isEmpty(), "Debe fallar si segundoNombre termina con muchos espacios");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("no puede terminar con espacios")));
    }

    @Test
    public void testSegundoNombreMenosDe2Caracteres() {
        ClienteRegistroRequestDto dto = new ClienteRegistroRequestDto();
        dto.setSegundoNombre("A");
        Set<ConstraintViolation<ClienteRegistroRequestDto>> violations = validator.validateProperty(dto, "segundoNombre");
        assertFalse(violations.isEmpty(), "Debe fallar si segundoNombre tiene menos de 2 caracteres");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("debe tener al menos 2 caracteres")));
    }

    @Test
    public void testSegundoNombreMasDe50Caracteres() {
        ClienteRegistroRequestDto dto = new ClienteRegistroRequestDto();
        dto.setSegundoNombre("A".repeat(51));
        Set<ConstraintViolation<ClienteRegistroRequestDto>> violations = validator.validateProperty(dto, "segundoNombre");
        assertFalse(violations.isEmpty(), "Debe fallar si segundoNombre excede 50 caracteres");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("no puede exceder 50 caracteres")));
    }

    @Test
    public void testSegundoNombreConNumeros() {
        ClienteRegistroRequestDto dto = new ClienteRegistroRequestDto();
        dto.setSegundoNombre("Carlos123");
        Set<ConstraintViolation<ClienteRegistroRequestDto>> violations = validator.validateProperty(dto, "segundoNombre");
        assertFalse(violations.isEmpty(), "Debe fallar si segundoNombre contiene números");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("solo puede aceptar letras")));
    }

    @Test
    public void testSegundoNombreVacioONuloValido() {
        ClienteRegistroRequestDto dto = new ClienteRegistroRequestDto();
        dto.setSegundoNombre(null);
        assertTrue(validator.validateProperty(dto, "segundoNombre").isEmpty(), "Debe ser valido si es null (opcional)");

        dto.setSegundoNombre("");
        assertTrue(validator.validateProperty(dto, "segundoNombre").isEmpty(), "Debe ser valido si es vacio (opcional)");
    }

    @Test
    public void testSegundoNombreValido() {
        ClienteRegistroRequestDto dto = new ClienteRegistroRequestDto();
        dto.setSegundoNombre("Carlos");
        assertTrue(validator.validateProperty(dto, "segundoNombre").isEmpty(), "Debe ser valido con nombre correcto");
    }
}
