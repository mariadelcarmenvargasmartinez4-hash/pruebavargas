package com.proyecto.servicios.model.cuenta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// DTO para la actualizacion parcial de una cuenta bancaria (el numero de cuenta no puede modificarse)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para actualización parcial de cuenta bancaria (tipo, saldo, estatus)")
public class CuentaActualizacionRequestDto {

    @Schema(description = "Tipo de cuenta (DEBITO, CREDITO, NOMINA, AHORRO)", example = "AHORRO")
    private String tipoCuenta;

    @DecimalMin(value = "0.00", message = "El saldo no puede ser negativo")
    @Schema(description = "Nuevo saldo disponible", example = "2500.50")
    private BigDecimal saldo;

    @Pattern(regexp = "^(ACTIVA|BLOQUEADA|INACTIVA|CANCELADA)$", message = "El estatus debe ser ACTIVA, BLOQUEADA, INACTIVA o CANCELADA")
    @Schema(description = "Estatus de la cuenta", example = "ACTIVA")
    private String estatus;
}
