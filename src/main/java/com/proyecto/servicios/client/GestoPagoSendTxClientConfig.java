package com.proyecto.servicios.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxResponse;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

/**
 * Configuración específica de decodificación para GestoPagoSendTxClient.
 * Soporta de forma transparente respuestas tanto en XML (text/xml de Struts sendTx.do) como en JSON.
 */
@Slf4j
public class GestoPagoSendTxClientConfig {

    @Bean
    public Decoder gestoPagoSendTxDecoder() {
        ObjectMapper objectMapper = new ObjectMapper();
        return (response, type) -> {
            if (response.body() == null) {
                return null;
            }
            String rawBody;
            try (InputStream is = response.body().asInputStream()) {
                rawBody = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
            if (rawBody.isEmpty()) {
                return null;
            }
            if (rawBody.startsWith("<")) {
                return parseXmlAutorizacion(rawBody, response.request());
            }
            return objectMapper.readValue(rawBody, GestoPagoSendTxResponse.class);
        };
    }

    private GestoPagoSendTxResponse parseXmlAutorizacion(String xml, feign.Request request) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();

            String codigo = firstTagText(doc, "CODIGO_MENSAJE", "CODIGO");
            String mensaje = firstTagText(doc, "MENSAJE", "TEXTO");
            String numAutorizacion = firstTagText(doc, "NUM_AUTORIZACION", "AUTORIZACION_ID");
            String folio = firstTagText(doc, "FOLIO", "ID_TX", "TRANSACCION");

            if ((folio == null || folio.isBlank())
                    && numAutorizacion != null
                    && !numAutorizacion.isBlank()
                    && !"-1".equals(numAutorizacion.trim())
                    && !"0".equals(numAutorizacion.trim())) {
                folio = "GP-" + numAutorizacion.trim();
            }

            return GestoPagoSendTxResponse.builder()
                    .codigo(codigo)
                    .mensaje(mensaje)
                    .autorizacion(numAutorizacion)
                    .folioTransaccion(folio)
                    .build();
        } catch (Exception ex) {
            log.error("Error al parsear respuesta XML de SendTx GestoPago: {}", ex.getMessage());
            throw new DecodeException(200, "No fue posible interpretar la respuesta XML de GestoPago: " + ex.getMessage(), request, ex);
        }
    }

    private String firstTagText(Document doc, String... tagNames) {
        for (String tag : tagNames) {
            NodeList nodes = doc.getElementsByTagName(tag);
            if (nodes != null && nodes.getLength() > 0) {
                for (int i = 0; i < nodes.getLength(); i++) {
                    if (nodes.item(i).getChildNodes().getLength() == 1
                            && nodes.item(i).getFirstChild().getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
                        String val = nodes.item(i).getTextContent();
                        if (val != null && !val.trim().isEmpty()) {
                            return val.trim();
                        }
                    }
                }
            }
        }
        return null;
    }
}
