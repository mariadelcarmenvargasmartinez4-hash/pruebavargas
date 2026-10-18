package com.proyecto.servicios.model.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para la administración de usuarios.
 * Protege información sensible del sistema y nunca expone contraseñas o hashes internos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta estándar con información de administración de usuario")
public class UsuarioResponseDto implements Serializable {

    @Schema(description = "Identificador único del usuario", example = "1")
    private Long id;

    @Schema(description = "Nombre de usuario o correo electrónico", example = "usuario.admin@empresa.com")
    private String username;

    @Schema(description = "Rol asignado en el sistema", example = "ADMIN")
    private String rol;

    @Schema(description = "Estado activo/inactivo del usuario", example = "true")
    private Boolean activo;

    @Schema(description = "Identificador del cliente asociado (si aplica)", example = "10")
    private Long clienteId;

    @Schema(description = "Fecha y hora de creación controlada por el servidor")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Fecha y hora de última actualización controlada por el servidor")
    private LocalDateTime fechaActualizacion;
}
