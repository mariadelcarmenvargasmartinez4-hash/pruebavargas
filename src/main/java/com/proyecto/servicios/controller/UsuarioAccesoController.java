package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.UsuarioAccesoDto;
import com.proyecto.servicios.model.cliente.UsuarioAgregarRequestDto;
import com.proyecto.servicios.service.UsuarioAccesoService;
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
public class UsuarioAccesoController {

    private final UsuarioAccesoService usuarioService;

    public UsuarioAccesoController(UsuarioAccesoService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping(value = "/filtro", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UsuarioAccesoDto>> filtrarUsuarios(
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(usuarioService.filtrarUsuarios(correo, activo));
    }

    @PutMapping(value = "/agregar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsuarioAccesoDto> agregarUsuario(@Valid @RequestBody UsuarioAgregarRequestDto request) {
        return ResponseEntity.ok(usuarioService.agregarUsuario(request));
    }
}