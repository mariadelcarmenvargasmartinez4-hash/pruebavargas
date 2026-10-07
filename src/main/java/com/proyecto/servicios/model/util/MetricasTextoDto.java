package com.proyecto.servicios.model.util;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Métricas cuantitativas de caracteres para campos alfanuméricos y de texto")
public class MetricasTextoDto {

    @Schema(description = "Nombre o identificador del campo analizado", example = "curp")
    private String campo;

    @Schema(description = "Texto o valor evaluado", example = "PELJ900515HDFRPR09")
    private String valor;

    @Schema(description = "Número total de caracteres", example = "18")
    private int totalCaracteres;

    @Schema(description = "Número total de letras (alfabéticos)", example = "10")
    private int totalLetras;

    @Schema(description = "Número total de dígitos (numéricos)", example = "8")
    private int totalDigitos;

    @Schema(description = "Número total de espacios en blanco", example = "0")
    private int totalEspacios;

    @Schema(description = "Número total de caracteres especiales o puntuación", example = "0")
    private int totalCaracteresEspeciales;

    @Schema(description = "Indica si el campo es estrictamente alfanumérico cerrado (solo letras y números)", example = "true")
    private boolean esAlfanumericoEstricto;
}
