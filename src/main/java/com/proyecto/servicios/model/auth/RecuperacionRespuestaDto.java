package com.proyecto.servicios.model.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta del proceso de recuperación de contraseña")
public class RecuperacionRespuestaDto {

    private long expiracionSegundos;
    private boolean verificado;
    private String resetToken;
}
