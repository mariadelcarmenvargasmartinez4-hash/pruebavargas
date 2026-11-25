package com.proyecto.servicios.controller.catalogo;

import com.proyecto.servicios.model.catalogo.CatalogoActualizacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoConsultaFiltroRecord;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.model.common.GenericResponse;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.service.EntidadFederativaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos/entidades-federativas")
@Tag(name = "Catálogo de Entidades Federativas", description = "API dedicada para la administración y consulta del catálogo de las 32 entidades federativas de México")
public class EntidadFederativaController {

    private final EntidadFederativaService entidadFederativaService;

    public EntidadFederativaController(EntidadFederativaService entidadFederativaService) {
        this.entidadFederativaService = entidadFederativaService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar entidades federativas con paginación o listado completo")
    public ResponseEntity<GenericResponse<PaginaResponseRecord<CatalogoItemDto>>> listar(
            @Parameter(description = "Número de pack solicitado") @RequestParam(value = "pack", required = false) Integer pack,
            @Parameter(description = "Número de página (inicia en 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página") @RequestParam(defaultValue = "50") int size,
            @Parameter(description = "Campo de ordenación") @RequestParam(defaultValue = "descripcion") String sortBy,
            @Parameter(description = "Dirección de orden (ASC / DESC)") @RequestParam(defaultValue = "ASC") String sortDir,
            @Parameter(description = "Filtrar solo activos") @RequestParam(defaultValue = "true") Boolean soloActivos,
            @Parameter(description = "Búsqueda por código o descripción") @RequestParam(required = false) String busqueda) {

        CatalogoConsultaFiltroRecord filtro = new CatalogoConsultaFiltroRecord(soloActivos, busqueda, page, size, sortBy, sortDir, pack);
        PaginaResponseRecord<CatalogoItemDto> resultado = entidadFederativaService.buscarPaginado(filtro);
        return ResponseEntity.ok(GenericResponse.exito(resultado, "Catálogo de entidades federativas consultado exitosamente"));
    }

    @GetMapping(value = "/todas", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar todas las entidades federativas activas")
    public ResponseEntity<GenericResponse<List<CatalogoItemDto>>> listarTodas() {
        return ResponseEntity.ok(GenericResponse.exito(entidadFederativaService.listarActivos(), "Lista de entidades federativas activas"));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Obtener entidad federativa por ID")
    public ResponseEntity<GenericResponse<CatalogoItemDto>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(GenericResponse.exito(entidadFederativaService.obtenerPorId(id), "Entidad federativa obtenida"));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear nueva entidad federativa")
    public ResponseEntity<GenericResponse<CatalogoItemDto>> crear(@Valid @RequestBody CatalogoCreacionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponse.exito(entidadFederativaService.crear(dto), "Entidad federativa creada exitosamente"));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar entidad federativa existente")
    public ResponseEntity<GenericResponse<CatalogoItemDto>> actualizar(@PathVariable Long id, @Valid @RequestBody CatalogoActualizacionDto dto) {
        return ResponseEntity.ok(GenericResponse.exito(entidadFederativaService.actualizar(id, dto), "Entidad federativa actualizada"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar entidad federativa")
    public ResponseEntity<GenericResponse<Void>> desactivar(@PathVariable Long id) {
        entidadFederativaService.desactivar(id);
        return ResponseEntity.ok(GenericResponse.exito(null, "Entidad federativa desactivada exitosamente"));
    }
}
