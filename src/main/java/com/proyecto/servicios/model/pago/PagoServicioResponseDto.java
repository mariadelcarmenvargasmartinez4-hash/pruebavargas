package com.proyecto.servicios.model.pago;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para la confirmación de pago de servicio o recarga telefónica.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta del procesamiento de pago de servicio o recarga")
public class PagoServicioResponseDto implements Serializable {

    @Schema(description = "Folio único de la transacción", example = "TX-8f3a9e2b-1234-5678-abcd-ef0123456789")
    private String folioTransaccion;

    @Schema(description = "Número de cuenta origen", example = "1234567890123456")
    private String numCuenta;

    @Schema(description = "Identificador del servicio", example = "1")
    private Long idServicio;

    @Schema(description = "Identificador del producto", example = "101")
    private Long idProducto;

    @Schema(description = "Identificador del concepto", example = "501")
    private Long idConcepto;

    @Schema(description = "Identificador del distribuidor", example = "83")
    private Integer idDistribuidor;

    @Schema(description = "Monto pagado", example = "150.00")
    private BigDecimal monto;

    @Schema(description = "Saldo de la cuenta previo al pago", example = "1000.00")
    private BigDecimal saldoAnterior;

    @Schema(description = "Saldo disponible actual en la cuenta tras el débito", example = "850.00")
    private BigDecimal saldoActual;

    @Schema(description = "Identificador Único (UPE) de 10 dígitos", example = "1234567890")
    private String upe;

    @Schema(description = "Tipo de operación realizada", example = "RECARGA_TELEFONICA")
    private String tipoOperacion;

    @Schema(description = "Destino del pago (teléfono o referencia)", example = "5512345678")
    private String destinoPago;

    @Schema(description = "Estatus de la transacción", example = "APLICADO")
    private String estatus;

    @Schema(description = "Mensaje informativo de confirmación", example = "Pago de servicio procesado y aplicado exitosamente")
    private String mensaje;

    @Schema(description = "Fecha y hora en que se aplicó el pago")
    private LocalDateTime fechaOperacion;
}
