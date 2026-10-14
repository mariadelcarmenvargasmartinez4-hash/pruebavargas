package com.proyecto.servicios.util;

import com.proyecto.servicios.exception.ReglaNegocioException;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Utilería declarativa para validación fail-fast y aserciones de reglas de negocio
 * sin recurrir a estructuras condicionales imperativas 'if'.
 */
public final class DeclarativeAssertUtil {

    private DeclarativeAssertUtil() {
        // Constructor privado para clase utilitaria inmutable
    }

    /**
     * Asegura que una condición se cumpla; en caso contrario, detona una ReglaNegocioException.
     */
    public static void asegurar(boolean condicion, String mensajeError) {
        Optional.of(condicion)
                .filter(Boolean::booleanValue)
                .orElseThrow(() -> new ReglaNegocioException(mensajeError));
    }

    /**
     * Asegura que una condición se cumpla; en caso contrario, detona la excepción proporcionada.
     */
    public static <X extends RuntimeException> void asegurar(boolean condicion, Supplier<X> exceptionSupplier) {
        Optional.of(condicion)
                .filter(Boolean::booleanValue)
                .orElseThrow(exceptionSupplier);
    }

    /**
     * Prohíbe que una condición sea verdadera; en caso de serlo, detona una ReglaNegocioException.
     */
    public static void prohibir(boolean condicion, String mensajeError) {
        asegurar(!condicion, mensajeError);
    }

    /**
     * Prohíbe que una condición sea verdadera; en caso de serlo, detona la excepción proporcionada.
     */
    public static <X extends RuntimeException> void prohibir(boolean condicion, Supplier<X> exceptionSupplier) {
        asegurar(!condicion, exceptionSupplier);
    }
}
