package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.auth.RecuperacionRespuestaDto;
import com.proyecto.servicios.model.auth.RecuperacionRestablecerDto;
import com.proyecto.servicios.model.auth.RecuperacionSolicitudDto;
import com.proyecto.servicios.model.auth.RecuperacionVerificacionDto;
import com.proyecto.servicios.model.common.GenericResponse;
import com.proyecto.servicios.service.RecuperacionPasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/recuperacion")
@Tag(name = "Recuperación de Contraseña", description = "Flujo seguro de restablecimiento de contraseña mediante código temporal enviado por correo electrónico")
public class RecuperacionPasswordController {

    private final RecuperacionPasswordService recuperacionService;

    public RecuperacionPasswordController(RecuperacionPasswordService recuperacionService) {
        this.recuperacionService = recuperacionService;
    }

    @PostMapping(value = "/solicitar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Solicitar código de recuperación", description = "Envía un código temporal de un solo uso al correo si existe, sin revelar enumeración de cuentas")
    public ResponseEntity<GenericResponse<RecuperacionRespuestaDto>> solicitar(@Valid @RequestBody RecuperacionSolicitudDto request) {
        RecuperacionRespuestaDto respuesta = recuperacionService.solicitarRecuperacion(request);
        return ResponseEntity.ok(GenericResponse.exito(
                respuesta,
                "Si el correo electrónico está registrado en nuestro sistema, recibirás un código temporal de recuperación."
        ));
    }

    @PostMapping(value = "/verificar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Verificar código temporal", description = "Valida el código de 6 dígitos con control de vigencia e intentos máximos")
    public ResponseEntity<GenericResponse<RecuperacionRespuestaDto>> verificar(@Valid @RequestBody RecuperacionVerificacionDto request) {
        RecuperacionRespuestaDto respuesta = recuperacionService.verificarCodigo(request);
        return ResponseEntity.ok(GenericResponse.exito(respuesta, "Código verificado correctamente. Ahora puedes ingresar tu nueva contraseña."));
    }

    @PostMapping(value = "/restablecer", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Restablecer contraseña", description = "Guarda la nueva contraseña cifrada con BCrypt previa verificación del código e invalida el código utilizado")
    public ResponseEntity<GenericResponse<Void>> restablecer(@Valid @RequestBody RecuperacionRestablecerDto request) {
        recuperacionService.restablecerPassword(request);
        return ResponseEntity.ok(GenericResponse.exito(null, "Tu contraseña ha sido actualizada exitosamente. Ya puedes iniciar sesión."));
    }
}
