package com.proyecto.servicios.model.catalogo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Elemento representativo de un catálogo")
public class CatalogoItemDto {

    @Schema(description = "Identificador único", example = "1")
    private Long id;

    @Schema(description = "Código identificador corto del catálogo", example = "MEX")
    private String codigo;

    @Schema(description = "Descripción o nombre del elemento", example = "MEXICANA")
    private String descripcion;

    @Schema(description = "Estatus activo del elemento", example = "true")
    private Boolean activo;

    @Schema(description = "Fecha de creación del registro")
    private LocalDateTime fechaCreacion;
}
