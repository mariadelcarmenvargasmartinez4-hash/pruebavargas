package com.proyecto.servicios.model.catalogo;

import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Record inmutable para la creación de elementos de catálogo con validación Fail-Fast.
 */
@Schema(description = "Record inmutable para creación de elementos de catálogo")
public record CatalogoCreacionRecord(
        String codigo,
        String descripcion
) implements Serializable {

    public CatalogoCreacionRecord {
        DeclarativeAssertUtil.asegurar(codigo != null && !codigo.isBlank(), "El código es obligatorio y no debe estar vacío");
        DeclarativeAssertUtil.asegurar(codigo.trim().matches("^[A-Za-z0-9_]{2,20}$"),
                "El código solo puede contener letras, números y guiones bajos (2 a 20 caracteres)");
        DeclarativeAssertUtil.asegurar(descripcion != null && !descripcion.isBlank(), "La descripción es obligatoria y no debe estar vacía");

        codigo = codigo.trim().toUpperCase();
        descripcion = descripcion.trim().toUpperCase();
    }

    public static CatalogoCreacionRecord de(CatalogoCreacionDto dto) {
        return new CatalogoCreacionRecord(dto.getCodigo(), dto.getDescripcion());
    }
}
