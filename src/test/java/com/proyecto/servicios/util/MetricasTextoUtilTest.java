package com.proyecto.servicios.util;

import com.proyecto.servicios.model.util.MetricasTextoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetricasTextoUtilTest {

    private MetricasTextoUtil metricasTextoUtil;

    @BeforeEach
    void setUp() {
        metricasTextoUtil = new MetricasTextoUtil();
    }

    @Test
    void testAnalizar_CurpValido() {
        // CURP de 18 caracteres: 10 letras, 8 digitos
        String curp = "PELJ900515HDFRPR09";
        MetricasTextoDto dto = metricasTextoUtil.analizar("curp", curp);

        assertNotNull(dto);
        assertEquals(18, dto.getTotalCaracteres());
        assertEquals(10, dto.getTotalLetras());
        assertEquals(8, dto.getTotalDigitos());
        assertEquals(0, dto.getTotalEspacios());
        assertEquals(0, dto.getTotalCaracteresEspeciales());
        assertTrue(dto.isEsAlfanumericoEstricto());
    }

    @Test
    void testAnalizar_TextoConEspaciosYEspeciales() {
        String texto = "Av. Reforma #123";
        MetricasTextoDto dto = metricasTextoUtil.analizar("calle", texto);

        assertNotNull(dto);
        assertEquals(16, dto.getTotalCaracteres());
        assertEquals(9, dto.getTotalLetras());
        assertEquals(3, dto.getTotalDigitos());
        assertEquals(2, dto.getTotalEspacios());
        assertEquals(2, dto.getTotalCaracteresEspeciales());
        assertFalse(dto.isEsAlfanumericoEstricto());
    }

    @Test
    void testAnalizar_TextoNulo() {
        MetricasTextoDto dto = metricasTextoUtil.analizar("campoNulo", null);

        assertNotNull(dto);
        assertEquals(0, dto.getTotalCaracteres());
        assertEquals(0, dto.getTotalLetras());
        assertEquals(0, dto.getTotalDigitos());
        assertFalse(dto.isEsAlfanumericoEstricto());
    }
}
