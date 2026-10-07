package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CatalogoNoEncontradoException extends RuntimeException {
    public CatalogoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
