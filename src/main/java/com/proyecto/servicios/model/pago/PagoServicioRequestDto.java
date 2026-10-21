package com.proyecto.servicios.model.pago;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO de solicitud para la recepción de pago de servicios o recargas telefónicas mediante @RequestBody.
 * Aplica Bean Validation estricto en tipos, formatos y restricciones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos de entrada para el procesamiento de pago de servicios o recargas telefónicas")
public class PagoServicioRequestDto implements Serializable {

    @NotBlank(message = "El identificador de la cuenta (numCuenta) es obligatorio")
    @Pattern(regexp = "^\\d{10,16}$", message = "El número de cuenta debe contener entre 10 y 16 dígitos numéricos")
    @Schema(description = "Número de cuenta bancaria del cliente", example = "1234567890123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String numCuenta;

    @NotNull(message = "El identificador del servicio (idServicio) es obligatorio")
    @Positive(message = "El idServicio debe ser un número positivo")
    @Schema(description = "Identificador del servicio a pagar", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long idServicio;

    @NotNull(message = "El identificador del producto (idProducto) es obligatorio")
    @Positive(message = "El idProducto debe ser un número positivo")
    @Schema(description = "Identificador del producto dentro del servicio", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long idProducto;

    @Positive(message = "El idConcepto debe ser un número positivo")
    @Schema(description = "Identificador opcional del concepto a pagar", example = "501")
    private Long idConcepto;

    @Schema(description = "Identificador opcional del distribuidor del servicio", example = "83")
    private Integer idDistribuidor;

    @JsonAlias({"precio", "monto"})
    @DecimalMin(value = "0.01", message = "El monto o precio debe ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "El monto no debe exceder 2 decimales")
    @Schema(description = "Monto o precio del servicio o recarga", example = "150.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal monto;

    @Schema(description = "Precio del servicio (alias de monto)", example = "150.00")
    private BigDecimal precio;

    @NotBlank(message = "El Identificador Único (UPE) es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El UPE debe ser un identificador numérico exactamente de 10 dígitos")
    @Schema(description = "Identificador Único (UPE) de 10 dígitos numéricos", example = "1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
    private String upe;

    @NotNull(message = "La bandera de tipo de pago es obligatoria (true=recarga telefónica, false=servicio con referencia)")
    @JsonAlias({"esRecargaTelefonica", "esTelefono", "bandera"})
    @Schema(description = "Bandera de tipo de pago: true para recargas/pago telefónico, false para servicios con referencia", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean bandera;

    @Schema(description = "Bandera alternativa para tipo de pago", example = "true")
    private Boolean esRecargaTelefonica;

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe contener exactamente 10 dígitos numéricos")
    @Schema(description = "Número telefónico (obligatorio si bandera = true)", example = "5512345678")
    private String telefono;

    @Schema(description = "Referencia de pago del servicio (obligatorio si bandera = false)", example = "REF-AGUA-987654")
    private String referencia;

    @NotBlank(message = "El PIN o pass seguro de verificación es obligatorio")
    @Pattern(regexp = "^\\d{4,8}$", message = "El PIN o pass seguro debe ser numérico y contener entre 4 y 8 dígitos")
    @JsonAlias({"passSeguro", "codigoVerificacion", "pin"})
    @Schema(description = "PIN o Pass seguro (código de verificación de un solo uso asignado al cliente)", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pin;

    @Schema(description = "Pass seguro alternativo (alias de PIN)", example = "1234")
    private String passSeguro;

    public PagoServicioRequestRecord toRecord() {
        return PagoServicioRequestRecord.de(this);
    }
}
