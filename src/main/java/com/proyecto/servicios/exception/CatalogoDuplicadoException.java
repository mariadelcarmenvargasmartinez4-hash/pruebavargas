package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CatalogoDuplicadoException extends RuntimeException {
    public CatalogoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
