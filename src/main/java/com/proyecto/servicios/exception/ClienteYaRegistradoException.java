package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Excepcion lanzada cuando un cliente ya se encuentra registrado
@ResponseStatus(HttpStatus.CONFLICT)
public class ClienteYaRegistradoException extends RuntimeException {
    public ClienteYaRegistradoException(String mensaje) {
        super(mensaje);
    }
}
