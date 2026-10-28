package com.proyecto.servicios.config;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.proyecto.servicios.exception.EntidadNoProcesableException;
import com.proyecto.servicios.model.common.GenericResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    void testHandleValidationExceptions_Retorna400YMensajeDetonado() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError errorNombre = new FieldError("dto", "nombre", "El nombre no puede iniciar con espacios");
        FieldError errorSegundo = new FieldError("dto", "segundoNombre", "No se permiten multiples espacios internos consecutivos");
        FieldError errorApellido = new FieldError("dto", "apellidoMaterno", "El apellido debe tener al menos 2 caracteres reales");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(errorNombre, errorSegundo, errorApellido));

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        GenericResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getCodigo());
        assertEquals("El nombre no puede iniciar con espacios", body.getMensaje());

        Map<String, String> errores = body.getErrores();
        assertNotNull(errores);
        assertEquals("El nombre no puede iniciar con espacios", errores.get("nombre"));
        assertEquals("No se permiten multiples espacios internos consecutivos", errores.get("segundoNombre"));
        assertEquals("El apellido debe tener al menos 2 caracteres reales", errores.get("apellidoMaterno"));
    }

    @Test
    void testHandleAccessDeniedException_Retorna403() {
        AccessDeniedException ex = new AccessDeniedException("Acceso denegado: No tiene permisos para consultar o manipular los datos de otro usuario");

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleAccessDeniedException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        GenericResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(403, body.getCodigo());
        assertTrue(body.getMensaje().contains("Acceso denegado"));
    }

    @Test
    void testHandleHttpMessageNotReadable_NumeroInvalido() {
        InvalidFormatException cause = mock(InvalidFormatException.class);
        when(cause.getTargetType()).thenReturn((Class) BigDecimal.class);

        JsonMappingException.Reference ref = new JsonMappingException.Reference(null, "ingresoMensual");
        when(cause.getPath()).thenReturn(List.of(ref));

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", cause, null);

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        GenericResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getCodigo());
        assertEquals("El campo 'ingresoMensual' debe ser un valor numérico decimal válido", body.getMensaje());
    }

    @Test
    void testHandleHttpMessageNotReadable_ArregloUObjetoInvalido() {
        MismatchedInputException cause = mock(MismatchedInputException.class);
        when(cause.getTargetType()).thenReturn((Class) String.class);

        JsonMappingException.Reference ref = new JsonMappingException.Reference(null, "telefonoMovil");
        when(cause.getPath()).thenReturn(List.of(ref));

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", cause, null);

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        GenericResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getCodigo());
        assertEquals("El campo 'telefonoMovil' no debe ser un arreglo u objeto", body.getMensaje());
    }

    @Test
    void testHandleDataIntegrityViolation_Retorna400() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("NumericValueOutOfRange: A result was out of range");

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        GenericResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getCodigo());
        assertTrue(body.getMensaje().contains("límite"));
    }

    @Test
    void testHandleHttpMessageNotReadable_PropiedadNoReconocida_MassAssignment() {
        com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException cause = mock(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
        when(cause.getPropertyName()).thenReturn("rol");

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", cause, null);

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        GenericResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getCodigo());
        assertEquals("El campo 'rol' no es una propiedad permitida en la petición (propiedad no reconocida)", body.getMensaje());
    }

    @Test
    void testHandleHttpMessageNotReadable_SintaxisCorrupta_JsonParseException() {
        com.fasterxml.jackson.core.JsonParseException cause = mock(com.fasterxml.jackson.core.JsonParseException.class);

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", cause, null);

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        GenericResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getCodigo());
        assertEquals("La sintaxis del JSON es inválida o está incompleta (payload corrupto o mal formado)", body.getMensaje());
    }

    @Test
    void testHandleMethodArgumentTypeMismatch_LetrasEnParametroNumerico_Retorna400() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        when(ex.getValue()).thenReturn("letras-en-lugar-de-numero");
        when(ex.getRequiredType()).thenReturn((Class) Long.class);

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCodigo());
        assertTrue(response.getBody().getMensaje().contains("El parámetro 'id' con valor 'letras-en-lugar-de-numero' es inválido"));
    }

    @Test
    void testHandleHttpRequestMethodNotSupported_MetodoInvalido_Retorna405() {
        HttpRequestMethodNotSupportedException ex = mock(HttpRequestMethodNotSupportedException.class);
        when(ex.getMethod()).thenReturn("PATCH");
        when(ex.getSupportedHttpMethods()).thenReturn(Set.of(HttpMethod.GET, HttpMethod.POST));

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleHttpRequestMethodNotSupported(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(405, response.getBody().getCodigo());
        assertTrue(response.getBody().getMensaje().contains("El método HTTP 'PATCH' no está permitido"));
    }

    @Test
    void testHandleHttpMediaTypeNotSupported_FormatoNoSoportado_Retorna415() {
        HttpMediaTypeNotSupportedException ex = mock(HttpMediaTypeNotSupportedException.class);
        when(ex.getContentType()).thenReturn(MediaType.APPLICATION_XML);

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleHttpMediaTypeNotSupported(ex);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(415, response.getBody().getCodigo());
        assertTrue(response.getBody().getMensaje().contains("application/json"));
    }

    @Test
    void testHandleNoResourceFound_RutaInexistente_Retorna404() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "api/ruta-desconocida");

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleNoResourceFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getCodigo());
        assertTrue(response.getBody().getMensaje().contains("El recurso solicitado no fue encontrado"));
    }

    @Test
    void testHandleMissingServletRequestParameter_ParametroFaltante_Retorna400() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("pack", "Integer");

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleMissingServletRequestParameter(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCodigo());
        assertTrue(response.getBody().getMensaje().contains("El parámetro obligatorio 'pack' de tipo Integer no fue proporcionado"));
    }

    @Test
    void testHandleEntidadNoProcesable_Retorna422() {
        EntidadNoProcesableException ex = new EntidadNoProcesableException("El producto solicitado no está activo en el catálogo");

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleEntidadNoProcesable(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(422, response.getBody().getCodigo());
        assertEquals("El producto solicitado no está activo en el catálogo", response.getBody().getMensaje());
    }

    @Test
    void testHandleDataAccessException_Retorna422() {
        DataAccessException ex = mock(DataAccessException.class);
        when(ex.getMessage()).thenReturn("Integrity violation");

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleDataAccessException(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(422, response.getBody().getCodigo());
        assertTrue(response.getBody().getMensaje().contains("No fue posible procesar la entidad"));
    }

    @Test
    void testHandleGeneralException_Retorna500SanitizadoSinStackTrace() {
        Exception ex = new RuntimeException("Fallo interno crítico que no debe exponerse");

        ResponseEntity<GenericResponse<Object>> response = exceptionHandler.handleGeneralException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getCodigo());
        assertEquals("Ha ocurrido un error interno en el servidor. Por favor contacte al administrador.", response.getBody().getMensaje());
    }
}
