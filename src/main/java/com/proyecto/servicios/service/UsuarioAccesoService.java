package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.UsuarioAccesoDto;
import com.proyecto.servicios.model.cliente.UsuarioAgregarRequestDto;
import com.proyecto.servicios.model.cliente.UsuarioConsultaFiltroRecord;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.model.usuario.UsuarioActualizacionRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioBusquedaRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioCambioPasswordRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioCreacionRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioDetalleRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioEstadoRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioResponseDto;

import java.util.List;

/**
 * Servicio de administración de usuarios desacoplado y siguiendo las mejores prácticas de arquitectura limpia.
 */
public interface UsuarioAccesoService {

    /**
     * Registra un nuevo usuario con validaciones de negocio y seguridad.
     */
    UsuarioResponseDto crearUsuario(UsuarioCreacionRequestDto request);

    /**
     * Búsqueda dinámica unificada y paginada con combinación de filtros opcionales.
     */
    PaginaResponseRecord<UsuarioResponseDto> buscarUsuariosDinamico(UsuarioBusquedaRequestDto filtro);

    /**
     * Obtiene el detalle de un usuario por su identificador o username.
     */
    UsuarioResponseDto obtenerDetalleUsuario(UsuarioDetalleRequestDto request);

    /**
     * Actualiza la información permitida de un usuario protegiendo campos del servidor.
     */
    UsuarioResponseDto actualizarUsuario(UsuarioActualizacionRequestDto request);

    /**
     * Actualiza la contraseña del usuario de forma segura.
     */
    void cambiarPassword(UsuarioCambioPasswordRequestDto request);

    /**
     * Realiza la baja lógica (desactivación) del usuario.
     */
    UsuarioResponseDto desactivarUsuario(UsuarioEstadoRequestDto request);

    /**
     * Reactiva un usuario previamente desactivado.
     */
    UsuarioResponseDto reactivarUsuario(UsuarioEstadoRequestDto request);

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