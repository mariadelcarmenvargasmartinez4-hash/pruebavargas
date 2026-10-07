package com.proyecto.servicios.config;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
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
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

// Manejador global de excepciones para estructurar respuestas HTTP estandarizadas
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Manejo de errores de validación de campos (@Valid) con el formato exacto solicitado
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("codigo", HttpStatus.BAD_REQUEST.value());
        body.put("mensaje", "Error de validacion de campos");
        body.put("errores", errores);

        log.warn("Error de validacion de campos: {}", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 2. Manejo de errores de malformación JSON o tipos incompatibles (evita HTTP 500 y extrae el campo exacto)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String mensaje = "El cuerpo de la peticion contiene datos mal formados o con formato invalido";
        Throwable cause = ex.getCause();

        if (cause instanceof JsonParseException jpe) {
            mensaje = "La sintaxis del JSON es invalida o esta incompleta (payload corrupto o conexion interrumpida)";
        } else if (cause instanceof UnrecognizedPropertyException upe) {
            String campo = upe.getPropertyName();
            mensaje = "El campo '" + campo + "' no es una propiedad permitida en la peticion (propiedad no reconocida)";
        } else if (cause instanceof InvalidFormatException ife) {
            String campo = ife.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .reduce((first, second) -> second)
                    .orElse("solicitado");

            Class<?> targetType = ife.getTargetType();
            if (BigDecimal.class.isAssignableFrom(targetType) || Number.class.isAssignableFrom(targetType)) {
                mensaje = "El campo '" + campo + "' debe ser un valor numerico decimal valido";
            } else if (LocalDate.class.isAssignableFrom(targetType)) {
                mensaje = "El campo '" + campo + "' debe ser una fecha valida con formato AAAA-MM-DD";
            } else {
                mensaje = "El campo '" + campo + "' contiene un tipo de dato invalido";
            }
        } else if (cause instanceof MismatchedInputException mie) {
            String campo = mie.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .reduce((first, second) -> second)
                    .orElse("solicitado");

            Class<?> targetType = mie.getTargetType();
            if (BigDecimal.class.isAssignableFrom(targetType) || Number.class.isAssignableFrom(targetType)) {
                mensaje = "El campo '" + campo + "' debe ser un valor numerico decimal valido";
            } else {
                mensaje = "El campo '" + campo + "' no debe ser un arreglo u objeto";
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("codigo", HttpStatus.BAD_REQUEST.value());
        body.put("mensaje", mensaje);

        log.warn("Error de lectura JSON capturado: {}", mensaje);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 3. Manejo de valores numéricos gigantescos o fuera de rango en base de datos (evita HTTP 500)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String mensaje = "El valor proporcionado excede el límite máximo permitido o viola una restricción de integridad";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("codigo", HttpStatus.BAD_REQUEST.value());
        body.put("mensaje", mensaje);

        log.warn("Violacion de integridad de datos / desbordamiento numerico: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 4. Manejo de violaciones de restricciones en parámetros
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(cv -> {
            String propiedad = cv.getPropertyPath().toString();
            errores.put(propiedad, cv.getMessage());
        });

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("codigo", HttpStatus.BAD_REQUEST.value());
        body.put("mensaje", "Error de validacion de campos");
        body.put("errores", errores);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Excepción personalizada: CURP duplicada
    @ExceptionHandler(CurpDuplicadaException.class)
    public ResponseEntity<Map<String, Object>> handleCurpDuplicadaException(CurpDuplicadaException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Excepción personalizada: RFC duplicado
    @ExceptionHandler(RfcDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleRfcDuplicadoException(RfcDuplicadoException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Excepción personalizada: Cliente ya registrado
    @ExceptionHandler(ClienteYaRegistradoException.class)
    public ResponseEntity<Map<String, Object>> handleClienteYaRegistradoException(ClienteYaRegistradoException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Excepción personalizada: Cliente no encontrado
    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleClienteNoEncontradoException(ClienteNoEncontradoException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // Excepción personalizada: Cuenta no encontrada
    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleCuentaNoEncontradaException(CuentaNoEncontradaException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // Excepción personalizada: Error de Regla de Negocio
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleReglaNegocioException(ReglaNegocioException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, Object>> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioInactivo(UsuarioInactivoException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleCorreoDuplicado(CorreoDuplicadoException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PasswordInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handlePasswordInvalida(PasswordInvalidaException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(CatalogoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleCatalogoNoEncontrado(CatalogoNoEncontradoException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CatalogoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleCatalogoDuplicado(CatalogoDuplicadoException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Excepciones ResponseStatusException
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        return buildErrorResponse(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason());
    }

    // Excepciones no controladas (fallback seguro)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("codigo", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("mensaje", "Error interno en el servidor: " + ex.getMessage());

        log.error("Excepcion no controlada: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("codigo", status.value());
        body.put("mensaje", mensaje);
        log.warn("Respuesta de error [{}]: {}", status.value(), mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
