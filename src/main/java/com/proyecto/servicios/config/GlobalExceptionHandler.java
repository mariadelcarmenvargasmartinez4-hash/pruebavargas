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
import com.proyecto.servicios.exception.EntidadNoProcesableException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.PasswordInvalidaException;
import com.proyecto.servicios.exception.ProveedorPagoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.exception.UsuarioYaExisteException;
import com.proyecto.servicios.model.common.GenericResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Manejador global de excepciones centralizado con @RestControllerAdvice y @ExceptionHandler.
 * Devuelve respuestas consistentes con GenericResponse, códigos de estado HTTP estándar (400, 401, 403, 404, 405, 409, 415, 422, 502),
 * mensajes descriptivos correspondientes a la validación detonada y oculta información interna del sistema.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Manejo de errores de validación de campos (@Valid en petición) - ValidadorCampos / dtoValidation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        return dtoValidation(ex.getBindingResult());
    }

    public ResponseEntity<GenericResponse<Object>> dtoValidation(MethodArgumentNotValidException ex) {
        return dtoValidation(ex.getBindingResult());
    }

    public ResponseEntity<GenericResponse<Object>> dtoValidation(org.springframework.validation.Errors errors) {
        Map<String, String> listError = errors.getFieldErrors().stream()
                .collect(java.util.stream.Collectors.toMap(
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

        log.warn("Validación detonada (400 BAD REQUEST): {} -> Detalle: {}", mensajeDetonado, listError);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), mensajeDetonado, listError));
    }

    // 2. Manejo de errores de malformación JSON o tipos incompatibles en RequestBody
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse<Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String mensaje = "El cuerpo de la petición contiene datos mal formados o con formato inválido";
        Throwable cause = ex.getCause();

        if (cause instanceof JsonParseException) {
            mensaje = "La sintaxis del JSON es inválida o está incompleta (payload corrupto o mal formado)";
        } else if (cause instanceof UnrecognizedPropertyException upe) {
            String campo = upe.getPropertyName();
            mensaje = "El campo '" + campo + "' no es una propiedad permitida en la petición (propiedad no reconocida)";
        } else if (cause instanceof InvalidFormatException ife) {
            String campo = ife.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .reduce((first, second) -> second)
                    .orElse("solicitado");

            Class<?> targetType = ife.getTargetType();
            if (BigDecimal.class.isAssignableFrom(targetType) || Number.class.isAssignableFrom(targetType)) {
                mensaje = "El campo '" + campo + "' debe ser un valor numérico decimal válido";
            } else if (LocalDate.class.isAssignableFrom(targetType)) {
                mensaje = "El campo '" + campo + "' debe ser una fecha válida con formato AAAA-MM-DD";
            } else {
                mensaje = "El campo '" + campo + "' contiene un tipo de dato inválido";
            }
        } else if (cause instanceof MismatchedInputException mie) {
            String campo = mie.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .reduce((first, second) -> second)
                    .orElse("solicitado");

            Class<?> targetType = mie.getTargetType();
            if (BigDecimal.class.isAssignableFrom(targetType) || Number.class.isAssignableFrom(targetType)) {
                mensaje = "El campo '" + campo + "' debe ser un valor numérico decimal válido";
            } else {
                mensaje = "El campo '" + campo + "' no debe ser un arreglo u objeto";
            }
        }

        log.warn("Error de lectura JSON capturado (400 BAD REQUEST): {}", mensaje);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), mensaje));
    }

    // 3. Manejo de errores de tipos incompatibles en variables de ruta o query params (ej. letras en ID numérico)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponse<Object>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String tipoRequerido = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "específico";
        String mensaje = String.format("El parámetro '%s' con valor '%s' es inválido. Se requiere un valor de tipo %s",
                ex.getName(), ex.getValue(), tipoRequerido);
        log.warn("Tipo de parámetro incompatible (400 BAD REQUEST): {}", mensaje);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), mensaje));
    }

    // 4. Parámetros de consulta obligatorios faltantes
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<GenericResponse<Object>> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        String mensaje = String.format("El parámetro obligatorio '%s' de tipo %s no fue proporcionado en la consulta",
                ex.getParameterName(), ex.getParameterType());
        log.warn("Parámetro requerido faltante (400 BAD REQUEST): {}", mensaje);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), mensaje));
    }

    // 5. Manejo de valores numéricos desbordados o violaciones de integridad en BD
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse<Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String mensaje = "El valor proporcionado excede el límite máximo permitido o viola una restricción de integridad en base de datos";
        log.warn("Violación de integridad de datos (400 BAD REQUEST): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), mensaje));
    }

    // 6. Manejo de violaciones de restricciones en parámetros (@Validated)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errores = ex.getConstraintViolations().stream()
                .collect(java.util.stream.Collectors.toMap(
                        cv -> cv.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existente, reemplazo) -> existente,
                        LinkedHashMap::new
                ));

        String mensajeDetonado = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Error de validación de parámetros");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), mensajeDetonado, errores));
    }

    // 7. Manejo de control de acceso y autorización (Anti-IDOR)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GenericResponse<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        String mensaje = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Acceso denegado: No tiene permisos para consultar o manipular los datos de otro usuario";
        log.warn("Acceso denegado (403 FORBIDDEN): {}", mensaje);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(GenericResponse.error(HttpStatus.FORBIDDEN.value(), mensaje));
    }

    // 8. Reglas de negocio y validaciones semánticas (400 BAD REQUEST)
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<GenericResponse<Object>> handleReglaNegocioException(ReglaNegocioException ex) {
        log.warn("Regla de negocio no satisfecha (400 BAD REQUEST): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(PasswordInvalidaException.class)
    public ResponseEntity<GenericResponse<Object>> handlePasswordInvalida(PasswordInvalidaException ex) {
        log.warn("Contraseña inválida (400 BAD REQUEST): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    // 9. Autenticación y credenciales (401 UNAUTHORIZED)
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<GenericResponse<Object>> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        log.warn("Credenciales inválidas (401 UNAUTHORIZED): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(GenericResponse.error(HttpStatus.UNAUTHORIZED.value(), ex.getMessage()));
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<GenericResponse<Object>> handleUsuarioInactivo(UsuarioInactivoException ex) {
        log.warn("Usuario inactivo (403 FORBIDDEN): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(GenericResponse.error(HttpStatus.FORBIDDEN.value(), ex.getMessage()));
    }

    // 10. Recursos no encontrados (404 NOT FOUND)
    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<GenericResponse<Object>> handleClienteNoEncontradoException(ClienteNoEncontradoException ex) {
        log.warn("Cliente no encontrado (404 NOT FOUND): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(GenericResponse.error(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<GenericResponse<Object>> handleCuentaNoEncontradaException(CuentaNoEncontradaException ex) {
        log.warn("Cuenta no encontrada (404 NOT FOUND): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(GenericResponse.error(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<GenericResponse<Object>> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        log.warn("Usuario no encontrado (404 NOT FOUND): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(GenericResponse.error(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(CatalogoNoEncontradoException.class)
    public ResponseEntity<GenericResponse<Object>> handleCatalogoNoEncontrado(CatalogoNoEncontradoException ex) {
        log.warn("Catálogo no encontrado (404 NOT FOUND): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(GenericResponse.error(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GenericResponse<Object>> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("Ruta estática o recurso web no encontrado (404 NOT FOUND): {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(GenericResponse.error(HttpStatus.NOT_FOUND.value(), "El recurso solicitado no fue encontrado en el servidor"));
    }

    // 11. Conflictos de duplicidad (409 CONFLICT)
    @ExceptionHandler(CurpDuplicadaException.class)
    public ResponseEntity<GenericResponse<Object>> handleCurpDuplicadaException(CurpDuplicadaException ex) {
        log.warn("CURP duplicada (409 CONFLICT): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(GenericResponse.error(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    @ExceptionHandler(RfcDuplicadoException.class)
    public ResponseEntity<GenericResponse<Object>> handleRfcDuplicadoException(RfcDuplicadoException ex) {
        log.warn("RFC duplicado (409 CONFLICT): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(GenericResponse.error(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<GenericResponse<Object>> handleCorreoDuplicado(CorreoDuplicadoException ex) {
        log.warn("Correo duplicado (409 CONFLICT): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(GenericResponse.error(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    @ExceptionHandler(UsuarioYaExisteException.class)
    public ResponseEntity<GenericResponse<Object>> handleUsuarioYaExiste(UsuarioYaExisteException ex) {
        log.warn("Usuario ya existe (409 CONFLICT): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(GenericResponse.error(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    @ExceptionHandler(ClienteYaRegistradoException.class)
    public ResponseEntity<GenericResponse<Object>> handleClienteYaRegistradoException(ClienteYaRegistradoException ex) {
        log.warn("Cliente ya registrado (409 CONFLICT): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(GenericResponse.error(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    @ExceptionHandler(CatalogoDuplicadoException.class)
    public ResponseEntity<GenericResponse<Object>> handleCatalogoDuplicado(CatalogoDuplicadoException ex) {
        log.warn("Elemento de catálogo duplicado (409 CONFLICT): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(GenericResponse.error(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    // 12. Métodos HTTP no soportados (405 METHOD NOT ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GenericResponse<Object>> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String mensaje = String.format("El método HTTP '%s' no está permitido para este endpoint. Métodos permitidos: %s",
                ex.getMethod(), ex.getSupportedHttpMethods());
        log.warn("Método HTTP no soportado (405 METHOD NOT ALLOWED): {}", mensaje);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(GenericResponse.error(HttpStatus.METHOD_NOT_ALLOWED.value(), mensaje));
    }

    // 13. Formatos / Media Types no soportados (415 UNSUPPORTED MEDIA TYPE)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<GenericResponse<Object>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        String mensaje = "El Content-Type enviado no está soportado. Se requiere 'application/json'";
        log.warn("Content-Type no soportado (415 UNSUPPORTED MEDIA TYPE): {}", ex.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(GenericResponse.error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), mensaje));
    }

    // 14. Respuestas con estado específico de Spring (ResponseStatusException)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<GenericResponse<Object>> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String mensaje = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        log.warn("ResponseStatusException capturada ({}): {}", status, mensaje);
        return ResponseEntity.status(status).body(GenericResponse.error(status.value(), mensaje));
    }

    // 15. Entidad no procesable (422 UNPROCESSABLE ENTITY)
    @ExceptionHandler(EntidadNoProcesableException.class)
    public ResponseEntity<GenericResponse<Object>> handleEntidadNoProcesable(EntidadNoProcesableException ex) {
        log.warn("Entidad o catálogo no procesable (422 UNPROCESSABLE ENTITY): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(GenericResponse.error(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<GenericResponse<Object>> handleDataAccessException(DataAccessException ex) {
        log.error("Error de acceso o integridad en base de datos (422 UNPROCESSABLE ENTITY): {}", ex.getMessage());
        String mensaje = "No fue posible procesar la entidad debido a restricciones de integridad o catálogos no encontrados en la base de datos.";
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(GenericResponse.error(HttpStatus.UNPROCESSABLE_ENTITY.value(), mensaje));
    }

    // 16. Argumentos o estados inválidos (400 BAD REQUEST)
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<GenericResponse<Object>> handleIllegalArgumentException(RuntimeException ex) {
        log.warn("Error en argumento o estado de la petición (400 BAD REQUEST): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    // 17. Proveedor de pagos externo (502 BAD GATEWAY)
    @ExceptionHandler(ProveedorPagoException.class)
    public ResponseEntity<GenericResponse<Object>> handleProveedorPagoException(ProveedorPagoException ex) {
        log.error("Error en comunicación con proveedor externo de pagos (502 BAD GATEWAY): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(GenericResponse.error(HttpStatus.BAD_GATEWAY.value(), ex.getMessage()));
    }

    // 18. Excepciones no controladas: Oculta detalles internos y evita fugas de información o stack traces crudos
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<Object>> handleGeneralException(Exception ex) {
        log.error("Excepción no controlada en el servidor (500 INTERNAL SERVER ERROR): ", ex);
        String mensajeSeguro = "Ha ocurrido un error interno en el servidor. Por favor contacte al administrador.";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GenericResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), mensajeSeguro));
    }
}
