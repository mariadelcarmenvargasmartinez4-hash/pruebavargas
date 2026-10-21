package com.proyecto.servicios.service;

import com.proyecto.servicios.model.pago.PagoServicioRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioResponseDto;

/**
 * Servicio desacoplado para la recepción, validación y procesamiento de pagos de servicios y recargas telefónicas.
 */
public interface PagoServicioService {

    /**
     * Procesa la solicitud de pago de servicio o recarga telefónica validando reglas de negocio, saldo y autenticación.
     *
     * @param request Datos de la petición con validaciones declarativas
     * @return DTO con los detalles de confirmación de la transacción
     */
    PagoServicioResponseDto procesarPago(PagoServicioRequestDto request);
}
