package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.ClienteActualizacionRequestDto;
import com.proyecto.servicios.model.cliente.ClienteConsultaFiltroRecord;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroResponseDto;
import com.proyecto.servicios.model.common.PaginaResponseRecord;

/**
 * Interfaz limpia y desacoplada para gestión de clientes personas físicas.
 * Sin sobrecargas ni métodos redundantes: unifica la consulta mediante paginación dinámica y records.
 */
public interface ClienteService {

    // Registra un cliente persona física, su domicilio y crea automáticamente su cuenta bancaria
    ClienteRegistroResponseDto registrarCliente(ClienteRegistroRequestDto requestDto);

    // Consulta unificada y paginada con filtros opcionales dinámicos mediante Record inmutable
    PaginaResponseRecord<ClienteRegistroResponseDto> buscarClientesPaginados(ClienteConsultaFiltroRecord filtro);

    // Consulta cliente por identificador único
    ClienteRegistroResponseDto obtenerClientePorId(Long id);

    // Consulta cliente por CURP
    ClienteRegistroResponseDto obtenerClientePorCurp(String curp);

    // Actualización de datos de cliente respetando reglas de baja lógica
    ClienteRegistroResponseDto actualizarCliente(Long id, ClienteActualizacionRequestDto requestDto);

    // Baja lógica de cliente
    void desactivarCliente(Long id);

    // Reactivación de cliente
    ClienteRegistroResponseDto reactivarCliente(Long id);
}
