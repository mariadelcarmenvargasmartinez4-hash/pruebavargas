package com.proyecto.servicios.model.cliente;

import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Record inmutable para la consulta paginada y filtrado dinámico de clientes.
 * Aplica validaciones declarativas desde el constructor compacto del record para detonar
 * excepciones antes de entrar a la capa de servicios o interfaces, con soporte nativo de pack.
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

        @Schema(description = "Número de página (inicia en 0)", example = "0")
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

    /**
     * Constructor compacto: valida y sanitiza de manera declarativa sin condicionales 'if'.
     */
    public ClienteConsultaFiltroRecord {
        int paginaEfectiva = Optional.ofNullable(pack).orElse(page);
        DeclarativeAssertUtil.asegurar(paginaEfectiva >= 0, "El número de página (pack/page) no puede ser menor a cero");
        DeclarativeAssertUtil.asegurar(size > 0 && size <= 100, "El tamaño de página debe estar entre 1 y 100 elementos por consulta");

        Optional.ofNullable(fechaInicio).ifPresent(fi ->
                Optional.ofNullable(fechaFin).ifPresent(ff ->
                        DeclarativeAssertUtil.prohibir(fi.isAfter(ff), "La fecha de inicio no puede ser posterior a la fecha de fin")));

        Optional.ofNullable(numeroCuenta).filter(nc -> !nc.isBlank()).ifPresent(nc ->
                DeclarativeAssertUtil.asegurar(nc.trim().matches("^\\d{1,10}$"),
                        "El número de cuenta bancaria debe contener solo dígitos numéricos (máximo 10)"));

        // Sanitización y valores predeterminados declarativos
        page = paginaEfectiva;
        nombre = Optional.ofNullable(nombre).map(String::trim).filter(s -> !s.isBlank()).orElse(null);
        apellidoPaterno = Optional.ofNullable(apellidoPaterno).map(String::trim).filter(s -> !s.isBlank()).orElse(null);
        apellidoMaterno = Optional.ofNullable(apellidoMaterno).map(String::trim).filter(s -> !s.isBlank()).orElse(null);
        curp = Optional.ofNullable(curp).map(String::trim).map(String::toUpperCase).filter(s -> !s.isBlank()).orElse(null);
        rfc = Optional.ofNullable(rfc).map(String::trim).map(String::toUpperCase).filter(s -> !s.isBlank()).orElse(null);
        correo = Optional.ofNullable(correo).map(String::trim).map(String::toLowerCase).filter(s -> !s.isBlank()).orElse(null);
        numeroCuenta = Optional.ofNullable(numeroCuenta).map(String::trim).filter(s -> !s.isBlank()).orElse(null);
        sortBy = Optional.ofNullable(sortBy).map(String::trim).filter(s -> !s.isBlank()).orElse("id");
        sortDir = Optional.ofNullable(sortDir).map(String::trim).map(String::toUpperCase).filter(s -> !s.isBlank()).orElse("DESC");
    }

    // Constructor sin pack para compatibilidad hacia atrás
    public ClienteConsultaFiltroRecord(
            String nombre, String apellidoPaterno, String apellidoMaterno,
            String curp, String rfc, String correo, String numeroCuenta,
            Boolean soloActivos, LocalDateTime fechaInicio, LocalDateTime fechaFin,
            int page, int size, String sortBy, String sortDir) {
        this(nombre, apellidoPaterno, apellidoMaterno, curp, rfc, correo, numeroCuenta,
                soloActivos, fechaInicio, fechaFin, page, size, sortBy, sortDir, null);
    }
}
