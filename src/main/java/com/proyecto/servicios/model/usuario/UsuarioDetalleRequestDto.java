package com.proyecto.servicios.model.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO para consultar el detalle de un usuario mediante @RequestBody sin exponer @PathVariable ni @RequestParam.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Petición para consultar detalle de usuario")
public class UsuarioDetalleRequestDto implements Serializable {

    @Positive(message = "El identificador del usuario debe ser un número positivo")
    @Schema(description = "Identificador único del usuario", example = "1")
    private Long id;

    @Schema(description = "Nombre de usuario o correo", example = "usuario@empresa.com")
    private String username;

    public UsuarioDetalleRequestRecord toRecord() {
        return UsuarioDetalleRequestRecord.de(this);
    }
}
