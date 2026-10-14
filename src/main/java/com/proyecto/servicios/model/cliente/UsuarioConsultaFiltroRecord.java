package com.proyecto.servicios.model.cliente;

import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Optional;

/**
 * Record inmutable para consulta paginada y filtrado dinámico de usuarios de acceso.
 * Soporta de manera nativa la consulta por número de pack/página y tamaño manipulable.
 */
@Schema(description = "Record inmutable para filtros y parámetros de paginación de usuarios")
public record UsuarioConsultaFiltroRecord(
        @Schema(description = "Filtro por correo o username")
        String correo,

        @Schema(description = "Filtro por estatus activo")
        Boolean activo,

        @Schema(description = "Número de página (inicia en 0)", example = "0")
        int page,

        @Schema(description = "Cantidad de elementos por página", example = "10")
        int size,

        @Schema(description = "Campo de ordenamiento", example = "id")
        String sortBy,

        @Schema(description = "Dirección de orden (ASC / DESC)", example = "DESC")
        String sortDir,

        @Schema(description = "Número de pack de registros solicitado (alias nativo de página)", example = "0")
        Integer pack
) implements Serializable {

    public UsuarioConsultaFiltroRecord {
        int paginaEfectiva = Optional.ofNullable(pack).orElse(page);
        DeclarativeAssertUtil.asegurar(paginaEfectiva >= 0, "El número de página (pack/page) no puede ser menor a cero");
        DeclarativeAssertUtil.asegurar(size > 0 && size <= 100, "El tamaño de página debe estar entre 1 y 100 elementos");

        page = paginaEfectiva;
        correo = Optional.ofNullable(correo).map(String::trim).filter(c -> !c.isBlank()).orElse(null);
        sortBy = Optional.ofNullable(sortBy).map(String::trim).filter(s -> !s.isBlank()).orElse("id");
        sortDir = Optional.ofNullable(sortDir).map(String::trim).map(String::toUpperCase).filter(s -> !s.isBlank()).orElse("DESC");
    }
}
