package com.proyecto.servicios.config;

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

// Manejador global de excepciones para respuestas claras y estandarizadas
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
        body.put("error", "Error de validacion en los datos de entrada");
        body.put("detalle", errores);

        log.warn("Error de validacion en peticion: {}", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Manejo de excepciones de negocio ResponseStatusException
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("codigo", ex.getStatusCode().value());
        body.put("error", ex.getReason());

        log.warn("Excepcion de negocio ({}): {}", ex.getStatusCode(), ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    // Manejo de cualquier error no controlado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("codigo", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Ha ocurrido un error interno en el servidor");
        body.put("mensaje", ex.getMessage());

        log.error("Error no controlado en la aplicacion: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
