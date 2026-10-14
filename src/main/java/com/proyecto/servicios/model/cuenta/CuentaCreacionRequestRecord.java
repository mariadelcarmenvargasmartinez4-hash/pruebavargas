package com.proyecto.servicios.model.cuenta;

import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Record inmutable para la creación de cuenta bancaria con validación Fail-Fast.
 */
@Schema(description = "Record inmutable para creación de cuenta con validación temprana Fail-Fast")
public record CuentaCreacionRequestRecord(
        @Schema(description = "Identificador del cliente", example = "1")
        Long clienteId,

        @Schema(description = "Tipo de cuenta", example = "DEBITO")
        String tipoCuenta,

        @Schema(description = "Saldo inicial", example = "1000.00")
        BigDecimal saldoInicial,

        @Schema(description = "Moneda", example = "MXN")
        String moneda
) implements Serializable {

    public CuentaCreacionRequestRecord {
        DeclarativeAssertUtil.asegurar(clienteId != null && clienteId > 0, "El clienteId es obligatorio y debe ser mayor a cero");
        Optional.ofNullable(saldoInicial).ifPresent(s ->
                DeclarativeAssertUtil.asegurar(s.compareTo(BigDecimal.ZERO) >= 0, "El saldo inicial no puede ser negativo"));

        tipoCuenta = Optional.ofNullable(tipoCuenta).map(String::trim).map(String::toUpperCase).orElse("DEBITO");
        moneda = Optional.ofNullable(moneda).map(String::trim).map(String::toUpperCase).orElse("MXN");
    }

    public static CuentaCreacionRequestRecord de(CuentaCreacionRequestDto dto) {
        return new CuentaCreacionRequestRecord(
                dto.getClienteId(),
                dto.getTipoCuenta(),
                dto.getSaldoInicial(),
                dto.getMoneda()
        );
    }
}
