package com.proyecto.servicios.model.usuario;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO para la consulta dinámica y paginada de usuarios mediante @RequestBody.
 * Permite combinar múltiples filtros opcionales en una única petición sin recurrir a @RequestParam.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Criterios de búsqueda dinámica y parámetros de paginación para usuarios")
public class UsuarioBusquedaRequestDto implements Serializable {

    @Schema(description = "Filtro por ID de usuario", example = "1")
    private Long id;

    @Schema(description = "Filtro por nombre de usuario o coincidencia parcial", example = "admin")
    private String username;

    @Schema(description = "Alias opcional de correo electrónico para búsqueda", example = "usuario@empresa.com")
    private String correo;

    @Schema(description = "Filtro por rol (CLIENTE o ADMIN)", example = "ADMIN")
    private String rol;

    @Schema(description = "Filtro por estatus de usuario (true=activo, false=inactivo)", example = "true")
    private Boolean activo;

    @Schema(description = "Filtro por identificador de cliente asociado", example = "5")
    private Long clienteId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fecha inicial de creación (formato yyyy-MM-ddTHH:mm:ss)", example = "2026-01-01T00:00:00")
    private LocalDateTime fechaInicio;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fecha final de creación (formato yyyy-MM-ddTHH:mm:ss)", example = "2026-12-31T23:59:59")
    private LocalDateTime fechaFin;

    @Schema(description = "Número de pack de registros solicitado (alias nativo de página)", example = "0")
    private Integer pack;

    @Min(value = 0, message = "El número de página no puede ser menor a 0")
    @Builder.Default
    @Schema(description = "Número de página (inicia en 0)", example = "0", defaultValue = "0")
    private Integer page = 0;

    @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
    @Max(value = 100, message = "El tamaño de página no puede exceder 100")
    @Builder.Default
    @Schema(description = "Cantidad de elementos por página", example = "10", defaultValue = "10")
    private Integer size = 10;

    @Builder.Default
    @Schema(description = "Campo por el cual ordenar", example = "id", defaultValue = "id")
    private String sortBy = "id";

    @Pattern(regexp = "^(?i)(ASC|DESC)$", message = "La dirección de ordenamiento debe ser ASC o DESC")
    @Builder.Default
    @Schema(description = "Dirección de ordenamiento (ASC / DESC)", example = "DESC", defaultValue = "DESC")
    private String sortDir = "DESC";

    public UsuarioBusquedaRequestRecord toRecord() {
        return UsuarioBusquedaRequestRecord.de(this);
    }
}
