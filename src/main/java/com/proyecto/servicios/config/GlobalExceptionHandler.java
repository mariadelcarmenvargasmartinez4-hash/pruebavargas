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
import com.proyecto.servicios.model.common.GenericResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

/**
 * Manejador global de excepciones estandarizado con GenericResponse y buenas prácticas.
 * Retorna siempre código HTTP 200 con el mensaje detonado correspondiente a la validación.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Manejo de errores de validación de campos (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }

        // Obtener el mensaje específico detonado por la validación
        String mensajeDetonado = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Error de validacion de campos");

        log.warn("Validación detonada: {} -> Detalle: {}", mensajeDetonado, errores);
        return ResponseEntity.ok(GenericResponse.error(200, mensajeDetonado, errores));
    }

    // 2. Manejo de errores de malformación JSON o tipos incompatibles
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse<Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String mensaje = "El cuerpo de la peticion contiene datos mal formados o con formato invalido";
        Throwable cause = ex.getCause();

        if (cause instanceof JsonParseException) {
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

        log.warn("Error de lectura JSON capturado: {}", mensaje);
        return ResponseEntity.ok(GenericResponse.error(200, mensaje));
    }

    // 3. Manejo de valores numéricos gigantescos o violaciones de integridad
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse<Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String mensaje = "El valor proporcionado excede el límite máximo permitido o viola una restricción de integridad";
        log.warn("Violacion de integridad de datos: {}", ex.getMessage());
        return ResponseEntity.ok(GenericResponse.error(200, mensaje));
    }

    // 4. Manejo de violaciones de restricciones en parámetros
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(cv -> {
            String propiedad = cv.getPropertyPath().toString();
            errores.put(propiedad, cv.getMessage());
        });

        String mensajeDetonado = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Error de validacion de campos");

        return ResponseEntity.ok(GenericResponse.error(200, mensajeDetonado, errores));
    }

    // Excepciones de negocio personalizadas
    @ExceptionHandler(CurpDuplicadaException.class)
    public ResponseEntity<GenericResponse<Object>> handleCurpDuplicadaException(CurpDuplicadaException ex) {
        return buildErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(RfcDuplicadoException.class)
    public ResponseEntity<GenericResponse<Object>> handleRfcDuplicadoException(RfcDuplicadoException ex) {
        return buildErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(ClienteYaRegistradoException.class)
    public ResponseEntity<GenericResponse<Object>> handleClienteYaRegistradoException(ClienteYaRegistradoException ex) {
        return buildErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<GenericResponse<Object>> handleClienteNoEncontradoException(ClienteNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<GenericResponse<Object>> handleCuentaNoEncontradaException(CuentaNoEncontradaException ex) {
        return buildErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<GenericResponse<Object>> handleReglaNegocioException(ReglaNegocioException ex) {
        return buildErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<GenericResponse<Object>> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        return buildErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<GenericResponse<Object>> handleUsuarioInactivo(UsuarioInactivoException ex) {
        return buildErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<GenericResponse<Object>> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<GenericResponse<Object>> handleCorreoDuplicado(CorreoDuplicadoException ex) {
        return buildErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(PasswordInvalidaException.class)
    public ResponseEntity<GenericResponse<Object>> handlePasswordInvalida(PasswordInvalidaException ex) {
        return buildErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(CatalogoNoEncontradoException.class)
    public ResponseEntity<GenericResponse<Object>> handleCatalogoNoEncontrado(CatalogoNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(CatalogoDuplicadoException.class)
    public ResponseEntity<GenericResponse<Object>> handleCatalogoDuplicado(CatalogoDuplicadoException ex) {
        return buildErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<GenericResponse<Object>> handleResponseStatusException(ResponseStatusException ex) {
        String mensaje = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return buildErrorResponse(mensaje);
    }

    // Excepciones no controladas (fallback seguro)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<Object>> handleGeneralException(Exception ex) {
        String mensaje = "Error interno en el servidor: " + ex.getMessage();
        log.error("Excepcion no controlada: ", ex);
        return buildErrorResponse(mensaje);
    }

    private ResponseEntity<GenericResponse<Object>> buildErrorResponse(String mensaje) {
        log.warn("Respuesta de error [codigo: 200]: {}", mensaje);
        return ResponseEntity.ok(GenericResponse.error(200, mensaje));
    }
}
