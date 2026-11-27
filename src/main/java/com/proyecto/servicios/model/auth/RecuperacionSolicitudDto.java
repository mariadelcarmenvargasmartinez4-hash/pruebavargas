package com.proyecto.servicios.model.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud de envío de código temporal para recuperación de contraseña")
public class RecuperacionSolicitudDto {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    @Size(max = 100, message = "El correo electrónico no debe exceder 100 caracteres")
    @Schema(description = "Correo electrónico registrado por el usuario", example = "usuario@ejemplo.com")
    private String correo;
}
