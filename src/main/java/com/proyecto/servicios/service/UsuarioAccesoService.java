package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.UsuarioAccesoDto;
import com.proyecto.servicios.model.cliente.UsuarioAgregarRequestDto;

import java.util.List;

public interface UsuarioAccesoService {
    List<UsuarioAccesoDto> filtrarUsuarios(String correo, Boolean activo);

    UsuarioAccesoDto agregarUsuario(UsuarioAgregarRequestDto request);
}