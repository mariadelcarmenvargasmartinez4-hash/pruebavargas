package com.proyecto.servicios.entity.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// Documento MongoDB para persistir el catalogo de productos
@Document(collection = "cat_productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private Integer idProducto;

    private Integer idServicio;
    private String producto;
    private String servicio;
    private Integer idCatTipoServicio;
    private Integer tipoFront;
    private Boolean hasDigitoVerificador;
    private String tipoReferencia;
    private String precio;
    private Boolean showAyuda;
    private String legend;
    private LocalDateTime fechaSincronizacion;
}
