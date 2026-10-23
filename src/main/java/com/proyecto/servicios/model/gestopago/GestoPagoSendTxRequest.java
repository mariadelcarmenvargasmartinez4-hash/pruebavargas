package com.proyecto.servicios.model.gestopago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Modelo de solicitud para el envío de transacciones (SendTx) hacia el proveedor GestoPago.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GestoPagoSendTxRequest implements Serializable {

    private String upe;
    private Long idServicio;
    private Long idProducto;
    private Long idConcepto;
    private Integer idDistribuidor;
    private BigDecimal monto;
    private Boolean esRecargaTelefonica;
    private String telefono;
    private String referencia;
    private String datosEncriptados;
    private String firma;
}
