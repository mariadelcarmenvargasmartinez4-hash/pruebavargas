package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Excepcion lanzada cuando una CURP ya existe en el sistema
@ResponseStatus(HttpStatus.CONFLICT)
public class CurpDuplicadaException extends RuntimeException {
    public CurpDuplicadaException(String mensaje) {
        super(mensaje);
    }
}
