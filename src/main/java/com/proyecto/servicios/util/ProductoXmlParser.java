package com.proyecto.servicios.util;

import com.proyecto.servicios.model.producto.GetProductListXmlResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.Optional;
import java.util.function.Predicate;

// Utilidad declarativa para deserialización de respuestas XML a objetos Java
@Component
@Slf4j
public class ProductoXmlParser {

    // Deserializa el contenido XML recibido a la estructura de clases JAXB
    public GetProductListXmlResponse parse(String xmlContent) throws JAXBException {
        String content = Optional.ofNullable(xmlContent)
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .orElseThrow(() -> new IllegalArgumentException("El contenido XML no puede ser nulo o vacio"));

        JAXBContext context = JAXBContext.newInstance(GetProductListXmlResponse.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return (GetProductListXmlResponse) unmarshaller.unmarshal(new StringReader(content));
    }
}
