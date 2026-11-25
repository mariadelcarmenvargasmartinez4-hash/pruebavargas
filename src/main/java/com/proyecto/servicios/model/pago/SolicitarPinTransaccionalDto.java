package com.proyecto.servicios.model.pago;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Petición para solicitar el envío de un PIN dinámico de autorización por correo electrónico")
public record SolicitarPinTransaccionalDto(
        @Schema(description = "Número de cuenta bancaria origen de 16 dígitos", example = "1234567890123456")
        @NotBlank(message = "El número de cuenta es obligatorio")
        String numCuenta,

        @Schema(description = "Monto de la transacción", example = "150.00")
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        BigDecimal monto,

        @Schema(description = "Nombre o descripción del servicio", example = "CFE Suministrador de Servicios Básicos")
        String servicio
) {}
