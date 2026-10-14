package com.proyecto.servicios.model.cliente;

import com.proyecto.servicios.exception.ReglaNegocioException;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Record inmutable para la consulta paginada y filtrado dinámico de clientes.
 * Aplica validaciones desde el constructor compacto del record para detonar
 * excepciones antes de entrar a la capa de servicios o interfaces.
 */
@Schema(description = "Record inmutable para filtros y parámetros de paginación de clientes")
public record ClienteConsultaFiltroRecord(
        @Schema(description = "Filtro por nombre")
        String nombre,

        @Schema(description = "Filtro por apellido paterno")
        String apellidoPaterno,

        @Schema(description = "Filtro por apellido materno")
        String apellidoMaterno,

        @Schema(description = "Filtro por CURP")
        String curp,

        @Schema(description = "Filtro por RFC")
        String rfc,

        @Schema(description = "Filtro por correo electrónico")
        String correo,

        @Schema(description = "Filtro por número de cuenta bancaria")
        String numeroCuenta,

        @Schema(description = "Filtro por estatus activo")
        Boolean soloActivos,

        @Schema(description = "Fecha inicial del rango de registro")
        LocalDateTime fechaInicio,

        @Schema(description = "Fecha final del rango de registro")
        LocalDateTime fechaFin,

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
     * Constructor compacto: valida los parámetros en el record directamente
     * para evitar lógica defensiva dentro de los servicios e interfaces.
     */
    public ClienteConsultaFiltroRecord {
        if (page < 0) {
            throw new ReglaNegocioException("El número de página (pack/page) no puede ser menor a cero");
        }
        if (size <= 0) {
            throw new ReglaNegocioException("El tamaño de página debe ser mayor a cero");
        }
        if (size > 100) {
            throw new ReglaNegocioException("El tamaño de página no puede ser mayor a 100 elementos por consulta");
        }
        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new ReglaNegocioException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        if (numeroCuenta != null && !numeroCuenta.isBlank() && !numeroCuenta.trim().matches("^\\d{1,10}$")) {
            throw new ReglaNegocioException("El número de cuenta bancaria debe contener solo dígitos numéricos (máximo 10)");
        }

        // Sanitización y valores predeterminados
        nombre = (nombre != null && !nombre.isBlank()) ? nombre.trim() : null;
        apellidoPaterno = (apellidoPaterno != null && !apellidoPaterno.isBlank()) ? apellidoPaterno.trim() : null;
        apellidoMaterno = (apellidoMaterno != null && !apellidoMaterno.isBlank()) ? apellidoMaterno.trim() : null;
        curp = (curp != null && !curp.isBlank()) ? curp.trim().toUpperCase() : null;
        rfc = (rfc != null && !rfc.isBlank()) ? rfc.trim().toUpperCase() : null;
        correo = (correo != null && !correo.isBlank()) ? correo.trim().toLowerCase() : null;
        numeroCuenta = (numeroCuenta != null && !numeroCuenta.isBlank()) ? numeroCuenta.trim() : null;
        sortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
        sortDir = (sortDir == null || sortDir.isBlank()) ? "DESC" : sortDir.trim().toUpperCase();
    }
}
