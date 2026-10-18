package com.proyecto.servicios.model.usuario;

import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Optional;

/**
 * Record inmutable para consulta de detalle de usuario por identificador o username.
 */
@Schema(description = "Record inmutable para consulta de detalle de usuario")
public record UsuarioDetalleRequestRecord(
        Long id,
        String username
) implements Serializable {

    public UsuarioDetalleRequestRecord {
        DeclarativeAssertUtil.asegurar(id != null || (username != null && !username.isBlank()),
                "Debe proporcionar al menos el ID o el nombre de usuario");
        Optional.ofNullable(id).ifPresent(i ->
                DeclarativeAssertUtil.asegurar(i > 0, "El ID de usuario debe ser mayor a cero"));
        username = Optional.ofNullable(username).map(String::trim).filter(u -> !u.isBlank()).orElse(null);
    }

    public static UsuarioDetalleRequestRecord de(UsuarioDetalleRequestDto dto) {
        return new UsuarioDetalleRequestRecord(dto.getId(), dto.getUsername());
    }
}
