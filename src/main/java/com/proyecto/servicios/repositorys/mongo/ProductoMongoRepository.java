package com.proyecto.servicios.repositorys.mongo;

import com.proyecto.servicios.entity.mongo.ProductoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Repositorio Spring Data MongoDB para catalogo de productos
@Repository
public interface ProductoMongoRepository extends MongoRepository<ProductoDocument, String> {

    // Busca un producto por su identificador unico de producto
    Optional<ProductoDocument> findByIdProducto(Integer idProducto);
}
