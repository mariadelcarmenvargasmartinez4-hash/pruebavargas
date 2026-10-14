package com.proyecto.servicios.controller.catalogo;

import com.proyecto.servicios.model.catalogo.CatalogoActualizacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.service.NacionalidadService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/catalogos/nacionalidades")
@Tag(name = "Catálogo de Nacionalidades", description = "API dedicada para la administración y consulta desacoplada del catálogo de nacionalidades")
public class NacionalidadController {

    private final NacionalidadService nacionalidadService;

    public NacionalidadController(NacionalidadService nacionalidadService) {
        this.nacionalidadService = nacionalidadService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar elementos del catálogo de nacionalidades con paginación", 
               description = "Permite paginar y filtrar con soporte nativo de pack y tamaño de página dinámico")
    public ResponseEntity<com.proyecto.servicios.model.common.GenericResponse<com.proyecto.servicios.model.common.PaginaResponseRecord<CatalogoItemDto>>> listar(
            @io.swagger.v3.oas.annotations.Parameter(description = "Número de pack solicitado") @RequestParam(value = "pack", required = false) Integer pack,
            @io.swagger.v3.oas.annotations.Parameter(description = "Número de página (inicia en 0)") @RequestParam(defaultValue = "0") int page,
            @io.swagger.v3.oas.annotations.Parameter(description = "Cantidad de elementos por página") @RequestParam(defaultValue = "10") int size,
            @io.swagger.v3.oas.annotations.Parameter(description = "Campo de ordenación") @RequestParam(defaultValue = "id") String sortBy,
            @io.swagger.v3.oas.annotations.Parameter(description = "Dirección de orden (ASC / DESC)") @RequestParam(defaultValue = "ASC") String sortDir,
            @io.swagger.v3.oas.annotations.Parameter(description = "Filtrar solo activos") @RequestParam(required = false) Boolean soloActivos,
            @io.swagger.v3.oas.annotations.Parameter(description = "Búsqueda por código o descripción") @RequestParam(required = false) String busqueda) {

        com.proyecto.servicios.model.catalogo.CatalogoConsultaFiltroRecord filtro =
                new com.proyecto.servicios.model.catalogo.CatalogoConsultaFiltroRecord(soloActivos, busqueda, page, size, sortBy, sortDir, pack);
        com.proyecto.servicios.model.common.PaginaResponseRecord<CatalogoItemDto> resultado = nacionalidadService.buscarPaginado(filtro);
        return ResponseEntity.ok(com.proyecto.servicios.model.common.GenericResponse.exito(resultado, "Catálogo de nacionalidades consultado exitosamente"));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar nacionalidad por identificador único")
    public ResponseEntity<CatalogoItemDto> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(nacionalidadService.obtenerPorId(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear nuevo elemento en el catálogo de nacionalidades")
    public ResponseEntity<CatalogoItemDto> crear(@Valid @RequestBody CatalogoCreacionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nacionalidadService.crear(dto));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar elemento del catálogo de nacionalidades")
    public ResponseEntity<CatalogoItemDto> actualizar(@PathVariable Long id, @Valid @RequestBody CatalogoActualizacionDto dto) {
        return ResponseEntity.ok(nacionalidadService.actualizar(id, dto));
    }

    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Desactivar elemento del catálogo de nacionalidades")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        nacionalidadService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
