package com.proyecto.servicios.model.cuenta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// DTO para crear una cuenta bancaria adicional asociada a un cliente existente
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para crear una cuenta bancaria asociada a un cliente")
public class CuentaCreacionRequestDto {

    @NotNull(message = "El clienteId es obligatorio")
    @Schema(description = "Identificador del cliente al que se le creará la cuenta", example = "1")
    private Long clienteId;

    @Schema(description = "Tipo de cuenta (DEBITO, CREDITO, NOMINA, AHORRO)", example = "DEBITO")
    private String tipoCuenta;

    @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
    @Schema(description = "Saldo inicial asignado a la nueva cuenta", example = "1000.00")
    private BigDecimal saldoInicial;

    @Schema(description = "Moneda de la cuenta (por defecto MXN)", example = "MXN")
    private String moneda;
}
