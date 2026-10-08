package com.proyecto.servicios.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValidatorsTest {

    private TextoNombreValidator nombreValidator;
    private FechaNacimientoValidator fechaValidator;
    private ConstraintValidatorContext context;
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        nombreValidator = new TextoNombreValidator();
        fechaValidator = new FechaNacimientoValidator();
        context = mock(ConstraintValidatorContext.class);
        violationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    }

    @Test
    void testNombre_EspaciosAlInicioOFin() {
        ValidTextoNombre annotation = mock(ValidTextoNombre.class);
        when(annotation.tipo()).thenReturn("nombre");
        when(annotation.obligatorio()).thenReturn(true);
        nombreValidator.initialize(annotation);

        assertFalse(nombreValidator.isValid(" Juan", context));
        assertFalse(nombreValidator.isValid("Juan ", context));
        assertTrue(nombreValidator.isValid("Juan", context));
    }

    @Test
    void testNombre_MultiplesEspaciosInternos() {
        ValidTextoNombre annotation = mock(ValidTextoNombre.class);
        when(annotation.tipo()).thenReturn("segundoNombre");
        when(annotation.obligatorio()).thenReturn(false);
        nombreValidator.initialize(annotation);

        assertFalse(nombreValidator.isValid("Carlos  Eduardo", context));
        assertTrue(nombreValidator.isValid("Carlos Eduardo", context));
    }

    @Test
    void testNombre_PalabrasReservadas() {
        ValidTextoNombre annotation = mock(ValidTextoNombre.class);
        when(annotation.tipo()).thenReturn("nombre");
        when(annotation.obligatorio()).thenReturn(true);
        nombreValidator.initialize(annotation);

        assertFalse(nombreValidator.isValid("execute", context));
        assertFalse(nombreValidator.isValid("Si", context));
        assertFalse(nombreValidator.isValid("No", context));
        assertFalse(nombreValidator.isValid("SELECT", context));
        assertTrue(nombreValidator.isValid("Carlos", context));
    }

    @Test
    void testNombre_LongitudMinimaReales() {
        ValidTextoNombre annotation = mock(ValidTextoNombre.class);
        when(annotation.tipo()).thenReturn("apellido");
        when(annotation.obligatorio()).thenReturn(true);
        nombreValidator.initialize(annotation);

        assertFalse(nombreValidator.isValid("A", context));
        assertTrue(nombreValidator.isValid("Li", context));
    }

    @Test
    void testFechaNacimiento_Mayor100Anos_Invalido() {
        LocalDate hace101Anos = LocalDate.now().minusYears(101);
        assertFalse(fechaValidator.isValid(hace101Anos, context));
    }

    @Test
    void testFechaNacimiento_Menor18Anos_Invalido() {
        LocalDate hace17Anos = LocalDate.now().minusYears(17);
        assertFalse(fechaValidator.isValid(hace17Anos, context));
    }

    @Test
    void testFechaNacimiento_Valida() {
        LocalDate hace30Anos = LocalDate.now().minusYears(30);
        assertTrue(fechaValidator.isValid(hace30Anos, context));
    }
}
