package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// DTO con la informacion de la cuenta bancaria creada para el cliente
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detalle de la cuenta bancaria creada y saldo asignado")
public class CuentaBancariaDto {

    @Schema(description = "Identificador de la cuenta", example = "1")
    private Long idCuenta;

    @Schema(description = "Número de cuenta bancaria asignado (10 dígitos)", example = "4815162342")
    private String numeroCuenta;

    @Schema(description = "CLABE interbancaria asignada (18 dígitos)", example = "012180004815162342")
    private String clabe;

    @Schema(description = "Tipo de cuenta asignada", example = "DEBITO")
    private String tipoCuenta;

    @Schema(description = "Saldo actual disponible", example = "1000.00")
    private BigDecimal saldo;

    @Schema(description = "Moneda de la cuenta", example = "MXN")
    private String moneda;

    @Schema(description = "Estado de la cuenta", example = "ACTIVA")
    private String estatus;

    @Schema(description = "Fecha de apertura")
    private LocalDateTime fechaApertura;
}
