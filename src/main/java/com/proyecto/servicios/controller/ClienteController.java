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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Controlador REST para registro, consultas especializadas y actualizacion de clientes y cuentas bancarias
@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Operaciones para registro, consulta y actualización de clientes personas físicas y cuentas bancarias asociadas")
public class ClienteController {

    private final ClienteService clienteService;

    // Inyeccion de dependencias por constructor
    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // 2. Creacion automatica de cuenta bancaria y registro de cliente persona fisica
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registrar cliente y crear cuenta bancaria", description = "Captura información del cliente, valida reglas de negocio, crea automáticamente una cuenta bancaria única en estatus ACTIVA y asigna un saldo inicial")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente y cuenta bancaria creados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o cliente menor de 18 años"),
            @ApiResponse(responseCode = "409", description = "CURP, RFC o correo electrónico ya registrado")
    })
    public ResponseEntity<ClienteRegistroResponseDto> registrarCliente(@Valid @RequestBody ClienteRegistroRequestDto requestDto) {
        ClienteRegistroResponseDto respuesta = clienteService.registrarCliente(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // 3. Consulta de informacion: todos los clientes
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar todos los clientes", description = "Obtiene la lista completa de clientes registrados con los datos de su cuenta bancaria")
    public ResponseEntity<List<ClienteRegistroResponseDto>> listarClientes() {
        return ResponseEntity.ok(clienteService.listarClientes());
    }

    // 3. Consulta de informacion: cliente por ID
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar cliente por ID", description = "Obtiene el detalle de un cliente específico y su cuenta mediante su identificador numérico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado con el ID indicado")
    })
    public ResponseEntity<ClienteRegistroResponseDto> obtenerClientePorId(@Parameter(description = "Identificador único del cliente", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerClientePorId(id));
    }

    // 3. Consulta de informacion: cliente por CURP
    @GetMapping(value = "/curp/{curp}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar cliente por CURP", description = "Busca un cliente por su clave CURP de 18 caracteres")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "No se encontró ningún cliente con el CURP proporcionado")
    })
    public ResponseEntity<ClienteRegistroResponseDto> obtenerClientePorCurp(@Parameter(description = "CURP a consultar", example = "PELJ900515HDFRPR09") @PathVariable String curp) {
        return ResponseEntity.ok(clienteService.obtenerClientePorCurp(curp));
    }

    // 3. Consulta de informacion: cliente por RFC
    @GetMapping(value = "/rfc/{rfc}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar cliente por RFC", description = "Busca un cliente por su homoclave RFC de 13 caracteres")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "No se encontró ningún cliente con el RFC proporcionado")
    })
    public ResponseEntity<ClienteRegistroResponseDto> obtenerClientePorRfc(@Parameter(description = "RFC a consultar", example = "PELJ9005151A2") @PathVariable String rfc) {
        return ResponseEntity.ok(clienteService.obtenerClientePorRfc(rfc));
    }

    // 3. Consulta de informacion: cliente por numero de cuenta bancaria
    @GetMapping(value = "/cuenta/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar cliente por número de cuenta", description = "Busca un cliente a través de su número de cuenta bancaria único")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente y cuenta encontrados"),
            @ApiResponse(responseCode = "404", description = "No se encontró ninguna cuenta bancaria con el número indicado")
    })
    public ResponseEntity<ClienteRegistroResponseDto> obtenerClientePorNumeroCuenta(@Parameter(description = "Número de cuenta de 10 dígitos", example = "4815162342") @PathVariable String numeroCuenta) {
        return ResponseEntity.ok(clienteService.obtenerClientePorNumeroCuenta(numeroCuenta));
    }

    // 4. Actualizacion de informacion de cliente
    // Permite modificar datos personales, contacto, domicilio y laboral.
    // No permite modificar CURP, RFC ni numero de cuenta.
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar información de cliente", description = "Permite modificar datos personales, de contacto, domicilio e información laboral. Protege CURP, RFC y número de cuenta impidiendo su alteración.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado con el ID indicado"),
            @ApiResponse(responseCode = "409", description = "El nuevo correo electrónico ya pertenece a otro cliente")
    })
    public ResponseEntity<ClienteRegistroResponseDto> actualizarCliente(
            @Parameter(description = "Identificador único del cliente a actualizar", example = "1") @PathVariable Long id,
            @Valid @RequestBody ClienteActualizacionRequestDto requestDto) {
        ClienteRegistroResponseDto respuesta = clienteService.actualizarCliente(id, requestDto);
        return ResponseEntity.ok(respuesta);
    }
}
