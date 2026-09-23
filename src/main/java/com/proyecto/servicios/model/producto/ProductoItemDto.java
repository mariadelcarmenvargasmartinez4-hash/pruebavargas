package com.proyecto.servicios.model.producto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Detalle individual de cada producto
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductoItemDto {

    @XmlAttribute(name = "idProducto")
    private Integer idProducto;

    @XmlAttribute(name = "idServicio")
    private Integer idServicio;

    @XmlAttribute(name = "producto")
    private String producto;

    @XmlAttribute(name = "servicio")
    private String servicio;

    @XmlAttribute(name = "idCatTipoServicio")
    private Integer idCatTipoServicio;

    @XmlAttribute(name = "tipoFront")
    private Integer tipoFront;

    @XmlAttribute(name = "hasDigitoVerificador")
    private Boolean hasDigitoVerificador;

    @XmlAttribute(name = "tipoReferencia")
    private String tipoReferencia;

    @XmlAttribute(name = "precio")
    private String precio;

    @XmlAttribute(name = "showAyuda")
    private Boolean showAyuda;

    @XmlElement(name = "legend")
    private String legend;
}
