package com.proyecto.servicios.controller.catalogo;

import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.model.common.GenericResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/catalogos/ocupaciones")
@Tag(name = "Catálogo de Ocupaciones", description = "Catálogo oficial de ocupaciones y actividades profesionales")
public class OcupacionController {

    private static final List<CatalogoItemDto> OCUPACIONES = Arrays.asList(
            CatalogoItemDto.builder().id(1L).codigo("AF").descripcion("Analista Financiero").activo(true).fechaCreacion(LocalDateTime.now()).build(),
            CatalogoItemDto.builder().id(2L).codigo("IS").descripcion("Ingeniero de Software / TI").activo(true).fechaCreacion(LocalDateTime.now()).build(),
            CatalogoItemDto.builder().id(3L).codigo("AD").descripcion("Administrador de Empresas").activo(true).fechaCreacion(LocalDateTime.now()).build(),
            CatalogoItemDto.builder().id(4L).codigo("CP").descripcion("Contador Público").activo(true).fechaCreacion(LocalDateTime.now()).build(),
            CatalogoItemDto.builder().id(5L).codigo("MD").descripcion("Médico / Especialista de la Salud").activo(true).fechaCreacion(LocalDateTime.now()).build(),
            CatalogoItemDto.builder().id(6L).codigo("AB").descripcion("Abogado / Consultor Jurídico").activo(true).fechaCreacion(LocalDateTime.now()).build(),
            CatalogoItemDto.builder().id(7L).codigo("DC").descripcion("Docente / Académico").activo(true).fechaCreacion(LocalDateTime.now()).build(),
            CatalogoItemDto.builder().id(8L).codigo("AQ").descripcion("Arquitecto / Ingeniero Civil").activo(true).fechaCreacion(LocalDateTime.now()).build(),
            CatalogoItemDto.builder().id(9L).codigo("CM").descripcion("Comerciante / Ventas").activo(true).fechaCreacion(LocalDateTime.now()).build(),
            CatalogoItemDto.builder().id(10L).codigo("EM").descripcion("Empresario / Director General").activo(true).fechaCreacion(LocalDateTime.now()).build(),
            CatalogoItemDto.builder().id(11L).codigo("MK").descripcion("Mercadólogo / Publicista").activo(true).fechaCreacion(LocalDateTime.now()).build(),
            CatalogoItemDto.builder().id(12L).codigo("DG").descripcion("Diseñador Gráfico / Multimedia").activo(true).fechaCreacion(LocalDateTime.now()).build(),
            CatalogoItemDto.builder().id(13L).codigo("ES").descripcion("Estudiante").activo(true).fechaCreacion(LocalDateTime.now()).build(),
            CatalogoItemDto.builder().id(14L).codigo("PF").descripcion("Profesionista Independiente").activo(true).fechaCreacion(LocalDateTime.now()).build(),
            CatalogoItemDto.builder().id(15L).codigo("OT").descripcion("Otro").activo(true).fechaCreacion(LocalDateTime.now()).build()
    );

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar catálogo de ocupaciones")
    public ResponseEntity<GenericResponse<List<CatalogoItemDto>>> listar() {
        return ResponseEntity.ok(GenericResponse.exito(OCUPACIONES, "Catálogo de ocupaciones consultado exitosamente"));
    }
}
