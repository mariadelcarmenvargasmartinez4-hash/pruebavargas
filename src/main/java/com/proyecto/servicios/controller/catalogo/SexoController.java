package com.proyecto.servicios.controller.catalogo;

import com.proyecto.servicios.model.catalogo.CatalogoActualizacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.service.SexoService;
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
@RequestMapping("/api/catalogos/sexos")
@Tag(name = "Catálogo de Sexos", description = "API dedicada para la administración y consulta desacoplada del catálogo de sexos")
public class SexoController {

    private final SexoService sexoService;

    public SexoController(SexoService sexoService) {
        this.sexoService = sexoService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar elementos del catálogo de sexos", description = "Permite consultar todos los sexos o filtrar solo los activos")
    public ResponseEntity<List<CatalogoItemDto>> listar(@RequestParam(required = false, defaultValue = "false") boolean soloActivos) {
        return ResponseEntity.ok(soloActivos ? sexoService.listarActivos() : sexoService.listarTodos());
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar sexo por identificador único")
    public ResponseEntity<CatalogoItemDto> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(sexoService.obtenerPorId(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear nuevo elemento en el catálogo de sexos")
    public ResponseEntity<CatalogoItemDto> crear(@Valid @RequestBody CatalogoCreacionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sexoService.crear(dto));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar elemento del catálogo de sexos")
    public ResponseEntity<CatalogoItemDto> actualizar(@PathVariable Long id, @Valid @RequestBody CatalogoActualizacionDto dto) {
        return ResponseEntity.ok(sexoService.actualizar(id, dto));
    }

    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Desactivar elemento del catálogo de sexos")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        sexoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
