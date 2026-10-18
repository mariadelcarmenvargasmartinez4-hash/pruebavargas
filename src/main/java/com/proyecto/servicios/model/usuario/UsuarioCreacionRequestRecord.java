package com.proyecto.servicios.model.usuario;

import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Optional;

/**
 * Record inmutable para creación de usuario con validación temprana Fail-Fast.
 */
@Schema(description = "Record inmutable para creación de usuario")
public record UsuarioCreacionRequestRecord(
        String username,
        String password,
        String rol,
        Long clienteId
) implements Serializable {

    public UsuarioCreacionRequestRecord {
        DeclarativeAssertUtil.asegurar(username != null && !username.isBlank(), "El nombre de usuario es obligatorio");
        DeclarativeAssertUtil.asegurar(password != null && password.length() >= 8, "La contraseña debe tener al menos 8 caracteres");
        username = username.trim().toLowerCase();
        rol = Optional.ofNullable(rol).map(String::trim).map(String::toUpperCase).filter(r -> !r.isBlank()).orElse("CLIENTE");
        DeclarativeAssertUtil.asegurar("CLIENTE".equals(rol) || "ADMIN".equals(rol), "El rol asignado debe ser CLIENTE o ADMIN");
        Optional.ofNullable(clienteId).ifPresent(id ->
                DeclarativeAssertUtil.asegurar(id > 0, "El identificador del cliente debe ser un número positivo"));
    }

    public static UsuarioCreacionRequestRecord de(UsuarioCreacionRequestDto dto) {
        return new UsuarioCreacionRequestRecord(dto.getUsername(), dto.getPassword(), dto.getRol(), dto.getClienteId());
    }
}
