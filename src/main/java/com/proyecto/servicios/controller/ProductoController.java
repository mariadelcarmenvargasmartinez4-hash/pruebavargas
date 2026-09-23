package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.producto.ProductoResponseDto;
import com.proyecto.servicios.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controlador REST para catalogo de productos
@RestController
@RequestMapping("/productos")
@Tag(name = "Productos", description = "Operaciones de consulta de catálogo de productos externos")
public class ProductoController {

    private final ProductoService productoService;

    // Inyeccion de dependencias por constructor
    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // Consulta los productos persistidos en MongoDB retornando JSON
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Obtener lista de productos")
    public ResponseEntity<ProductoResponseDto> obtenerProductos() {
        ProductoResponseDto respuesta = productoService.obtenerListaProductos();
        return ResponseEntity.ok(respuesta);
    }
}
