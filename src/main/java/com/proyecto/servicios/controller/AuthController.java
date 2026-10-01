package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.auth.LoginRequestDto;
import com.proyecto.servicios.model.auth.LoginResponseDto;
import com.proyecto.servicios.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponseDto> iniciarSesion(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.iniciarSesion(request));
    }
}