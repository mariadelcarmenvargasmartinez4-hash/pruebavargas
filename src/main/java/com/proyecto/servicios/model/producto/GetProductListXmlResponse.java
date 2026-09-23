package com.proyecto.servicios.model.producto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Mapeo raiz del XML retornado por el servicio externo
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetProductListXmlResponse {

    @XmlElement(name = "MENSAJE")
    private MensajeRespuestaDto mensaje;

    @XmlElement(name = "PRODUCTOS")
    private ProductosWrapperDto productosWrapper;
}
