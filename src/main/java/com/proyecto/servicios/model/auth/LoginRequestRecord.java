package com.proyecto.servicios.model.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Record inmutable para la autenticación con validación temprana Fail-Fast.
 */
@Schema(description = "Record inmutable para login con validación temprana")
public record LoginRequestRecord(
        @JsonAlias({"correoElectronico", "email", "username"})
        String correo,
        String password
) implements Serializable {

    public LoginRequestRecord {
        DeclarativeAssertUtil.asegurar(correo != null && !correo.isBlank(), "El correo electrónico es obligatorio");
        DeclarativeAssertUtil.asegurar(correo.trim().matches("^[A-Za-z0-9+_.-]+@(.+)$"), "El formato de correo electrónico es inválido");
        DeclarativeAssertUtil.asegurar(password != null && !password.isBlank(), "La contraseña es obligatoria");

        correo = correo.trim().toLowerCase();
    }

    public static LoginRequestRecord de(LoginRequestDto dto) {
        return new LoginRequestRecord(dto.getCorreo(), dto.getPassword());
    }
}
