package com.proyecto.servicios.model.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para validar el PIN temporal de Autenticación Multifactor (2FA)")
public class TwoFactorVerificacionDto {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato de correo electrónico es inválido")
    @JsonAlias({"correoElectronico", "email", "usuario"})
    @Schema(description = "Correo del usuario", example = "juan.perez@example.com")
    private String username;

    @NotBlank(message = "El PIN temporal es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El PIN temporal debe contener exactamente 6 dígitos numéricos")
    @JsonAlias({"codigo", "otp", "pinTemporal"})
    @Schema(description = "PIN temporal de 6 dígitos", example = "481516")
    private String pin;

    @Schema(description = "Dispositivo de origen (WEB, MOVIL)", example = "WEB")
    private String dispositivo;
}
