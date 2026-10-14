package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.CuentaBancariaDto;
import com.proyecto.servicios.model.cuenta.CuentaActualizacionRequestDto;
import com.proyecto.servicios.model.cuenta.CuentaCreacionRequestDto;
import com.proyecto.servicios.model.cuenta.SaldoCuentaDto;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.security.SecurityService;
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
@Tag(name = "Cuentas", description = "Operaciones de creación, consulta unificada con filtros dinámicos, saldo y actualización de cuentas bancarias")
public class CuentaController {

    private final CuentaService cuentaService;
    private final SecurityService securityService;

    // Inyeccion de dependencias por constructor
    public CuentaController(CuentaService cuentaService, SecurityService securityService) {
        this.cuentaService = cuentaService;
        this.securityService = securityService;
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
        if (requestDto.getClienteId() != null) {
            securityService.validarAccesoCliente(requestDto.getClienteId());
        }
        CuentaBancariaDto cuenta = cuentaService.crearCuenta(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(cuenta);
    }

    // Endpoint unificado de consulta y búsqueda dinámica de cuentas bancarias
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar cuentas con filtros dinámicos opcionales", 
               description = "Endpoint unificado para realizar búsquedas dinámicas de cuentas bancarias por número de cuenta, clienteId, estatus o solo cuentas activas evitando duplicidad de endpoints")
    public ResponseEntity<List<CuentaBancariaDto>> consultarCuentas(
            @Parameter(description = "Número de cuenta bancaria (10 dígitos)") @RequestParam(required = false) String numeroCuenta,
            @Parameter(description = "Identificador del cliente") @RequestParam(required = false) Long clienteId,
            @Parameter(description = "Estatus de la cuenta (ACTIVA, INACTIVA, BLOQUEADA, CANCELADA)") @RequestParam(required = false) String estatus,
            @Parameter(description = "Listar solo cuentas activas (true/false)") @RequestParam(required = false) Boolean soloActivas) {

        if (numeroCuenta != null && !numeroCuenta.isBlank()) {
            securityService.validarAccesoCuenta(numeroCuenta);
        }
        if (clienteId != null) {
            securityService.validarAccesoCliente(clienteId);
        }

        List<CuentaBancariaDto> cuentas = cuentaService.buscarCuentasDinamicas(numeroCuenta, clienteId, estatus, soloActivas);
        return ResponseEntity.ok(cuentas);
    }

    // Consultar una cuenta por número de cuenta (con control de autorización IDOR)
    @GetMapping(value = "/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar una cuenta por número de cuenta", description = "Obtiene los detalles de la cuenta bancaria especificada previa validación de autorización")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuenta encontrada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado a cuentas de otros clientes"),
            @ApiResponse(responseCode = "404", description = "Cuenta bancaria no encontrada")
    })
    public ResponseEntity<CuentaBancariaDto> obtenerCuentaPorNumero(
            @Parameter(description = "Número de cuenta bancaria de 10 dígitos", example = "4815162342") @PathVariable String numeroCuenta) {
        securityService.validarAccesoCuenta(numeroCuenta);
        return ResponseEntity.ok(cuentaService.obtenerCuentaPorNumero(numeroCuenta));
    }

    // Consultar saldo de una cuenta (/cuentas/{numeroCuenta}/saldo con control de autorización IDOR)
    @GetMapping(value = "/{numeroCuenta}/saldo", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar saldo de una cuenta", description = "Obtiene el saldo disponible actual, moneda, titular y estatus de la cuenta bancaria previa validación de permisos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saldo consultado exitosamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado al saldo de cuentas ajenas"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<SaldoCuentaDto> obtenerSaldoCuenta(
            @Parameter(description = "Número de cuenta bancaria", example = "4815162342") @PathVariable String numeroCuenta) {
        securityService.validarAccesoCuenta(numeroCuenta);
        return ResponseEntity.ok(cuentaService.obtenerSaldoCuenta(numeroCuenta));
    }

    // Actualizar parcialmente la información de una cuenta (/cuentas/{numeroCuenta})
    @PatchMapping(value = "/{numeroCuenta}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar parcialmente la información de una cuenta", description = "Permite actualizar saldo, tipo de cuenta o estatus sin modificar el número de cuenta. Valida autorización IDOR y reglas de baja lógica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuenta actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o intento de activación en cliente inactivo"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado a modificar cuentas de otros clientes"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<CuentaBancariaDto> actualizarParcialCuenta(
            @Parameter(description = "Número de cuenta bancaria a actualizar", example = "4815162342") @PathVariable String numeroCuenta,
            @Valid @RequestBody CuentaActualizacionRequestDto requestDto) {
        securityService.validarAccesoCuenta(numeroCuenta);
        return ResponseEntity.ok(cuentaService.actualizarCuenta(numeroCuenta, requestDto));
    }

    @PutMapping(value = "/{numeroCuenta}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar información de una cuenta (PUT)", description = "Método PUT para actualizar datos de cuenta bancaria. Valida permisos y reglas de negocio.")
    public ResponseEntity<CuentaBancariaDto> actualizarCuenta(
            @Parameter(description = "Número de cuenta bancaria a actualizar", example = "4815162342") @PathVariable String numeroCuenta,
            @Valid @RequestBody CuentaActualizacionRequestDto requestDto) {
        securityService.validarAccesoCuenta(numeroCuenta);
        return ResponseEntity.ok(cuentaService.actualizarCuenta(numeroCuenta, requestDto));
    }
}
