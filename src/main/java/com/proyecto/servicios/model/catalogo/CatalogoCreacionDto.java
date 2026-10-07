package com.proyecto.servicios.model.catalogo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Datos para crear un nuevo elemento en un catálogo")
public class CatalogoCreacionDto {

    @NotBlank(message = "El código es obligatorio y no debe estar vacío")
    @Size(min = 2, max = 20, message = "El código debe tener entre 2 y 20 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "El código solo puede contener letras, números y guiones bajos sin espacios")
    @Schema(description = "Código único del elemento", example = "MEX")
    private String codigo;

    @NotBlank(message = "La descripción es obligatoria y no debe estar vacía")
    @Size(min = 2, max = 100, message = "La descripción debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9]+( [a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9]+)*$", message = "La descripción no debe iniciar ni terminar con espacios ni tener espacios consecutivos")
    @Schema(description = "Descripción o nombre del elemento", example = "MEXICANA")
    private String descripcion;
}
