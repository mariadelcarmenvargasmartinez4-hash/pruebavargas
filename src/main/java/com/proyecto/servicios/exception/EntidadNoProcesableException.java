package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando una entidad, catálogo o parámetro no puede ser procesado debido
 * a incoherencias semánticas o llaves foráneas no existentes en la base de datos.
 * Se mapea al código HTTP 422 (Unprocessable Entity).
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class EntidadNoProcesableException extends RuntimeException {

    public EntidadNoProcesableException(String mensaje) {
        super(mensaje);
    }

    public EntidadNoProcesableException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
