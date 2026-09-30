package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.ClienteActualizacionRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroResponseDto;

import java.time.LocalDateTime;
import java.util.List;

// Interfaz de servicio para registro, consulta, actualizacion y baja logica de clientes
public interface ClienteService {

    // Registra un cliente persona fisica, su domicilio y crea automaticamente su cuenta bancaria
    ClienteRegistroResponseDto registrarCliente(ClienteRegistroRequestDto requestDto);

    // Consulta todos los clientes registrados
    List<ClienteRegistroResponseDto> listarClientes();

    // Consulta clientes activos
    List<ClienteRegistroResponseDto> listarClientesActivos();

    // Consulta cliente por identificador unico
    ClienteRegistroResponseDto obtenerClientePorId(Long id);

    // Consulta cliente por CURP
    ClienteRegistroResponseDto obtenerClientePorCurp(String curp);

    // Consulta cliente por RFC
    ClienteRegistroResponseDto obtenerClientePorRfc(String rfc);

    // Consulta cliente por correo electronico
    ClienteRegistroResponseDto obtenerClientePorCorreo(String correo);

    // Consulta cliente por numero de cuenta bancaria
    ClienteRegistroResponseDto obtenerClientePorNumeroCuenta(String numeroCuenta);

    // Busqueda por filtros parciales (nombre, apellidos)
    List<ClienteRegistroResponseDto> buscarClientesPorNombre(String nombre);

    List<ClienteRegistroResponseDto> buscarClientesPorApellidoPaterno(String apellidoPaterno);

    List<ClienteRegistroResponseDto> buscarClientesPorApellidoMaterno(String apellidoMaterno);

    // Consulta clientes registrados en un rango de fechas
    List<ClienteRegistroResponseDto> buscarClientesPorRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    // Actualizacion parcial de datos personales, contacto, domicilio y laboral
    ClienteRegistroResponseDto actualizarCliente(Long id, ClienteActualizacionRequestDto requestDto);

    // Baja logica de cliente (desactiva cliente y sus cuentas asociadas sin eliminar registros)
    void desactivarCliente(Long id);

    // Reactivacion de cliente
    ClienteRegistroResponseDto reactivarCliente(Long id);
}
