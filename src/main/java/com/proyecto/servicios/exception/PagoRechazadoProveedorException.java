package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.pago.PagoServicioResponseDto;
import lombok.Getter;

/**
 * Excepción de negocio lanzada cuando GestoPago / proveedor rechaza una operación,
 * adjuntando los datos reales del comprobante de rechazo para su presentación en cliente.
 */
@Getter
public class PagoRechazadoProveedorException extends ReglaNegocioException {

    private final PagoServicioResponseDto comprobante;

    public PagoRechazadoProveedorException(String message, PagoServicioResponseDto comprobante) {
        super(message);
        this.comprobante = comprobante;
    }
}
