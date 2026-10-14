package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.ClienteActualizacionRequestDto;
import com.proyecto.servicios.model.cliente.ClienteConsultaFiltroRecord;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroResponseDto;
import com.proyecto.servicios.model.common.GenericResponse;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.security.SecurityService;
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
@Tag(name = "Clientes", description = "Operaciones de registro, consultas dinámicas paginadas, actualización y baja lógica de clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final SecurityService securityService;

    // Inyeccion de dependencias por constructor
    public ClienteController(ClienteService clienteService, SecurityService securityService) {
        this.clienteService = clienteService;
        this.securityService = securityService;
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

    // Consultar clientes mediante búsqueda dinámica unificada y paginada con Record
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar clientes con filtros dinámicos y paginación", 
               description = "Búsqueda dinámica paginada de clientes. Permite manipular el número de página (pack/page) y el tamaño de página (size)")
    public ResponseEntity<GenericResponse<PaginaResponseRecord<ClienteRegistroResponseDto>>> consultarClientes(
            @Parameter(description = "Número de página/pack (inicia en 0)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página (tamaño)", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenación", example = "id") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Dirección de orden (ASC / DESC)", example = "DESC") @RequestParam(defaultValue = "DESC") String sortDir,
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

        ClienteConsultaFiltroRecord filtro = new ClienteConsultaFiltroRecord(
                nombre, apellidoPaterno, apellidoMaterno, curp, rfc, correo, numeroCuenta, soloActivos, fechaInicio, fechaFin, page, size, sortBy, sortDir);

        PaginaResponseRecord<ClienteRegistroResponseDto> pagina = clienteService.buscarClientesPaginados(filtro);
        return ResponseEntity.ok(GenericResponse.exito(pagina, "Consulta paginada de clientes realizada con éxito"));
    }

    // Consultar un cliente por su identificador (con control de autorización IDOR)
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar un cliente por su identificador", description = "Obtiene los detalles del cliente autenticado. Valida permisos para prevenir acceso a otros usuarios (IDOR).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: sin permisos para ver otro cliente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<ClienteRegistroResponseDto> obtenerClientePorId(
            @Parameter(description = "Identificador del cliente", example = "1") @PathVariable Long id) {
        securityService.validarAccesoCliente(id);
        return ResponseEntity.ok(clienteService.obtenerClientePorId(id));
    }

    // Actualizar parcialmente la información de un cliente (Patch / Put con control de autorización IDOR)
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar parcialmente la información de un cliente", description = "Permite modificar datos de contacto, domicilio e información laboral. Valida permisos de autorización IDOR y estado activo.")
    public ResponseEntity<ClienteRegistroResponseDto> actualizarParcialCliente(
            @Parameter(description = "Identificador del cliente", example = "1") @PathVariable Long id,
            @Valid @RequestBody ClienteActualizacionRequestDto requestDto) {
        securityService.validarAccesoCliente(id);
        return ResponseEntity.ok(clienteService.actualizarCliente(id, requestDto));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar información de un cliente", description = "Método PUT para actualizar datos de un cliente manteniendo CURP y RFC inmutables. Valida autorización IDOR y estado activo.")
    public ResponseEntity<ClienteRegistroResponseDto> actualizarCliente(
            @Parameter(description = "Identificador del cliente", example = "1") @PathVariable Long id,
            @Valid @RequestBody ClienteActualizacionRequestDto requestDto) {
        securityService.validarAccesoCliente(id);
        return ResponseEntity.ok(clienteService.actualizarCliente(id, requestDto));
    }

    // 5. Baja Logica: Desactivar cliente sin eliminar registros fisicos de la base de datos
    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Baja lógica de cliente", description = "Desactiva el cliente y sus cuentas bancarias sin eliminar físicamente el registro en base de datos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cliente desactivado exitosamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<Void> desactivarCliente(@Parameter(description = "Identificador del cliente a desactivar", example = "1") @PathVariable Long id) {
        securityService.validarAccesoCliente(id);
        clienteService.desactivarCliente(id);
        return ResponseEntity.noContent().build();
    }

    // Reactivar cliente previamente dado de baja logica
    @PostMapping(value = "/{id}/reactivar", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reactivar cliente", description = "Vuelve a activar un cliente previamente desactivado")
    public ResponseEntity<ClienteRegistroResponseDto> reactivarCliente(@Parameter(description = "Identificador del cliente a reactivar", example = "1") @PathVariable Long id) {
        securityService.validarAccesoCliente(id);
        return ResponseEntity.ok(clienteService.reactivarCliente(id));
    }
}
