package com.proyecto.servicios.model.usuario;

import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Record inmutable para cambio de contraseña con validación temprana Fail-Fast.
 */
@Schema(description = "Record inmutable para cambio de contraseña")
public record UsuarioCambioPasswordRequestRecord(
        Long id,
        String passwordActual,
        String nuevaPassword
) implements Serializable {

    public UsuarioCambioPasswordRequestRecord {
        DeclarativeAssertUtil.asegurar(id != null && id > 0, "El identificador del usuario es obligatorio y debe ser mayor a cero");
        DeclarativeAssertUtil.asegurar(passwordActual != null && !passwordActual.isBlank(), "La contraseña actual es obligatoria");
        DeclarativeAssertUtil.asegurar(nuevaPassword != null && nuevaPassword.length() >= 8, "La nueva contraseña debe tener al menos 8 caracteres");
        DeclarativeAssertUtil.prohibir(passwordActual.equals(nuevaPassword), "La nueva contraseña no puede ser idéntica a la anterior");
    }

    public static UsuarioCambioPasswordRequestRecord de(UsuarioCambioPasswordRequestDto dto) {
        return new UsuarioCambioPasswordRequestRecord(dto.getId(), dto.getPasswordActual(), dto.getNuevaPassword());
    }
}
