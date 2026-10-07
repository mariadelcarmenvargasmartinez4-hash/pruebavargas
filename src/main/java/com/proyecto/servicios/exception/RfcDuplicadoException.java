package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Excepcion lanzada cuando un RFC ya existe en el sistema
@ResponseStatus(HttpStatus.CONFLICT)
public class RfcDuplicadoException extends RuntimeException {
    public RfcDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
