package com.proyecto.servicios.model.producto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Mensaje de respuesta del servicio XML
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class MensajeRespuestaDto {

    @XmlElement(name = "CODIGO")
    private String codigo;

    @XmlElement(name = "TEXTO")
    private String texto;
}
