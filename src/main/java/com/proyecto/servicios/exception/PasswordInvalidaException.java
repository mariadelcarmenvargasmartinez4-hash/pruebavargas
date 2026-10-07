package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Excepcion lanzada cuando la contrasena no cumple con las reglas de seguridad
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PasswordInvalidaException extends RuntimeException {
    public PasswordInvalidaException(String mensaje) {
        super(mensaje);
    }
}
