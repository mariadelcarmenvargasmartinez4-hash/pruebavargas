package com.proyecto.servicios.model.cuenta;

import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Optional;

/**
 * Record inmutable para la consulta paginada y filtrado dinámico de cuentas bancarias.
 * Aplica validaciones tempranas declarativas (Fail-Fast) con soporte nativo para 'pack'.
 */
@Schema(description = "Record inmutable para filtros y parámetros de paginación de cuentas bancarias")
public record CuentaConsultaFiltroRecord(
        @Schema(description = "Número de cuenta bancaria (10 dígitos)")
        String numeroCuenta,

        @Schema(description = "Identificador del cliente asociado")
        Long clienteId,

        @Schema(description = "Estatus de la cuenta (ACTIVA, INACTIVA, etc.)")
        String estatus,

        @Schema(description = "Filtrar únicamente cuentas activas")
        Boolean soloActivas,

        @Schema(description = "Número de página/pack (inicia en 0)", example = "0")
        int page,

        @Schema(description = "Tamaño de página deseado", example = "10")
        int size,

        @Schema(description = "Campo de ordenación", example = "id")
        String sortBy,

        @Schema(description = "Dirección de orden (ASC / DESC)", example = "DESC")
        String sortDir,

        @Schema(description = "Número de pack de registros solicitado (alias nativo de página)", example = "0")
        Integer pack
) implements Serializable {

    public CuentaConsultaFiltroRecord {
        int paginaEfectiva = Optional.ofNullable(pack).orElse(page);
        DeclarativeAssertUtil.asegurar(paginaEfectiva >= 0, "El número de página (pack/page) no puede ser menor a cero");
        DeclarativeAssertUtil.asegurar(size > 0 && size <= 100, "El tamaño de página debe estar entre 1 y 100 elementos por consulta");

        Optional.ofNullable(numeroCuenta).filter(nc -> !nc.isBlank()).ifPresent(nc ->
                DeclarativeAssertUtil.asegurar(nc.trim().matches("^\\d{1,10}$"),
                        "El número de cuenta bancaria debe contener solo dígitos numéricos (máximo 10)"));

        page = paginaEfectiva;
        numeroCuenta = Optional.ofNullable(numeroCuenta).map(String::trim).filter(s -> !s.isBlank()).orElse(null);
        estatus = Optional.ofNullable(estatus).map(String::trim).map(String::toUpperCase).filter(s -> !s.isBlank()).orElse(null);
        sortBy = Optional.ofNullable(sortBy).map(String::trim).filter(s -> !s.isBlank()).orElse("id");
        sortDir = Optional.ofNullable(sortDir).map(String::trim).map(String::toUpperCase).filter(s -> !s.isBlank()).orElse("DESC");
    }

    // Constructor sin pack para compatibilidad hacia atrás
    public CuentaConsultaFiltroRecord(
            String numeroCuenta, Long clienteId, String estatus, Boolean soloActivas,
            int page, int size, String sortBy, String sortDir) {
        this(numeroCuenta, clienteId, estatus, soloActivas, page, size, sortBy, sortDir, null);
    }
}
