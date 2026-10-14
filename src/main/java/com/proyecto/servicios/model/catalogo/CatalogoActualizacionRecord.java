package com.proyecto.servicios.model.catalogo;

import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Optional;

/**
 * Record inmutable para la actualización de elementos de catálogo con validación Fail-Fast.
 */
@Schema(description = "Record inmutable para actualización de elementos de catálogo")
public record CatalogoActualizacionRecord(
        String descripcion,
        Boolean activo
) implements Serializable {

    public CatalogoActualizacionRecord {
        Optional.ofNullable(descripcion).filter(d -> !d.isBlank()).ifPresent(d ->
                DeclarativeAssertUtil.asegurar(d.trim().length() >= 2 && d.trim().length() <= 100,
                        "La descripción debe tener entre 2 y 100 caracteres"));

        descripcion = Optional.ofNullable(descripcion).map(String::trim).map(String::toUpperCase).orElse(null);
    }

    public static CatalogoActualizacionRecord de(CatalogoActualizacionDto dto) {
        return new CatalogoActualizacionRecord(dto.getDescripcion(), dto.getActivo());
    }
}
