package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.UsuarioAccesoDto;
import com.proyecto.servicios.model.cliente.UsuarioAgregarRequestDto;
import com.proyecto.servicios.model.cliente.UsuarioConsultaFiltroRecord;
import com.proyecto.servicios.model.common.PaginaResponseRecord;

import java.util.List;

/**
 * Servicio desacoplado para la administración y consulta paginada de usuarios de acceso.
 */
public interface UsuarioAccesoService {

    /**
     * Consulta paginada y filtrado dinámico de usuarios con soporte nativo de pack.
     */
    PaginaResponseRecord<UsuarioAccesoDto> buscarUsuariosPaginados(UsuarioConsultaFiltroRecord filtro);

    /**
     * Consulta con soporte para compatibilidad (internamente paginada).
     */
    List<UsuarioAccesoDto> filtrarUsuarios(String correo, Boolean activo);

    /**
     * Agrega un nuevo usuario de acceso para un cliente existente.
     */
    UsuarioAccesoDto agregarUsuario(UsuarioAgregarRequestDto request);
}