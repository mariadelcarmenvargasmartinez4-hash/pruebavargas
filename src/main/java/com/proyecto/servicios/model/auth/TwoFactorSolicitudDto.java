package com.proyecto.servicios.model.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para solicitar la generación de un PIN temporal 2FA")
public class TwoFactorSolicitudDto {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato de correo electrónico es inválido")
    @JsonAlias({"correoElectronico", "email", "usuario"})
    @Schema(description = "Correo del usuario para recibir el código 2FA", example = "juan.perez@example.com")
    private String username;
}
