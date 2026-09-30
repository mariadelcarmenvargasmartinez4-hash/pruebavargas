package com.proyecto.servicios.model.cuenta;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// DTO para la consulta rapida de saldo de una cuenta bancaria
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detalle del saldo y estatus de una cuenta bancaria")
public class SaldoCuentaDto {

    @Schema(description = "Número de cuenta bancaria", example = "4815162342")
    private String numeroCuenta;

    @Schema(description = "CLABE interbancaria", example = "012180004815162342")
    private String clabe;

    @Schema(description = "Nombre completo del titular", example = "Juan Carlos Perez Lopez")
    private String titular;

    @Schema(description = "Saldo disponible actual", example = "1000.00")
    private BigDecimal saldo;

    @Schema(description = "Moneda de la cuenta", example = "MXN")
    private String moneda;

    @Schema(description = "Estatus de la cuenta", example = "ACTIVA")
    private String estatus;

    @Schema(description = "Fecha y hora de la consulta")
    private LocalDateTime fechaConsulta;
}
