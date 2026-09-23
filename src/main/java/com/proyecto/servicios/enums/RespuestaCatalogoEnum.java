package com.proyecto.servicios.enums;

import lombok.Getter;

// Enumerador para estandarizar las respuestas de catalogo de productos
@Getter
public enum RespuestaCatalogoEnum {

    EXITO(0, "Operacion realizada con exito"),
    SINCRONIZACION_EXITOSA(1, "Catalogo de productos sincronizado en MongoDB exitosamente"),
    SIN_DATOS(2, "No se encontraron productos en la base de datos"),
    ERROR_AUTORIZACION(401, "Error de autorizacion con el servicio externo"),
    ERROR_TOKEN_EXPIRADO(403, "Token invalido o expirado en el servicio externo"),
    ERROR_TIMEOUT(408, "Tiempo de espera agotado al conectar con el servicio externo"),
    ERROR_PROVEEDOR(502, "Respuesta no exitosa del proveedor externo"),
    ERROR_FORMATO_XML(500, "Error al interpretar la respuesta XML del servicio externo"),
    ERROR_INTERNO(500, "Error interno al procesar la solicitud de productos");

    private final Integer codigo;
    private final String mensaje;

    RespuestaCatalogoEnum(Integer codigo, String mensaje) {
        this.codigo = codigo;
        this.mensaje = mensaje;
    }
}
