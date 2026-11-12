package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.auth.LoginRequestDto;
import com.proyecto.servicios.model.auth.LoginResponseDto;
import com.proyecto.servicios.model.auth.SesionUsuarioDto;
import com.proyecto.servicios.model.auth.TwoFactorRespuestaDto;
import com.proyecto.servicios.model.auth.TwoFactorSolicitudDto;
import com.proyecto.servicios.model.auth.TwoFactorVerificacionDto;
import com.proyecto.servicios.model.common.GenericResponse;
import com.proyecto.servicios.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Endpoints de login con JWT, autenticación multifactor (2FA) y control de sesión única respaldada por servidor")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Iniciar sesión", description = "Valida credenciales con BCrypt, emite token JWT y registra sesión única en el servidor invalidando sesiones anteriores")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
            @ApiResponse(responseCode = "403", description = "Usuario o cliente inactivo")
    })
    public ResponseEntity<LoginResponseDto> iniciarSesion(@Valid @RequestBody LoginRequestDto request) {
        request.toRecord();
        return ResponseEntity.ok(authService.iniciarSesion(request));
    }

    @PostMapping(value = "/2fa/solicitar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Solicitar PIN temporal 2FA", description = "Genera un código OTP de 6 dígitos numéricos con expiración de 5 minutos y mitigación contra fuerza bruta")
    public ResponseEntity<GenericResponse<TwoFactorRespuestaDto>> solicitarTwoFactor(@Valid @RequestBody TwoFactorSolicitudDto request) {
        TwoFactorRespuestaDto respuesta = authService.solicitarTwoFactor(request);
        return ResponseEntity.ok(GenericResponse.exito(respuesta, "Código de verificación 2FA generado exitosamente"));
    }

    @PostMapping(value = "/2fa/verificar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Verificar PIN temporal 2FA", description = "Valida el PIN temporal de 6 dígitos y emite el token JWT de acceso con control de sesión única")
    public ResponseEntity<LoginResponseDto> verificarTwoFactor(@Valid @RequestBody TwoFactorVerificacionDto request) {
        return ResponseEntity.ok(authService.verificarTwoFactor(request));
    }

    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cerrar sesión", description = "Invalida la sesión activa en el servidor impidiendo el uso posterior del token JWT")
    public ResponseEntity<GenericResponse<Void>> cerrarSesion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            authService.cerrarSesion(auth.getName());
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(GenericResponse.exito(null, "Sesión cerrada exitosamente en el servidor"));
    }

    @GetMapping(value = "/sesion", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar estado de sesión", description = "Obtiene los detalles de la sesión única activa del usuario autenticado")
    public ResponseEntity<GenericResponse<SesionUsuarioDto>> obtenerSesionActiva() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? auth.getName() : null;
        SesionUsuarioDto sesion = authService.obtenerSesionActiva(username);
        return ResponseEntity.ok(GenericResponse.exito(sesion, "Información de sesión activa consultada con éxito"));
    }
}
