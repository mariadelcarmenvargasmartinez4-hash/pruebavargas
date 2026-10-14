package com.proyecto.servicios.model.cliente;

import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Record inmutable para la creación de usuario de acceso con validación temprana Fail-Fast.
 */
@Schema(description = "Record inmutable para creación de usuario de acceso")
public record UsuarioAgregarRequestRecord(
        Long clienteId,
        String password
) implements Serializable {

    public UsuarioAgregarRequestRecord {
        DeclarativeAssertUtil.asegurar(clienteId != null && clienteId > 0, "El identificador del cliente es obligatorio y debe ser mayor a cero");
        DeclarativeAssertUtil.asegurar(password != null && password.length() >= 8, "La contraseña debe tener al menos 8 caracteres");
    }

    public static UsuarioAgregarRequestRecord de(UsuarioAgregarRequestDto dto) {
        return new UsuarioAgregarRequestRecord(dto.getClienteId(), dto.getPassword());
    }
}
