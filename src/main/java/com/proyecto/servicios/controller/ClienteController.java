package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.ClienteActualizacionRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroResponseDto;
import com.proyecto.servicios.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

// Controlador REST para la gestion de clientes personas fisicas
@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Operaciones de registro, consultas con filtros, actualización parcial y baja lógica de clientes")
public class ClienteController {

    private final ClienteService clienteService;

    // Inyeccion de dependencias por constructor
    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // Registrar un nuevo cliente persona fisica
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registrar un nuevo cliente", description = "Captura información personal, de contacto, domicilio y laboral, valida reglas de negocio y crea automáticamente su cuenta bancaria con saldo inicial")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente y cuenta creados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error de validación o cliente menor de 18 años"),
            @ApiResponse(responseCode = "409", description = "CURP, RFC o correo electrónico duplicado")
    })
    public ResponseEntity<ClienteRegistroResponseDto> registrarCliente(@Valid @RequestBody ClienteRegistroRequestDto requestDto) {
        ClienteRegistroResponseDto respuesta = clienteService.registrarCliente(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // Consultar todos los clientes o filtrar por nombre, apellidoPaterno, apellidoMaterno, curp, rfc o estatus activo
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar clientes con filtros opcionales", description = "Permite consultar todos los clientes o aplicar filtros por nombre, apellido paterno, apellido materno, curp, rfc, correo o estatus activo")
    public ResponseEntity<List<ClienteRegistroResponseDto>> consultarClientes(
            @Parameter(description = "Filtrar por nombre") @RequestParam(required = false) String nombre,
            @Parameter(description = "Filtrar por apellido paterno") @RequestParam(required = false) String apellidoPaterno,
            @Parameter(description = "Filtrar por apellido materno") @RequestParam(required = false) String apellidoMaterno,
            @Parameter(description = "Buscar cliente por CURP") @RequestParam(required = false) String curp,
            @Parameter(description = "Buscar cliente por RFC") @RequestParam(required = false) String rfc,
            @Parameter(description = "Buscar cliente por correo electrónico") @RequestParam(required = false) String correo,
            @Parameter(description = "Buscar cliente por número de cuenta bancaria") @RequestParam(required = false) String numeroCuenta,
            @Parameter(description = "Consultar solo clientes activos (true/false)") @RequestParam(required = false) Boolean soloActivos,
            @Parameter(description = "Fecha inicio para rango de registro (AAAA-MM-DDTHH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @Parameter(description = "Fecha fin para rango de registro (AAAA-MM-DDTHH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {

        if (numeroCuenta != null && !numeroCuenta.isBlank()) {
            return ResponseEntity.ok(List.of(clienteService.obtenerClientePorNumeroCuenta(numeroCuenta)));
        }
        if (curp != null && !curp.isBlank()) {
            return ResponseEntity.ok(List.of(clienteService.obtenerClientePorCurp(curp)));
        }
        if (rfc != null && !rfc.isBlank()) {
            return ResponseEntity.ok(List.of(clienteService.obtenerClientePorRfc(rfc)));
        }
        if (correo != null && !correo.isBlank()) {
            return ResponseEntity.ok(List.of(clienteService.obtenerClientePorCorreo(correo)));
        }
        if (nombre != null && !nombre.isBlank()) {
            return ResponseEntity.ok(clienteService.buscarClientesPorNombre(nombre));
        }
        if (apellidoPaterno != null && !apellidoPaterno.isBlank()) {
            return ResponseEntity.ok(clienteService.buscarClientesPorApellidoPaterno(apellidoPaterno));
        }
        if (apellidoMaterno != null && !apellidoMaterno.isBlank()) {
            return ResponseEntity.ok(clienteService.buscarClientesPorApellidoMaterno(apellidoMaterno));
        }
        if (fechaInicio != null && fechaFin != null) {
            return ResponseEntity.ok(clienteService.buscarClientesPorRangoFechas(fechaInicio, fechaFin));
        }
        if (Boolean.TRUE.equals(soloActivos)) {
            return ResponseEntity.ok(clienteService.listarClientesActivos());
        }

        return ResponseEntity.ok(clienteService.listarClientes());
    }

    // Consultar un cliente por su identificador
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar un cliente por su identificador", description = "Obtiene los detalles del cliente, su domicilio y sus cuentas asociadas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<ClienteRegistroResponseDto> obtenerClientePorId(@Parameter(description = "Identificador del cliente", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerClientePorId(id));
    }

    // Actualizar parcialmente la información de un cliente (Patch / Put)
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar parcialmente la información de un cliente", description = "Permite modificar datos personales, de contacto, domicilio e información laboral. No permite modificar CURP ni RFC.")
    public ResponseEntity<ClienteRegistroResponseDto> actualizarParcialCliente(
            @Parameter(description = "Identificador del cliente", example = "1") @PathVariable Long id,
            @Valid @RequestBody ClienteActualizacionRequestDto requestDto) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, requestDto));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar información de un cliente", description = "Método PUT para actualizar datos de un cliente manteniendo CURP y RFC inmutables")
    public ResponseEntity<ClienteRegistroResponseDto> actualizarCliente(
            @Parameter(description = "Identificador del cliente", example = "1") @PathVariable Long id,
            @Valid @RequestBody ClienteActualizacionRequestDto requestDto) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, requestDto));
    }

    // 5. Baja Logica: Desactivar cliente sin eliminar registros fisicos de la base de datos
    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Baja lógica de cliente", description = "Desactiva el cliente y sus cuentas bancarias sin eliminar físicamente el registro en base de datos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cliente desactivado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<Void> desactivarCliente(@Parameter(description = "Identificador del cliente a desactivar", example = "1") @PathVariable Long id) {
        clienteService.desactivarCliente(id);
        return ResponseEntity.noContent().build();
    }

    // Reactivar cliente previamente dado de baja logica
    @PostMapping(value = "/{id}/reactivar", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reactivar cliente", description = "Vuelve a activar un cliente previamente desactivado")
    public ResponseEntity<ClienteRegistroResponseDto> reactivarCliente(@Parameter(description = "Identificador del cliente a reactivar", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.reactivarCliente(id));
    }
}
