package com.proyecto.servicios.model.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO para la creación de un nuevo usuario en el sistema.
 * Utiliza Bean Validation para validaciones rigurosas de tipo, formato y restricciones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para el registro de un nuevo usuario")
public class UsuarioCreacionRequestDto implements Serializable {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Email(message = "El nombre de usuario debe tener un formato de correo electrónico válido")
    @Size(max = 100, message = "El nombre de usuario no puede exceder 100 caracteres")
    @Schema(description = "Nombre de usuario (correo electrónico)", example = "usuario.nuevo@empresa.com")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 50, message = "La contraseña debe tener entre 8 y 50 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,50}$",
             message = "La contraseña debe contener al menos una mayúscula, una minúscula, un número y un caracter especial")
    @Schema(description = "Contraseña de acceso segura", example = "Segura123#")
    private String password;

    @Pattern(regexp = "^(CLIENTE|ADMIN)$", message = "El rol debe ser CLIENTE o ADMIN")
    @Schema(description = "Rol del usuario (CLIENTE o ADMIN)", example = "CLIENTE", defaultValue = "CLIENTE")
    private String rol;

    @Positive(message = "El identificador del cliente debe ser un número positivo")
    @Schema(description = "Identificador opcional del cliente vinculado", example = "5")
    private Long clienteId;

    public UsuarioCreacionRequestRecord toRecord() {
        return UsuarioCreacionRequestRecord.de(this);
    }
}
