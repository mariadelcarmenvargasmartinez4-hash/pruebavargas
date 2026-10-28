package com.proyecto.servicios.service;

import com.proyecto.servicios.model.pago.PagoConciliacionRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioResponseDto;

/**
 * Servicio desacoplado para la recepción, validación, procesamiento y conciliación de pagos de servicios y recargas telefónicas.
 */
public interface PagoServicioService {

    /**
     * Procesa la solicitud de pago de servicio o recarga telefónica validando reglas de negocio, saldo, catálogo y autenticación.
     *
     * @param request Datos de la petición con validaciones declarativas
     * @return DTO con los detalles de confirmación de la transacción
     */
    PagoServicioResponseDto procesarPago(PagoServicioRequestDto request);

    /**
     * Consulta y concilia una transacción previa mediante su identificador único (UPE).
     * Evita reintentos ciegos y resuelve transacciones en estado EN_CONCILIACION.
     *
     * @param request DTO con el UPE a conciliar
     * @return DTO con el estado actual o conciliado del pago
     */
    PagoServicioResponseDto consultarOConciliarPago(PagoConciliacionRequestDto request);
}
