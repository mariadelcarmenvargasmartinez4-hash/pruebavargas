package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Excepcion lanzada cuando no se encuentra un cliente
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ClienteNoEncontradoException extends RuntimeException {
    public ClienteNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
