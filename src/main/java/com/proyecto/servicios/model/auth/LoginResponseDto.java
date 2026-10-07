package com.proyecto.servicios.model.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// DTO de respuesta para el inicio de sesion exitoso con token JWT
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta de autenticación exitosa con token JWT")
public class LoginResponseDto {

    @Schema(description = "Código de respuesta (0 = Éxito)", example = "0")
    private Integer codigo;

    @Schema(description = "Mensaje descriptivo", example = "Autenticación exitosa")
    private String mensaje;

    @Schema(description = "Token JWT para acceso a endpoints protegidos")
    private String token;

    @Schema(description = "Tipo de token", example = "Bearer")
    private String tipoToken;

    @Schema(description = "Nombre de usuario autenticado", example = "juan.perez@example.com")
    private String username;

    @Schema(description = "Identificador del cliente asociado", example = "1")
    private Long clienteId;

    @Schema(description = "Nombre completo del cliente", example = "Juan Carlos Perez Lopez")
    private String nombreCompleto;

    @Schema(description = "Tiempo de expiración del token en segundos", example = "3600")
    private Long expiresIn;

    @Schema(description = "Fecha y hora de la autenticación")
    private LocalDateTime fechaAutenticacion;
}
