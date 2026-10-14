package com.proyecto.servicios.model.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

/**
 * Record genérico inmutable para respuestas paginadas estándar.
 * Proporciona el contenido y metadatos de paginación solicitados (número de página/pack, tamaño, total).
 *
 * @param <T> Tipo de elemento contenido en la página
 */
@Schema(description = "Estructura inmutable de respuesta paginada")
public record PaginaResponseRecord<T>(
        @Schema(description = "Lista de elementos contenidos en la página actual")
        List<T> contenido,

        @Schema(description = "Número de página actual consultada (pack/page)", example = "0")
        int pagina,

        @Schema(description = "Tamaño de página (cantidad de elementos por página)", example = "10")
        int tamanoPagina,

        @Schema(description = "Total general de elementos existentes en la consulta", example = "45")
        long totalElementos,

        @Schema(description = "Total de páginas calculadas disponibles", example = "5")
        int totalPaginas,

        @Schema(description = "Indica si es la primera página", example = "true")
        boolean primera,

        @Schema(description = "Indica si es la última página", example = "false")
        boolean ultima
) implements Serializable {

    /**
     * Construye un PaginaResponseRecord a partir de un Page de Spring Data.
     */
    public static <T> PaginaResponseRecord<T> de(Page<T> page) {
        return new PaginaResponseRecord<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
