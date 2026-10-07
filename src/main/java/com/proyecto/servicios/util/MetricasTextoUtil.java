package com.proyecto.servicios.util;

import com.proyecto.servicios.model.util.MetricasTextoDto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utilería para el cálculo exacto de métricas en campos de texto y alfanuméricos.
 * Contabiliza de forma precisa letras, números, espacios y caracteres especiales.
 */
@Component
public class MetricasTextoUtil {

    public MetricasTextoDto analizar(String campo, String valor) {
        if (valor == null) {
            return MetricasTextoDto.builder()
                    .campo(campo)
                    .valor(null)
                    .totalCaracteres(0)
                    .totalLetras(0)
                    .totalDigitos(0)
                    .totalEspacios(0)
                    .totalCaracteresEspeciales(0)
                    .esAlfanumericoEstricto(false)
                    .build();
        }

        int total = valor.length();
        int letras = 0;
        int digitos = 0;
        int espacios = 0;
        int especiales = 0;

        for (int i = 0; i < total; i++) {
            char c = valor.charAt(i);
            if (Character.isLetter(c)) {
                letras++;
            } else if (Character.isDigit(c)) {
                digitos++;
            } else if (Character.isWhitespace(c)) {
                espacios++;
            } else {
                especiales++;
            }
        }

        boolean esAlfanumerico = total > 0 && (letras + digitos == total);

        return MetricasTextoDto.builder()
                .campo(campo)
                .valor(valor)
                .totalCaracteres(total)
                .totalLetras(letras)
                .totalDigitos(digitos)
                .totalEspacios(espacios)
                .totalCaracteresEspeciales(especiales)
                .esAlfanumericoEstricto(esAlfanumerico)
                .build();
    }

    public Map<String, MetricasTextoDto> analizarCampos(Map<String, String> campos) {
        if (campos == null || campos.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, MetricasTextoDto> resultados = new LinkedHashMap<>();
        campos.forEach((campo, valor) -> resultados.put(campo, analizar(campo, valor)));
        return resultados;
    }

    public int contarLetras(String valor) {
        return analizar(null, valor).getTotalLetras();
    }

    public int contarNumeros(String valor) {
        return analizar(null, valor).getTotalDigitos();
    }
}
