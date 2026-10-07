package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// DTO de respuesta con la informacion del usuario de acceso (sin exponer la contrasena)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información del usuario de acceso creado para el cliente")
public class UsuarioAccesoDto {

    @Schema(description = "Identificador del usuario", example = "1")
    private Long idUsuario;

    @Schema(description = "Nombre de usuario (correo electrónico)", example = "juan.perez@example.com")
    private String username;

    @Schema(description = "Estatus activo del usuario", example = "true")
    private Boolean activo;

    @Schema(description = "Fecha de creación del usuario")
    private LocalDateTime fechaCreacion;
}
