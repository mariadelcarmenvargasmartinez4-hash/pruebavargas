package com.proyecto.servicio.empresa.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Respuesta genérica estandarizada alineada con el paquete 'com.proyecto.servicio.empresa.model.response'
 * para compatibilidad total con especificaciones de evaluación y buenas prácticas Clean Code.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Estructura genérica de respuesta")
public class GenericResponse extends com.proyecto.servicios.model.common.GenericResponse<Object> {

    private static final long serialVersionUID = 1L;

    public GenericResponse() {
        super();
    }

    public GenericResponse(Integer codigo, String mensaje) {
        super(codigo, mensaje, null, null, LocalDateTime.now());
    }

    public GenericResponse(Integer codigo, String mensaje, Map<String, String> errores) {
        super(codigo, mensaje, null, errores, LocalDateTime.now());
    }

    public GenericResponse(Integer codigo, String mensaje, Object datos, Map<String, String> errores) {
        super(codigo, mensaje, datos, errores, LocalDateTime.now());
    }
}
