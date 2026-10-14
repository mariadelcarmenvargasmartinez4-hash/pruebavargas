package com.proyecto.servicios.model.cuenta;

import com.proyecto.servicios.exception.ReglaNegocioException;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Record inmutable para la consulta paginada y filtrado dinámico de cuentas bancarias.
 * Valida los parámetros de consulta y paginación en su constructor compacto.
 */
@Schema(description = "Record inmutable para filtros y parámetros de paginación de cuentas")
public record CuentaConsultaFiltroRecord(
        @Schema(description = "Filtro por número de cuenta bancaria de 10 dígitos")
        String numeroCuenta,

        @Schema(description = "Filtro por identificador de cliente")
        Long clienteId,

        @Schema(description = "Filtro por estatus de la cuenta (ACTIVA, INACTIVA, CANCELADA)")
        String estatus,

        @Schema(description = "Filtro booleano para solo cuentas activas")
        Boolean soloActivas,

        @Schema(description = "Número de página/pack (inicia en 0)", example = "0")
        int page,

        @Schema(description = "Tamaño de página deseado", example = "10")
        int size,

        @Schema(description = "Campo de ordenación", example = "id")
        String sortBy,

        @Schema(description = "Dirección de orden (ASC / DESC)", example = "DESC")
        String sortDir
) implements Serializable {

    /**
     * Constructor compacto con validación preventiva inmediata.
     */
    public CuentaConsultaFiltroRecord {
        if (page < 0) {
            throw new ReglaNegocioException("El número de página (pack/page) no puede ser menor a cero");
        }
        if (size <= 0) {
            throw new ReglaNegocioException("El tamaño de página debe ser mayor a cero");
        }
        if (size > 100) {
            throw new ReglaNegocioException("El tamaño de página no puede exceder 100 elementos por consulta");
        }
        if (numeroCuenta != null && !numeroCuenta.isBlank() && !numeroCuenta.trim().matches("^\\d{1,10}$")) {
            throw new ReglaNegocioException("El número de cuenta bancaria debe contener solo dígitos numéricos (máximo 10)");
        }

        // Sanitización y valores predeterminados
        numeroCuenta = (numeroCuenta != null && !numeroCuenta.isBlank()) ? numeroCuenta.trim() : null;
        estatus = (estatus != null && !estatus.isBlank()) ? estatus.trim().toUpperCase() : null;
        sortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
        sortDir = (sortDir == null || sortDir.isBlank()) ? "DESC" : sortDir.trim().toUpperCase();
    }
}
