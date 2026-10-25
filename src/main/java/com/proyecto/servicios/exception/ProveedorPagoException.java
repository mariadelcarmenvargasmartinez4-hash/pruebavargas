package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando ocurre un error de transporte, timeout o falla técnica
 * en la comunicación con el proveedor externo de pagos (GestoPago).
 * Se mapea a un código HTTP 502 (Bad Gateway).
 */
public class ProveedorPagoException extends RuntimeException {

    public ProveedorPagoException(String mensaje) {
        super(mensaje);
    }

    public ProveedorPagoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
