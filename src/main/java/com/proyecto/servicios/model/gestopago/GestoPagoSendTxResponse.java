package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Modelo de respuesta del servicio de transacciones (SendTx) de GestoPago.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoSendTxResponse implements Serializable {

    private String codigo;
    private String mensaje;
    private String folioTransaccion;
    private String autorizacion;
    private String fechaHora;

    /**
     * Evalúa si la respuesta del proveedor corresponde al código 01 (Transacción Exitosa).
     *
     * @return true únicamente si el código es "01"
     */
    public boolean esExitosa() {
        return "01".equals(codigo);
    }
}
