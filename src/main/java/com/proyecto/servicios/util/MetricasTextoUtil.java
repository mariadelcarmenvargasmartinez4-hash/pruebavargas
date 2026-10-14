package com.proyecto.servicios.util;

import com.proyecto.servicios.model.util.MetricasTextoDto;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Utilería declarativa para el cálculo exacto de métricas en campos de texto y alfanuméricos
 * implementada funcionalmente con Streams sin condicionales 'if'.
 */
@Component
public class MetricasTextoUtil {

    public MetricasTextoDto analizar(String campo, String valor) {
        return Optional.ofNullable(valor)
                .map(v -> {
                    int total = v.length();
                    int letras = (int) v.chars().filter(Character::isLetter).count();
                    int digitos = (int) v.chars().filter(Character::isDigit).count();
                    int espacios = (int) v.chars().filter(Character::isWhitespace).count();
                    int especiales = total - (letras + digitos + espacios);
                    boolean esAlfanumerico = total > 0 && (letras + digitos == total);

                    return MetricasTextoDto.builder()
                            .campo(campo)
                            .valor(v)
                            .totalCaracteres(total)
                            .totalLetras(letras)
                            .totalDigitos(digitos)
                            .totalEspacios(espacios)
                            .totalCaracteresEspeciales(especiales)
                            .esAlfanumericoEstricto(esAlfanumerico)
                            .build();
                })
                .orElseGet(() -> MetricasTextoDto.builder()
                        .campo(campo)
                        .valor(null)
                        .totalCaracteres(0)
                        .totalLetras(0)
                        .totalDigitos(0)
                        .totalEspacios(0)
                        .totalCaracteresEspeciales(0)
                        .esAlfanumericoEstricto(false)
                        .build());
    }

    public java.util.Map<String, MetricasTextoDto> analizarCampos(java.util.Map<String, String> campos) {
        return Optional.ofNullable(campos)
                .map(map -> map.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                java.util.Map.Entry::getKey,
                                entry -> analizar(entry.getKey(), entry.getValue()),
                                (a, b) -> a,
                                java.util.LinkedHashMap::new
                        )))
                .orElseGet(java.util.LinkedHashMap::new);
    }
}
