package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroResponseDto;

import java.util.List;

// Interfaz de servicio para registro y gestion de clientes
public interface ClienteService {

    // Registra un cliente persona fisica y genera su cuenta bancaria con saldo inicial
    ClienteRegistroResponseDto registrarCliente(ClienteRegistroRequestDto requestDto);

    // Consulta los clientes registrados
    List<ClienteRegistroResponseDto> listarClientes();

    // Consulta un cliente por ID
    ClienteRegistroResponseDto obtenerClientePorId(Long id);
}
