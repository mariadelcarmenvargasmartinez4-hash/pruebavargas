package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.util.MetricasTextoDto;
import com.proyecto.servicios.util.MetricasTextoUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/utilidades/metricas-texto")
@Tag(name = "Métricas de Texto", description = "Servicio de análisis y métricas precisas para campos de texto y alfanuméricos")
public class MetricasTextoController {

    private final MetricasTextoUtil metricasTextoUtil;

    public MetricasTextoController(MetricasTextoUtil metricasTextoUtil) {
        this.metricasTextoUtil = metricasTextoUtil;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Calcular métricas de un texto (conteo de letras y números)", description = "Devuelve el conteo exacto de caracteres, letras, dígitos, espacios y si es alfanumérico estricto")
    public ResponseEntity<MetricasTextoDto> analizarTexto(
            @RequestParam(required = false, defaultValue = "texto") String campo,
            @RequestParam String valor) {
        return ResponseEntity.ok(metricasTextoUtil.analizar(campo, valor));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Calcular métricas para múltiples campos en lote")
    public ResponseEntity<Map<String, MetricasTextoDto>> analizarLote(@RequestBody Map<String, String> campos) {
        return ResponseEntity.ok(metricasTextoUtil.analizarCampos(campos));
    }
}
