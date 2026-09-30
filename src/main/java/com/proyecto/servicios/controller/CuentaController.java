package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.CuentaBancariaDto;
import com.proyecto.servicios.model.cuenta.CuentaActualizacionRequestDto;
import com.proyecto.servicios.model.cuenta.CuentaCreacionRequestDto;
import com.proyecto.servicios.model.cuenta.SaldoCuentaDto;
import com.proyecto.servicios.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Controlador REST para la gestion de cuentas bancarias
@RestController
@RequestMapping("/cuentas")
@Tag(name = "Cuentas", description = "Operaciones de creación, consulta por cuenta, cliente o estatus, saldo y actualización parcial de cuentas bancarias")
public class CuentaController {

    private final CuentaService cuentaService;

    // Inyeccion de dependencias por constructor
    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    // Crear una cuenta asociada a un cliente
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear una cuenta asociada a un cliente", description = "Genera una nueva cuenta bancaria única en estatus ACTIVA asociada a un cliente existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cuenta creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Regla de negocio no satisfecha o saldo inicial inválido"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<CuentaBancariaDto> crearCuenta(@Valid @RequestBody CuentaCreacionRequestDto requestDto) {
        CuentaBancariaDto cuenta = cuentaService.crearCuenta(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(cuenta);
    }

    // Consultar una cuenta por número de cuenta
    @GetMapping(value = "/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar una cuenta por número de cuenta", description = "Obtiene los detalles de la cuenta bancaria especificada")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuenta encontrada"),
            @ApiResponse(responseCode = "404", description = "Cuenta bancaria no encontrada")
    })
    public ResponseEntity<CuentaBancariaDto> obtenerCuentaPorNumero(
            @Parameter(description = "Número de cuenta bancaria de 10 dígitos", example = "4815162342") @PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerCuentaPorNumero(numeroCuenta));
    }

    // Consultar las cuentas asociadas a un cliente (/cuentas?clienteId={clienteId})
    // o buscar cuentas por estatus (/cuentas?estatus={estatus})
    // o consultar cuentas activas (/cuentas?activas=true)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar cuentas con filtros opcionales", description = "Permite consultar cuentas por clienteId, por estatus (por ejemplo ACTIVA), o listar todas las cuentas activas")
    public ResponseEntity<List<CuentaBancariaDto>> consultarCuentas(
            @Parameter(description = "Identificador del cliente") @RequestParam(required = false) Long clienteId,
            @Parameter(description = "Estatus de la cuenta (ACTIVA, INACTIVA, BLOQUEADA)") @RequestParam(required = false) String estatus,
            @Parameter(description = "Listar solo cuentas activas") @RequestParam(required = false) Boolean soloActivas) {

        if (clienteId != null) {
            return ResponseEntity.ok(cuentaService.obtenerCuentasPorClienteId(clienteId));
        }
        if (estatus != null && !estatus.isBlank()) {
            return ResponseEntity.ok(cuentaService.obtenerCuentasPorEstatus(estatus));
        }
        if (Boolean.TRUE.equals(soloActivas)) {
            return ResponseEntity.ok(cuentaService.obtenerCuentasActivas());
        }

        return ResponseEntity.ok(cuentaService.obtenerCuentasActivas());
    }

    // Consultar saldo de una cuenta (/cuentas/{numeroCuenta}/saldo)
    @GetMapping(value = "/{numeroCuenta}/saldo", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar saldo de una cuenta", description = "Obtiene el saldo disponible actual, moneda, titular y estatus de la cuenta bancaria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saldo consultado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<SaldoCuentaDto> obtenerSaldoCuenta(
            @Parameter(description = "Número de cuenta bancaria", example = "4815162342") @PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerSaldoCuenta(numeroCuenta));
    }

    // Actualizar parcialmente la información de una cuenta (/cuentas/{numeroCuenta})
    @PatchMapping(value = "/{numeroCuenta}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar parcialmente la información de una cuenta", description = "Permite actualizar saldo, tipo de cuenta o estatus sin modificar el número de cuenta")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuenta actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o intento de activación en cliente inactivo"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<CuentaBancariaDto> actualizarParcialCuenta(
            @Parameter(description = "Número de cuenta bancaria a actualizar", example = "4815162342") @PathVariable String numeroCuenta,
            @Valid @RequestBody CuentaActualizacionRequestDto requestDto) {
        return ResponseEntity.ok(cuentaService.actualizarCuenta(numeroCuenta, requestDto));
    }

    @PutMapping(value = "/{numeroCuenta}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar información de una cuenta (PUT)", description = "Método PUT para actualizar datos de cuenta bancaria")
    public ResponseEntity<CuentaBancariaDto> actualizarCuenta(
            @Parameter(description = "Número de cuenta bancaria a actualizar", example = "4815162342") @PathVariable String numeroCuenta,
            @Valid @RequestBody CuentaActualizacionRequestDto requestDto) {
        return ResponseEntity.ok(cuentaService.actualizarCuenta(numeroCuenta, requestDto));
    }
}
