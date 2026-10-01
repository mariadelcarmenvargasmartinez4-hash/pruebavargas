package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Excepcion lanzada cuando un usuario esta inactivo e intenta autenticarse
@ResponseStatus(HttpStatus.FORBIDDEN)
public class UsuarioInactivoException extends RuntimeException {
    public UsuarioInactivoException(String mensaje) {
        super(mensaje);
    }
}
