package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.common.GenericResponse;
import com.proyecto.servicios.model.pago.PagoConciliacionRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioResponseDto;
import com.proyecto.servicios.service.PagoServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la recepción, procesamiento y conciliación de pagos de servicios y recargas telefónicas.
 * Cumple con los estándares Senior de diseño:
 * - Ruta canónica estandarizada: /pagos
 * - Toda la información viaja exclusivamente en el cuerpo de la petición (JSON) mediante DTOs.
 * - Sin uso de @PathVariable ni @RequestParam.
 * - Validación temprana con Bean Validation (@Valid) y Records Fail-Fast.
 * - Manejo centralizado de excepciones y respuestas uniformes con GenericResponse.
 */
@RestController
@RequestMapping("/pagos")
@Tag(name = "Pagos de Servicios", description = "Servicio para la recepción, aplicación y conciliación de pagos de servicios y recargas telefónicas")
public class PagoServicioController {

    private final PagoServicioService pagoServicioService;

    public PagoServicioController(PagoServicioService pagoServicioService) {
        this.pagoServicioService = pagoServicioService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Información del servicio de pagos", description = "Describe los requerimientos del payload JSON para procesar un pago.")
    public ResponseEntity<GenericResponse<String>> informacionServicio() {
        return ResponseEntity.ok(GenericResponse.exito(
                "Para realizar un pago de servicio o recarga, envíe una petición POST a /pagos con el cuerpo JSON correspondiente. Visite /swagger-ui/index.html para probar interactivamente.",
                "API de Pagos de Servicios activa"));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Paso 1: Recibir y procesar petición de pago de servicios o recargas",
               description = "Recibe los datos de la cuenta, servicio, UPE de 10 dígitos, parámetros de pago condicionales (teléfono si bandera=true, referencia si bandera=false) y PIN de autenticación.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pago procesado y aplicado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos, error en UPE, o parámetros condicionales no satisfechos"),
            @ApiResponse(responseCode = "404", description = "Cuenta bancaria o producto no encontrado"),
            @ApiResponse(responseCode = "502", description = "Fallo de comunicación o tiempo de espera con GestoPago")
    })
    public ResponseEntity<GenericResponse<PagoServicioResponseDto>> procesarPago(
            @Valid @RequestBody PagoServicioRequestDto request) {
        PagoServicioResponseDto respuesta = pagoServicioService.procesarPago(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponse.exito(respuesta, "Pago de servicio recibido y procesado exitosamente"));
    }

    @PostMapping(value = "/conciliar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar o conciliar estado de una transacción previa",
               description = "Permite verificar el estado de una transacción financiera mediante su UPE sin arriesgar cargos duplicados ni reintentos ciegos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consulta o conciliación procesada exitosamente"),
            @ApiResponse(responseCode = "400", description = "UPE no registrada o formato inválido")
    })
    public ResponseEntity<GenericResponse<PagoServicioResponseDto>> conciliarPago(
            @Valid @RequestBody PagoConciliacionRequestDto request) {
        PagoServicioResponseDto respuesta = pagoServicioService.consultarOConciliarPago(request);
        return ResponseEntity.ok(GenericResponse.exito(respuesta, "Consulta de conciliación procesada exitosamente"));
    }
}
