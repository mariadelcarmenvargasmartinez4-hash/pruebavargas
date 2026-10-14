package com.proyecto.servicios.model.cuenta;

import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Record inmutable para la actualización de cuenta con validación temprana Fail-Fast.
 */
@Schema(description = "Record inmutable para actualización de cuenta bancaria")
public record CuentaActualizacionRequestRecord(
        String tipoCuenta,
        BigDecimal saldo,
        String estatus
) implements Serializable {

    public CuentaActualizacionRequestRecord {
        Optional.ofNullable(saldo).ifPresent(s ->
                DeclarativeAssertUtil.asegurar(s.compareTo(BigDecimal.ZERO) >= 0, "El saldo no puede ser negativo"));

        Optional.ofNullable(estatus).filter(e -> !e.isBlank()).ifPresent(e ->
                DeclarativeAssertUtil.asegurar(List.of("ACTIVA", "BLOQUEADA", "INACTIVA", "CANCELADA").contains(e.trim().toUpperCase()),
                        "El estatus debe ser ACTIVA, BLOQUEADA, INACTIVA o CANCELADA"));
    }

    public static CuentaActualizacionRequestRecord de(CuentaActualizacionRequestDto dto) {
        return new CuentaActualizacionRequestRecord(
                dto.getTipoCuenta(),
                dto.getSaldo(),
                dto.getEstatus()
        );
    }
}
