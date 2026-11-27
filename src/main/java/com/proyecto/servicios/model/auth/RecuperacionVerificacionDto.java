package com.proyecto.servicios.model.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para verificar el código temporal de recuperación de contraseña")
public class RecuperacionVerificacionDto {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    @Size(max = 100, message = "El correo electrónico no debe exceder 100 caracteres")
    private String correo;

    @NotBlank(message = "El código de verificación es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código debe contener exactamente 6 dígitos numéricos")
    private String codigo;
}
