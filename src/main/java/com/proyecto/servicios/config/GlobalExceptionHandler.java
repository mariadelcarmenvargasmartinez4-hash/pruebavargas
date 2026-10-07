package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.CatalogoDuplicadoException;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.PasswordInvalidaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// Manejador global de excepciones para estructurar respuestas HTTP estandarizadas
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Manejo de errores de validacion de campos (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("codigo", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Error de validacion");
        body.put("detalle", errores);

        log.warn("Error de validacion en datos de entrada: {}", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Excepcion personalizada: CURP duplicada
    @ExceptionHandler(CurpDuplicadaException.class)
    public ResponseEntity<Map<String, Object>> handleCurpDuplicadaException(CurpDuplicadaException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "CURP duplicada", ex.getMessage());
    }

    // Excepcion personalizada: RFC duplicado
    @ExceptionHandler(RfcDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleRfcDuplicadoException(RfcDuplicadoException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "RFC duplicado", ex.getMessage());
    }

    // Excepcion personalizada: Cliente ya registrado
    @ExceptionHandler(ClienteYaRegistradoException.class)
    public ResponseEntity<Map<String, Object>> handleClienteYaRegistradoException(ClienteYaRegistradoException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Cliente ya registrado", ex.getMessage());
    }

    // Excepcion personalizada: Cliente no encontrado
    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleClienteNoEncontradoException(ClienteNoEncontradoException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Cliente no encontrado", ex.getMessage());
    }

    // Excepcion personalizada: Cuenta no encontrada
    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleCuentaNoEncontradaException(CuentaNoEncontradaException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Cuenta no encontrada", ex.getMessage());
    }

    // Excepcion personalizada: Error de Regla de Negocio
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleReglaNegocioException(ReglaNegocioException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Regla de negocio no satisfecha", ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, Object>> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Credenciales invalidas", ex.getMessage());
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioInactivo(UsuarioInactivoException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Usuario inactivo", ex.getMessage());
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Usuario no encontrado", ex.getMessage());
    }

    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleCorreoDuplicado(CorreoDuplicadoException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Correo electronico duplicado", ex.getMessage());
    }

    @ExceptionHandler(PasswordInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handlePasswordInvalida(PasswordInvalidaException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Contrasena invalida", ex.getMessage());
    }

    @ExceptionHandler(CatalogoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleCatalogoNoEncontrado(CatalogoNoEncontradoException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Elemento de catálogo no encontrado", ex.getMessage());
    }

    @ExceptionHandler(CatalogoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleCatalogoDuplicado(CatalogoDuplicadoException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Elemento de catálogo duplicado", ex.getMessage());
    }

    // Excepciones ResponseStatusException
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        return buildErrorResponse(HttpStatus.valueOf(ex.getStatusCode().value()), "Error en la peticion", ex.getReason());
    }

    // Excepciones no controladas
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("codigo", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Error interno en el servidor");
        body.put("mensaje", ex.getMessage());

        log.error("Excepcion no controlada: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String titulo, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("codigo", status.value());
        body.put("error", titulo);
        body.put("mensaje", mensaje);
        log.warn("{}: {}", titulo, mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
