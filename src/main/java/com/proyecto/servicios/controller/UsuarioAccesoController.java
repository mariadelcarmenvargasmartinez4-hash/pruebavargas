package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.common.GenericResponse;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.model.usuario.UsuarioActualizacionRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioBusquedaRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioCambioPasswordRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioCreacionRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioDetalleRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioEstadoRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioResponseDto;
import com.proyecto.servicios.service.UsuarioAccesoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la administración integral de usuarios.
 * Cumple estrictamente con las directrices de arquitectura limpia y seguridad:
 * - Sin @PathVariable ni @RequestParam: toda la información y filtros viajan en el cuerpo de la petición mediante DTOs.
 * - Validación declarativa con @Valid y Bean Validation.
 * - Búsqueda dinámica unificada en un solo endpoint.
 * - Respuestas uniformes con GenericResponse y códigos HTTP estándar.
 * - Manejo centralizado de excepciones sin bloques try-catch ni código repetitivo.
 */
@RestController
@RequestMapping(value = {"/usuarios", "/api/usuarios"})
@Tag(name = "Administración de Usuarios", description = "Operaciones de administración, registro, consulta dinámica, actualización y control de estatus de usuarios")
public class UsuarioAccesoController {

    private final UsuarioAccesoService usuarioService;

    public UsuarioAccesoController(UsuarioAccesoService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Información general y catálogo interactivo de usuarios",
               description = "Informa sobre los endpoints disponibles y redirige al Swagger UI interactivo.")
    public ResponseEntity<GenericResponse<String>> obtenerInfoUsuarios() {
        return ResponseEntity.ok(GenericResponse.exito(
                "Para consultar usuarios de forma dinámica y paginada utilice POST /usuarios/consulta con un JSON en el cuerpo. Para interactuar con todos los endpoints visualmente, visite: http://localhost:8088/swagger-ui/index.html",
                "Módulo de Administración de Usuarios activo"));
    }

    @PostMapping(value = {"", "/crear"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear nuevo usuario", description = "Registra un nuevo usuario en el sistema con credenciales seguras y rol asignado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error de validación en los campos de entrada"),
            @ApiResponse(responseCode = "409", description = "El nombre de usuario ya existe o cliente duplicado")
    })
    public ResponseEntity<GenericResponse<UsuarioResponseDto>> crearUsuario(
            @Valid @RequestBody UsuarioCreacionRequestDto request) {
        UsuarioResponseDto usuario = usuarioService.crearUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponse.exito(usuario, "Usuario registrado exitosamente"));
    }

    @PostMapping(value = {"/consulta", "/buscar"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consulta dinámica y paginada de usuarios",
               description = "Único endpoint para consultar usuarios combinando filtros opcionales (id, username, rol, activo, fechas) con soporte de paginación y ordenamiento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consulta realizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de paginación o filtros inválidos")
    })
    public ResponseEntity<GenericResponse<PaginaResponseRecord<UsuarioResponseDto>>> consultarUsuarios(
            @Valid @RequestBody UsuarioBusquedaRequestDto filtro) {
        PaginaResponseRecord<UsuarioResponseDto> resultado = usuarioService.buscarUsuariosDinamico(filtro);
        return ResponseEntity.ok(GenericResponse.exito(resultado, "Usuarios consultados exitosamente"));
    }

    @PostMapping(value = "/detalle", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar detalle de usuario",
               description = "Obtiene los detalles del usuario solicitado a partir de su ID o nombre de usuario recibido en el cuerpo de la petición.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalle del usuario obtenido exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de consulta no proporcionados"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<GenericResponse<UsuarioResponseDto>> obtenerDetalle(
            @Valid @RequestBody UsuarioDetalleRequestDto request) {
        UsuarioResponseDto usuario = usuarioService.obtenerDetalleUsuario(request);
        return ResponseEntity.ok(GenericResponse.exito(usuario, "Detalle de usuario obtenido exitosamente"));
    }

    @PutMapping(value = {"", "/actualizar"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar información de usuario",
               description = "Actualiza los campos permitidos del usuario protegiendo campos de control del servidor como fechas de auditoría y credenciales.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de actualización inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "409", description = "El nuevo nombre de usuario ya está en uso")
    })
    public ResponseEntity<GenericResponse<UsuarioResponseDto>> actualizarUsuario(
            @Valid @RequestBody UsuarioActualizacionRequestDto request) {
        UsuarioResponseDto usuario = usuarioService.actualizarUsuario(request);
        return ResponseEntity.ok(GenericResponse.exito(usuario, "Usuario actualizado exitosamente"));
    }

    @PatchMapping(value = "/password", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cambiar contraseña de usuario",
               description = "Actualiza la contraseña del usuario previa verificación de la contraseña actual y validación de complejidad.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Contraseña actual incorrecta o formato inválido"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<GenericResponse<Void>> cambiarPassword(
            @Valid @RequestBody UsuarioCambioPasswordRequestDto request) {
        usuarioService.cambiarPassword(request);
        return ResponseEntity.ok(GenericResponse.exito(null, "Contraseña actualizada exitosamente"));
    }

    @PatchMapping(value = "/desactivar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Desactivar usuario (baja lógica)",
               description = "Realiza la baja lógica de un usuario activo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario desactivado exitosamente"),
            @ApiResponse(responseCode = "400", description = "El usuario ya se encuentra inactivo"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<GenericResponse<UsuarioResponseDto>> desactivarUsuario(
            @Valid @RequestBody UsuarioEstadoRequestDto request) {
        UsuarioResponseDto usuario = usuarioService.desactivarUsuario(request);
        return ResponseEntity.ok(GenericResponse.exito(usuario, "Usuario desactivado exitosamente"));
    }

    @PatchMapping(value = "/reactivar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reactivar usuario",
               description = "Reactiva el acceso de un usuario previamente desactivado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario reactivado exitosamente"),
            @ApiResponse(responseCode = "400", description = "El usuario ya se encuentra activo"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<GenericResponse<UsuarioResponseDto>> reactivarUsuario(
            @Valid @RequestBody UsuarioEstadoRequestDto request) {
        UsuarioResponseDto usuario = usuarioService.reactivarUsuario(request);
        return ResponseEntity.ok(GenericResponse.exito(usuario, "Usuario reactivado exitosamente"));
    }
}
