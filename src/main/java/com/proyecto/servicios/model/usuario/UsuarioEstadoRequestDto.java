package com.proyecto.servicios.model.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO para operaciones de baja lógica y reactivación de usuario mediante @RequestBody.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para baja lógica o reactivación de usuario")
public class UsuarioEstadoRequestDto implements Serializable {

    @NotNull(message = "El identificador del usuario es obligatorio")
    @Positive(message = "El identificador del usuario debe ser un número positivo")
    @Schema(description = "Identificador único del usuario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Size(max = 200, message = "El motivo no puede exceder 200 caracteres")
    @Schema(description = "Motivo opcional del cambio de estatus", example = "Baja solicitada por el titular")
    private String motivo;

    public UsuarioEstadoRequestRecord toRecord() {
        return UsuarioEstadoRequestRecord.de(this);
    }
}
