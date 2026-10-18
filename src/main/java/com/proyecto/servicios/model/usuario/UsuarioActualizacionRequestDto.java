package com.proyecto.servicios.model.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
 * DTO para la actualización de datos de un usuario mediante @RequestBody.
 * No incluye campos protegidos de auditoría ni credenciales de seguridad.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para actualización de información de usuario")
public class UsuarioActualizacionRequestDto implements Serializable {

    @NotNull(message = "El identificador del usuario es obligatorio")
    @Positive(message = "El identificador del usuario debe ser un número positivo")
    @Schema(description = "Identificador único del usuario a actualizar", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Email(message = "El nombre de usuario debe tener un formato de correo electrónico válido")
    @Size(max = 100, message = "El nombre de usuario no puede exceder 100 caracteres")
    @Schema(description = "Nuevo correo electrónico o nombre de usuario", example = "usuario.actualizado@empresa.com")
    private String username;

    @Pattern(regexp = "^(CLIENTE|ADMIN)$", message = "El rol debe ser CLIENTE o ADMIN")
    @Schema(description = "Nuevo rol para el usuario (CLIENTE o ADMIN)", example = "ADMIN")
    private String rol;

    public UsuarioActualizacionRequestRecord toRecord() {
        return UsuarioActualizacionRequestRecord.de(this);
    }
}
