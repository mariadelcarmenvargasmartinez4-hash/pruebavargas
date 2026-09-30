package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Excepcion lanzada cuando no se encuentra una cuenta bancaria
@ResponseStatus(HttpStatus.NOT_FOUND)
public class CuentaNoEncontradaException extends RuntimeException {
    public CuentaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
