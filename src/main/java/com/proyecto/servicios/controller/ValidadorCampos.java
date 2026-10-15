package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.common.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Validador global de campos y excepciones de datos (Data Exceptions / ValidadorCampos)
 * implementado bajo estándares Senior de Clean Code:
 * - Programación funcional con Streams (sin ciclos 'for' ni condicionales imperativas)
 * - Mapeo inmutable y preservación del orden con LinkedHashMap
 * - Manejo robusto de Data Exceptions (DataIntegrityViolationException, HttpMessageNotReadableException)
 * - Respuestas estandarizadas con GenericResponse y códigos HTTP correctos
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ValidadorCampos {

    /**
     * En caso de que alguna validación no se cumpla para algún DTO se tira MethodArgumentNotValidException.
     * Este método la captura y regresa los mensajes de error para el cliente de forma limpia y estructurada.
     *
     * @param ex Excepción lanzada por @Valid en los controladores
     * @return ResponseEntity con GenericResponse y código HTTP 400 BAD REQUEST
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<Object>> dtoValidation(MethodArgumentNotValidException ex) {
        return dtoValidation(ex.getBindingResult());
    }

    /**
     * Sobrecarga funcional que recibe directamente la interfaz Errors / BindingResult,
     * procesando los errores de campo mediante Streams sin bucles 'for' imperativos.
     *
     * @param errors Objeto Errors de Spring Validation
     * @return ResponseEntity con GenericResponse conteniendo el código y mensaje descriptivo
     */
    public ResponseEntity<GenericResponse<Object>> dtoValidation(Errors errors) {
        Map<String, String> listError = errors.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> Optional.ofNullable(error.getDefaultMessage()).orElse("Campo inválido"),
                        (existente, reemplazo) -> existente,
                        LinkedHashMap::new
                ));

        String mensajeDetonado = errors.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Datos no válidos");

        log.warn("Validación DTO detonada (400 BAD REQUEST): {} -> Detalle: {}", mensajeDetonado, listError);

        GenericResponse<Object> genericResponse = GenericResponse.error(HttpStatus.BAD_REQUEST.value(), mensajeDetonado, listError);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(genericResponse);
    }

    /**
     * Manejo especializado de Data Exceptions (DataIntegrityViolationException).
     * Captura desbordamientos numéricos, violaciones de unicidad o restricciones de integridad en base de datos.
     *
     * @param ex Excepción de violación de integridad de datos
     * @return ResponseEntity con GenericResponse y código HTTP 400 BAD REQUEST
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse<Object>> dataException(DataIntegrityViolationException ex) {
        String mensaje = "El valor proporcionado excede el límite máximo permitido o viola una restricción de integridad de datos";
        log.warn("Data Exception capturada (400 BAD REQUEST): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), mensaje));
    }

    /**
     * Manejo de excepciones de datos en deserialización y lectura JSON (HttpMessageNotReadableException).
     *
     * @param ex Excepción de mensaje HTTP no legible
     * @return ResponseEntity con GenericResponse y código HTTP 400 BAD REQUEST
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse<Object>> dataParseException(HttpMessageNotReadableException ex) {
        String mensaje = "El cuerpo de la petición contiene datos mal formados, tipos incompatibles o propiedades no permitidas";
        log.warn("Data Parse Exception capturada (400 BAD REQUEST): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), mensaje));
    }
}
