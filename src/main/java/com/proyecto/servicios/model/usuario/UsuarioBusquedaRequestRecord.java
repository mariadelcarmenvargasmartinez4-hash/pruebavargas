package com.proyecto.servicios.model.usuario;

import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Record inmutable para filtros dinámicos de consulta y paginación de usuarios.
 * Aplica validación Fail-Fast y normalización declarativa de parámetros.
 */
@Schema(description = "Record inmutable para filtros y parámetros de paginación de usuarios")
public record UsuarioBusquedaRequestRecord(
        Long id,
        String username,
        String rol,
        Boolean activo,
        Long clienteId,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        int page,
        int size,
        String sortBy,
        String sortDir,
        Integer pack
) implements Serializable {

    public UsuarioBusquedaRequestRecord {
        int paginaEfectiva = Optional.ofNullable(pack).orElse(page);
        DeclarativeAssertUtil.asegurar(paginaEfectiva >= 0, "El número de página (pack/page) no puede ser menor a cero");
        DeclarativeAssertUtil.asegurar(size > 0 && size <= 100, "El tamaño de página debe estar entre 1 y 100 elementos");

        page = paginaEfectiva;
        username = Optional.ofNullable(username).map(String::trim).filter(u -> !u.isBlank()).orElse(null);
        rol = Optional.ofNullable(rol).map(String::trim).map(String::toUpperCase).filter(r -> !r.isBlank()).orElse(null);
        sortBy = Optional.ofNullable(sortBy).map(String::trim).filter(s -> !s.isBlank()).orElse("id");
        sortDir = Optional.ofNullable(sortDir).map(String::trim).map(String::toUpperCase).filter(s -> !s.isBlank()).orElse("DESC");

        DeclarativeAssertUtil.asegurar("ASC".equals(sortDir) || "DESC".equals(sortDir), "La dirección de orden debe ser ASC o DESC");
    }

    public static UsuarioBusquedaRequestRecord de(UsuarioBusquedaRequestDto dto) {
        String usernameFiltro = Optional.ofNullable(dto.getUsername())
                .filter(u -> !u.isBlank())
                .orElse(dto.getCorreo());
        int pageVal = Optional.ofNullable(dto.getPage()).orElse(0);
        int sizeVal = Optional.ofNullable(dto.getSize()).orElse(10);
        String sortB = Optional.ofNullable(dto.getSortBy()).orElse("id");
        String sortD = Optional.ofNullable(dto.getSortDir()).orElse("DESC");

        return new UsuarioBusquedaRequestRecord(
                dto.getId(),
                usernameFiltro,
                dto.getRol(),
                dto.getActivo(),
                dto.getClienteId(),
                dto.getFechaInicio(),
                dto.getFechaFin(),
                pageVal,
                sizeVal,
                sortB,
                sortD,
                dto.getPack()
        );
    }
}
