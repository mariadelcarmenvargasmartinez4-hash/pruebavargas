package com.proyecto.servicios.service;

import com.proyecto.servicios.model.producto.ProductoResponseDto;

// Contrato para la logica de negocio de productos con persistencia en MongoDB
public interface ProductoService {

    // Sincroniza los productos desde la API externa hacia MongoDB
    ProductoResponseDto sincronizarProductosDesdeServicio();

    // Consulta los productos almacenados en MongoDB y los retorna en formato JSON
    ProductoResponseDto obtenerListaProductos();
}
