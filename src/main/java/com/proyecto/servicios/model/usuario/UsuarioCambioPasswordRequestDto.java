package com.proyecto.servicios.model.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO para cambio de contraseña de usuario mediante @RequestBody.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para cambio de contraseña de usuario")
public class UsuarioCambioPasswordRequestDto implements Serializable {

    @NotNull(message = "El identificador del usuario es obligatorio")
    @Positive(message = "El identificador del usuario debe ser un número positivo")
    @Schema(description = "Identificador único del usuario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "La contraseña actual es obligatoria")
    @Schema(description = "Contraseña actual del usuario", example = "Segura123#")
    private String passwordActual;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 50, message = "La nueva contraseña debe tener entre 8 y 50 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,50}$",
             message = "La nueva contraseña debe contener al menos una mayúscula, una minúscula, un número y un caracter especial")
    @Schema(description = "Nueva contraseña segura", example = "NuevaSegura456#")
    private String nuevaPassword;

    public UsuarioCambioPasswordRequestRecord toRecord() {
        return UsuarioCambioPasswordRequestRecord.de(this);
    }
}
