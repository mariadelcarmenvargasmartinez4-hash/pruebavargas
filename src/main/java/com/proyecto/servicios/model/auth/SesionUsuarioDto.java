package com.proyecto.servicios.model.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información de la sesión activa del usuario respaldada en el servidor")
public class SesionUsuarioDto {

    @Schema(description = "Identificador único de la sesión activa", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String sessionId;

    @Schema(description = "Nombre de usuario (correo)", example = "juan.perez@example.com")
    private String username;

    @Schema(description = "Dispositivo de origen", example = "WEB")
    private String dispositivo;

    @Schema(description = "Dirección IP del cliente", example = "192.168.1.100")
    private String ipAddress;

    @Schema(description = "Fecha y hora de inicio de la sesión")
    private LocalDateTime fechaInicio;

    @Schema(description = "Fecha y hora del último acceso")
    private LocalDateTime ultimoAcceso;

    @Schema(description = "Indica si la sesión se encuentra activa")
    private Boolean activa;
}
