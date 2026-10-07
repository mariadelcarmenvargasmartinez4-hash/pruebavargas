package com.proyecto.servicios.model.catalogo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Datos para actualizar un elemento de un catálogo")
public class CatalogoActualizacionDto {

    @Size(min = 2, max = 100, message = "La descripción debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^$|^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9]+( [a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9]+)*$", message = "La descripción no debe iniciar ni terminar con espacios ni tener espacios consecutivos")
    @Schema(description = "Nueva descripción", example = "MEXICANA ACTUALIZADA")
    private String descripcion;

    @Schema(description = "Estatus activo del elemento", example = "true")
    private Boolean activo;
}
