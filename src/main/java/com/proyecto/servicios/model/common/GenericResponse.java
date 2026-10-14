package com.proyecto.servicios.model.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Estructura de respuesta genérica estandarizada (Generic Response) para APIs REST.
 * Diseñada bajo buenas prácticas:
 * - Tipo genérico <T> para reutilización de payloads
 * - Métodos estáticos de fábrica (Static Factory Methods) para construcción limpia
 * - Soporte de código 200 en capa de transporte con mensaje adecuado a la validación detonada
 * - Omisión de campos nulos en serialización JSON
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Estructura genérica estandarizada de respuesta para APIs REST")
public class GenericResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Código de respuesta", example = "200")
    @Builder.Default
    private Integer codigo = 200;

    @Schema(description = "Mensaje descriptivo del resultado o error de validación detonado", example = "El nombre no puede iniciar con espacios")
    private String mensaje;

    @Schema(description = "Datos devueltos en la respuesta si aplica")
    private T datos;

    @Schema(description = "Detalle granular de errores de validación de campos")
    private Map<String, String> errores;

    @Schema(description = "Fecha y hora de generación de la respuesta", example = "2026-10-13T18:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // Métodos estáticos de fábrica (Buenas Prácticas)

    public static <T> GenericResponse<T> exito(T datos, String mensaje) {
        return GenericResponse.<T>builder()
                .codigo(200)
                .mensaje(mensaje)
                .datos(datos)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> GenericResponse<T> exito(T datos) {
        return exito(datos, "Operación realizada con éxito");
    }

    public static <T> GenericResponse<T> error(String mensaje) {
        return GenericResponse.<T>builder()
                .codigo(200)
                .mensaje(mensaje)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> GenericResponse<T> error(String mensaje, Map<String, String> errores) {
        return GenericResponse.<T>builder()
                .codigo(200)
                .mensaje(mensaje)
                .errores(errores)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> GenericResponse<T> error(Integer codigo, String mensaje) {
        return GenericResponse.<T>builder()
                .codigo(codigo)
                .mensaje(mensaje)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> GenericResponse<T> error(Integer codigo, String mensaje, Map<String, String> errores) {
        return GenericResponse.<T>builder()
                .codigo(codigo)
                .mensaje(mensaje)
                .errores(errores)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
