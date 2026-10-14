package com.proyecto.servicios.model.catalogo;

import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Optional;

/**
 * Record inmutable para la consulta paginada y filtrado dinámico de catálogos (estados civiles, sexos, nacionalidades).
 * Soporta de manera nativa la consulta por número de pack y tamaño manipulable.
 */
@Schema(description = "Record inmutable para filtros y parámetros de paginación de catálogos")
public record CatalogoConsultaFiltroRecord(
        @Schema(description = "Filtrar únicamente elementos activos")
        Boolean soloActivos,

        @Schema(description = "Búsqueda textual por código o descripción")
        String busqueda,

        @Schema(description = "Número de página (inicia en 0)", example = "0")
        int page,

        @Schema(description = "Cantidad de elementos por página", example = "10")
        int size,

        @Schema(description = "Campo de ordenación", example = "id")
        String sortBy,

        @Schema(description = "Dirección de orden (ASC / DESC)", example = "ASC")
        String sortDir,

        @Schema(description = "Número de pack de registros solicitado (alias nativo de página)", example = "0")
        Integer pack
) implements Serializable {

    public CatalogoConsultaFiltroRecord {
        int paginaEfectiva = Optional.ofNullable(pack).orElse(page);
        DeclarativeAssertUtil.asegurar(paginaEfectiva >= 0, "El número de página (pack/page) no puede ser menor a cero");
        DeclarativeAssertUtil.asegurar(size > 0 && size <= 100, "El tamaño de página debe estar entre 1 y 100 elementos por consulta");

        page = paginaEfectiva;
        busqueda = Optional.ofNullable(busqueda).map(String::trim).filter(s -> !s.isBlank()).orElse(null);
        sortBy = Optional.ofNullable(sortBy).map(String::trim).filter(s -> !s.isBlank()).orElse("id");
        sortDir = Optional.ofNullable(sortDir).map(String::trim).map(String::toUpperCase).filter(s -> !s.isBlank()).orElse("ASC");
    }

    public CatalogoConsultaFiltroRecord(Boolean soloActivos, int page, int size) {
        this(soloActivos, null, page, size, "id", "ASC", null);
    }
}
