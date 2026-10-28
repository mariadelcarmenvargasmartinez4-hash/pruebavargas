package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.CuentaBancariaDto;
import com.proyecto.servicios.model.common.GenericResponse;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.model.cuenta.CuentaActualizacionRequestDto;
import com.proyecto.servicios.model.cuenta.CuentaConsultaFiltroRecord;
import com.proyecto.servicios.model.cuenta.CuentaCreacionRequestDto;
import com.proyecto.servicios.model.cuenta.SaldoCuentaDto;
import com.proyecto.servicios.security.SecurityService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.function.Predicate;

// Controlador REST para la gestión de cuentas bancarias
@RestController
@RequestMapping("/cuentas")
@Tag(name = "Cuentas", description = "Operaciones canónicas de creación, consulta unificada paginada, saldo y actualización de cuentas bancarias")
public class CuentaController {

    private final CuentaService cuentaService;
    private final SecurityService securityService;

    // Inyección de dependencias por constructor
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
        requestDto.toRecord(); // Validación temprana Fail-Fast en Record
        Optional.ofNullable(requestDto.getClienteId()).ifPresent(securityService::validarAccesoCliente);
        CuentaBancariaDto cuenta = cuentaService.crearCuenta(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(cuenta);
    }

    // Endpoint unificado de consulta y búsqueda dinámica paginada de cuentas bancarias con Record
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar cuentas con filtros dinámicos y paginación", 
               description = "Búsqueda dinámica paginada de cuentas bancarias mediante Record inmutable. Permite manipular número de página (pack/page) y tamaño de página (size)")
    public ResponseEntity<GenericResponse<PaginaResponseRecord<CuentaBancariaDto>>> consultarCuentas(
            @Parameter(description = "Número de pack de registros solicitado (alias nativo de página)", example = "0") @RequestParam(value = "pack", required = false) Integer pack,
            @Parameter(description = "Número de página/pack (inicia en 0)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página (tamaño)", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenación", example = "id") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Dirección de orden (ASC / DESC)", example = "DESC") @RequestParam(defaultValue = "DESC") String sortDir,
            @Parameter(description = "Número de cuenta bancaria (10 dígitos)") @RequestParam(required = false) String numeroCuenta,
            @Parameter(description = "Identificador del cliente") @RequestParam(required = false) Long clienteId,
            @Parameter(description = "Estatus de la cuenta (ACTIVA, INACTIVA, BLOQUEADA, CANCELADA)") @RequestParam(required = false) String estatus,
            @Parameter(description = "Listar solo cuentas activas (true/false)") @RequestParam(required = false) Boolean soloActivas) {

        Optional.ofNullable(numeroCuenta).filter(Predicate.not(String::isBlank)).ifPresent(securityService::validarAccesoCuenta);
        Optional.ofNullable(clienteId).ifPresent(securityService::validarAccesoCliente);

        CuentaConsultaFiltroRecord filtro = new CuentaConsultaFiltroRecord(
                numeroCuenta, clienteId, estatus, soloActivas, page, size, sortBy, sortDir, pack);

        PaginaResponseRecord<CuentaBancariaDto> pagina = cuentaService.buscarCuentasPaginadas(filtro);
        return ResponseEntity.ok(GenericResponse.exito(pagina, "Consulta paginada de cuentas realizada con éxito"));
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

    // Actualizar información de una cuenta (Ruta canónica PUT)
    @PutMapping(value = "/{numeroCuenta}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar información de una cuenta", description = "Método PUT canónico para actualizar datos de cuenta bancaria. Valida permisos IDOR y reglas de negocio.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuenta actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o intento de activación en cliente inactivo"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado a modificar cuentas de otros clientes"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<CuentaBancariaDto> actualizarCuenta(
            @Parameter(description = "Número de cuenta bancaria a actualizar", example = "4815162342") @PathVariable String numeroCuenta,
            @Valid @RequestBody CuentaActualizacionRequestDto requestDto) {
        requestDto.toRecord(); // Validación temprana Fail-Fast en Record
        securityService.validarAccesoCuenta(numeroCuenta);
        return ResponseEntity.ok(cuentaService.actualizarCuenta(numeroCuenta, requestDto));
    }

    // Baja lógica de cuenta bancaria
    @DeleteMapping(value = "/{numeroCuenta}")
    @Operation(summary = "Baja lógica de una cuenta bancaria", description = "Cancela la cuenta bancaria (estatus CANCELADA) sin eliminar físicamente el registro en base de datos. Valida permisos de autorización IDOR.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cuenta cancelada/dada de baja exitosamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado a cuentas de otros clientes"),
            @ApiResponse(responseCode = "404", description = "Cuenta bancaria no encontrada")
    })
    public ResponseEntity<Void> desactivarCuenta(
            @Parameter(description = "Número de cuenta bancaria a cancelar", example = "4815162342") @PathVariable String numeroCuenta) {
        securityService.validarAccesoCuenta(numeroCuenta);
        cuentaService.desactivarCuenta(numeroCuenta);
        return ResponseEntity.noContent().build();
    }
}
