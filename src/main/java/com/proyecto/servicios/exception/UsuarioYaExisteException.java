package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando se intenta registrar un usuario con un nombre de usuario que ya existe.
 */
public class UsuarioYaExisteException extends RuntimeException {

    public UsuarioYaExisteException(String message) {
        super(message);
    }
}
