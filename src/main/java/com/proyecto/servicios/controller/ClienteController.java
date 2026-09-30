package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroResponseDto;
import com.proyecto.servicios.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Controlador REST para la gestion de clientes personas fisicas y cuentas bancarias
@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Operaciones para el registro y consulta de clientes personas físicas y cuentas bancarias")
public class ClienteController {

    private final ClienteService clienteService;

    // Inyeccion de dependencias por constructor
    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // Registra un nuevo cliente persona fisica, crea su cuenta bancaria y asigna saldo inicial
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registrar cliente persona física", description = "Captura datos personales, de contacto, domicilio, laborales, valida la información, crea una cuenta bancaria y asigna un saldo inicial")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente y cuenta creados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o cliente menor de edad"),
            @ApiResponse(responseCode = "409", description = "CURP, RFC o correo electrónico ya registrado")
    })
    public ResponseEntity<ClienteRegistroResponseDto> registrarCliente(@Valid @RequestBody ClienteRegistroRequestDto requestDto) {
        ClienteRegistroResponseDto respuesta = clienteService.registrarCliente(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // Consulta la lista de clientes registrados
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar clientes", description = "Obtiene todos los clientes registrados junto con sus cuentas bancarias")
    public ResponseEntity<List<ClienteRegistroResponseDto>> listarClientes() {
        return ResponseEntity.ok(clienteService.listarClientes());
    }

    // Consulta un cliente específico por ID
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Obtener cliente por ID", description = "Obtiene los detalles del cliente y su cuenta bancaria mediante su ID")
    public ResponseEntity<ClienteRegistroResponseDto> obtenerClientePorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerClientePorId(id));
    }
}
