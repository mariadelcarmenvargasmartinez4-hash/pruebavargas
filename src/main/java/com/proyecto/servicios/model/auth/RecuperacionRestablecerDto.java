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
@Schema(description = "Petición para establecer una nueva contraseña previa verificación del código temporal")
public class RecuperacionRestablecerDto {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    @Size(max = 100, message = "El correo electrónico no debe exceder 100 caracteres")
    private String correo;

    @NotBlank(message = "El comprobante de validación del código es obligatorio")
    @Size(max = 128, message = "Token de validación inválido")
    private String resetToken;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 50, message = "La nueva contraseña debe tener entre 8 y 50 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).{8,50}$",
            message = "La contraseña debe contener entre 8 y 50 caracteres, al menos 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial sin espacios"
    )
    private String nuevaPassword;

    @NotBlank(message = "La confirmación de la contraseña es obligatoria")
    private String confirmarPassword;
}
