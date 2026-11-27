package com.proyecto.servicios.service;

import com.proyecto.servicios.model.producto.ProductoItemDto;
import com.proyecto.servicios.model.producto.ProductoResponseDto;

import java.util.Optional;

// Contrato para la logica de negocio de productos con persistencia en MongoDB
public interface ProductoService {

    // Sincroniza los productos desde la API externa hacia MongoDB
    ProductoResponseDto sincronizarProductosDesdeServicio();

    // Consulta los productos almacenados en MongoDB y los retorna en formato JSON
    ProductoResponseDto obtenerListaProductos();

    // Busca un producto real por su ID en el catálogo sincronizado de GestoPago
    default Optional<ProductoItemDto> buscarProductoRealPorId(Integer idProducto) {
        return Optional.empty();
    }
}
