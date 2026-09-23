package com.proyecto.servicios.model.producto;

import com.proyecto.servicios.model.GenericResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// Respuesta estandarizada JSON para los consumidores del microservicio
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResponseDto extends GenericResponse {

    private List<ProductoItemDto> productos = new ArrayList<>();
}
