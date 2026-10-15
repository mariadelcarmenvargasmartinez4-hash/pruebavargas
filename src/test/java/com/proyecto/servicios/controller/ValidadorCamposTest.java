package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.common.GenericResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValidadorCamposTest {

    private ValidadorCampos validadorCampos;

    @BeforeEach
    void setUp() {
        validadorCampos = new ValidadorCampos();
    }

    @Test
    void testDtoValidation_ConErrorsDirecto_Retorna400YDetalleErrores() {
        BindingResult errors = mock(BindingResult.class);
        FieldError fe1 = new FieldError("dto", "nombre", "El nombre es obligatorio");
        FieldError fe2 = new FieldError("dto", "correoElectronico", "El formato de correo es inválido");

        when(errors.getFieldErrors()).thenReturn(List.of(fe1, fe2));

        ResponseEntity<GenericResponse<Object>> response = validadorCampos.dtoValidation(errors);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        GenericResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getCodigo());
        assertEquals("El nombre es obligatorio", body.getMensaje());
        assertNotNull(body.getErrores());
        assertEquals("El nombre es obligatorio", body.getErrores().get("nombre"));
        assertEquals("El formato de correo es inválido", body.getErrores().get("correoElectronico"));
    }

    @Test
    void testDtoValidation_ConMethodArgumentNotValidException_Retorna400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult errors = mock(BindingResult.class);
        FieldError fe = new FieldError("dto", "curp", "El CURP no cumple con el formato");

        when(ex.getBindingResult()).thenReturn(errors);
        when(errors.getFieldErrors()).thenReturn(List.of(fe));

        ResponseEntity<GenericResponse<Object>> response = validadorCampos.dtoValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("El CURP no cumple con el formato", response.getBody().getMensaje());
    }

    @Test
    void testDataException_DataIntegrityViolation_Retorna400() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Duplicate key violation or constraint error");

        ResponseEntity<GenericResponse<Object>> response = validadorCampos.dataException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMensaje().contains("integridad de datos"));
    }

    @Test
    void testDataParseException_HttpMessageNotReadable_Retorna400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", null, null);

        ResponseEntity<GenericResponse<Object>> response = validadorCampos.dataParseException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMensaje().contains("datos mal formados"));
    }
}
