package com.proyecto.servicios.model.usuario;

import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Optional;

/**
 * Record inmutable para operaciones de cambio de estado (desactivación / reactivación).
 */
@Schema(description = "Record inmutable para cambio de estado de usuario")
public record UsuarioEstadoRequestRecord(
        Long id,
        String motivo
) implements Serializable {

    public UsuarioEstadoRequestRecord {
        DeclarativeAssertUtil.asegurar(id != null && id > 0, "El identificador del usuario es obligatorio y debe ser mayor a cero");
        motivo = Optional.ofNullable(motivo).map(String::trim).filter(m -> !m.isBlank()).orElse(null);
    }

    public static UsuarioEstadoRequestRecord de(UsuarioEstadoRequestDto dto) {
        return new UsuarioEstadoRequestRecord(dto.getId(), dto.getMotivo());
    }
}
