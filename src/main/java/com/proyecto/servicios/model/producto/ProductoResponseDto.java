package com.proyecto.servicios.model.producto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Respuesta estandarizada JSON para los consumidores del microservicio
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta estandarizada de catálogo de productos")
public class ProductoResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Código de resultado de la operación", example = "200")
    private Integer codigo;

    @Schema(description = "Mensaje descriptivo del resultado", example = "Exito")
    private String mensaje;

    @Schema(description = "Lista de productos consultados")
    private List<ProductoItemDto> productos = new ArrayList<>();
}
