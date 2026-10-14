package com.proyecto.servicios.config;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleValidationExceptions_Retorna200YMensajeDetonado() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError errorNombre = new FieldError("dto", "nombre", "El nombre no puede iniciar con espacios");
        FieldError errorSegundo = new FieldError("dto", "segundoNombre", "No se permiten multiples espacios internos consecutivos");
        FieldError errorApellido = new FieldError("dto", "apellidoMaterno", "El apellido debe tener al menos 2 caracteres reales");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(errorNombre, errorSegundo, errorApellido));

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        GenericResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(200, body.getCodigo());
        assertEquals("El nombre no puede iniciar con espacios", body.getMensaje());

        Map<String, String> errores = body.getErrores();
        assertNotNull(errores);
        assertEquals("El nombre no puede iniciar con espacios", errores.get("nombre"));
        assertEquals("No se permiten multiples espacios internos consecutivos", errores.get("segundoNombre"));
        assertEquals("El apellido debe tener al menos 2 caracteres reales", errores.get("apellidoMaterno"));
    }

    @Test
    void testHandleHttpMessageNotReadable_NumeroInvalido() {
        InvalidFormatException cause = mock(InvalidFormatException.class);
        when(cause.getTargetType()).thenReturn((Class) BigDecimal.class);

        JsonMappingException.Reference ref = new JsonMappingException.Reference(null, "ingresoMensual");
        when(cause.getPath()).thenReturn(List.of(ref));

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", cause, null);

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        GenericResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(200, body.getCodigo());
        assertEquals("El campo 'ingresoMensual' debe ser un valor numerico decimal valido", body.getMensaje());
    }

    @Test
    void testHandleHttpMessageNotReadable_ArregloUObjetoInvalido() {
        MismatchedInputException cause = mock(MismatchedInputException.class);
        when(cause.getTargetType()).thenReturn((Class) String.class);

        JsonMappingException.Reference ref = new JsonMappingException.Reference(null, "telefonoMovil");
        when(cause.getPath()).thenReturn(List.of(ref));

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", cause, null);

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        GenericResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(200, body.getCodigo());
        assertEquals("El campo 'telefonoMovil' no debe ser un arreglo u objeto", body.getMensaje());
    }

    @Test
    void testHandleDataIntegrityViolation_EvitaHttp500() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("NumericValueOutOfRange: A result was out of range");

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        GenericResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(200, body.getCodigo());
        assertTrue(body.getMensaje().contains("límite"));
    }

    @Test
    void testHandleHttpMessageNotReadable_PropiedadNoReconocida_MassAssignment() {
        com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException cause = mock(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
        when(cause.getPropertyName()).thenReturn("rol");

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", cause, null);

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        GenericResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(200, body.getCodigo());
        assertEquals("El campo 'rol' no es una propiedad permitida en la peticion (propiedad no reconocida)", body.getMensaje());
    }

    @Test
    void testHandleHttpMessageNotReadable_SintaxisCorrupta_JsonParseException() {
        com.fasterxml.jackson.core.JsonParseException cause = mock(com.fasterxml.jackson.core.JsonParseException.class);

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", cause, null);

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        GenericResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(200, body.getCodigo());
        assertEquals("La sintaxis del JSON es invalida o esta incompleta (payload corrupto o conexion interrumpida)", body.getMensaje());
    }
}
