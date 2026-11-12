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
@Schema(description = "Respuesta de solicitud o verificación 2FA")
public class TwoFactorRespuestaDto {

    @Schema(description = "Código de resultado (0 = Éxito)", example = "0")
    private Integer codigo;

    @Schema(description = "Mensaje informativo", example = "PIN temporal generado y enviado con éxito")
    private String mensaje;

    @Schema(description = "Tiempo de validez en segundos", example = "300")
    private Long expiracionSegundos;

    @Schema(description = "Fecha de expiración")
    private LocalDateTime fechaExpiracion;

    @Schema(description = "Indica si la verificación fue exitosa")
    private Boolean verificado;
}
