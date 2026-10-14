package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.UsuarioAccesoDto;
import com.proyecto.servicios.model.cliente.UsuarioAgregarRequestDto;
import com.proyecto.servicios.model.cliente.UsuarioConsultaFiltroRecord;
import com.proyecto.servicios.model.common.GenericResponse;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.service.UsuarioAccesoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuarios de Acceso", description = "Operaciones de consulta paginada y gestión de credenciales de clientes")
public class UsuarioAccesoController {

    private final UsuarioAccesoService usuarioService;

    public UsuarioAccesoController(UsuarioAccesoService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar usuarios con paginación y filtros dinámicos",
               description = "Permite filtrar por correo y estatus con soporte nativo de pack y tamaño de página dinámico")
    public ResponseEntity<GenericResponse<PaginaResponseRecord<UsuarioAccesoDto>>> consultarUsuariosPaginados(
            @Parameter(description = "Número de pack de registros solicitado", example = "0")
            @RequestParam(value = "pack", required = false) Integer pack,
            @Parameter(description = "Número de página (inicia en 0)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página", example = "10")
            @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenamiento", example = "id")
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @Parameter(description = "Dirección de orden (ASC / DESC)", example = "DESC")
            @RequestParam(value = "sortDir", defaultValue = "DESC") String sortDir,
            @Parameter(description = "Filtrar por correo o username")
            @RequestParam(required = false) String correo,
            @Parameter(description = "Filtrar por estatus activo")
            @RequestParam(required = false) Boolean activo) {

        UsuarioConsultaFiltroRecord filtro = new UsuarioConsultaFiltroRecord(correo, activo, page, size, sortBy, sortDir, pack);
        PaginaResponseRecord<UsuarioAccesoDto> resultado = usuarioService.buscarUsuariosPaginados(filtro);
        return ResponseEntity.ok(GenericResponse.exito(resultado, "Usuarios consultados exitosamente"));
    }

    @GetMapping(value = "/filtro", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Filtrar usuarios (compatibilidad)", description = "Retorna lista de usuarios aplicando paginación interna")
    public ResponseEntity<List<UsuarioAccesoDto>> filtrarUsuarios(
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(usuarioService.filtrarUsuarios(correo, activo));
    }

    @PutMapping(value = "/agregar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear credenciales de usuario para un cliente")
    public ResponseEntity<UsuarioAccesoDto> agregarUsuario(@Valid @RequestBody UsuarioAgregarRequestDto request) {
        return ResponseEntity.ok(usuarioService.agregarUsuario(request));
    }
}