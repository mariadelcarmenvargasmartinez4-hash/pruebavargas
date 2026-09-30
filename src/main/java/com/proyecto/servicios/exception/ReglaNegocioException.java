package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Excepcion lanzada ante errores de reglas de negocio en clientes o cuentas
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
