package com.proyecto.servicios.model.usuario;

import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Optional;

/**
 * Record inmutable para actualización de usuario.
 * Protege campos controlados por el servidor e impide modificaciones ilegítimas.
 */
@Schema(description = "Record inmutable para actualización de usuario")
public record UsuarioActualizacionRequestRecord(
        Long id,
        String username,
        String rol
) implements Serializable {

    public UsuarioActualizacionRequestRecord {
        DeclarativeAssertUtil.asegurar(id != null && id > 0, "El identificador del usuario es obligatorio y debe ser mayor a cero");
        username = Optional.ofNullable(username).map(String::trim).map(String::toLowerCase).filter(u -> !u.isBlank()).orElse(null);
        rol = Optional.ofNullable(rol).map(String::trim).map(String::toUpperCase).filter(r -> !r.isBlank()).orElse(null);
        Optional.ofNullable(rol).ifPresent(r ->
                DeclarativeAssertUtil.asegurar("CLIENTE".equals(r) || "ADMIN".equals(r), "El rol asignado debe ser CLIENTE o ADMIN"));
    }

    public static UsuarioActualizacionRequestRecord de(UsuarioActualizacionRequestDto dto) {
        return new UsuarioActualizacionRequestRecord(dto.getId(), dto.getUsername(), dto.getRol());
    }
}
