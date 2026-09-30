package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.ClienteActualizacionRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroResponseDto;

import java.util.List;

// Interfaz de servicio para registro, consulta y actualizacion de clientes y cuentas bancarias
public interface ClienteService {

    // Registra un cliente persona fisica y genera automaticamente su cuenta bancaria con saldo inicial
    ClienteRegistroResponseDto registrarCliente(ClienteRegistroRequestDto requestDto);

    // Consulta todos los clientes registrados con su informacion de cuenta
    List<ClienteRegistroResponseDto> listarClientes();

    // Consulta cliente por identificador unico
    ClienteRegistroResponseDto obtenerClientePorId(Long id);

    // Consulta cliente por clave unica de registro de poblacion (CURP)
    ClienteRegistroResponseDto obtenerClientePorCurp(String curp);

    // Consulta cliente por registro federal de contribuyentes (RFC)
    ClienteRegistroResponseDto obtenerClientePorRfc(String rfc);

    // Consulta cliente a traves del numero de cuenta bancaria
    ClienteRegistroResponseDto obtenerClientePorNumeroCuenta(String numeroCuenta);

    // Actualiza datos personales, contacto, domicilio y laborales (sin modificar CURP, RFC ni numero de cuenta)
    ClienteRegistroResponseDto actualizarCliente(Long id, ClienteActualizacionRequestDto requestDto);
}
