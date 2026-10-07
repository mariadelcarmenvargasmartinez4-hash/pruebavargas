package com.proyecto.servicios.service;

import com.proyecto.servicios.model.catalogo.CatalogoActualizacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;

import java.util.List;

public interface NacionalidadService {
    List<CatalogoItemDto> listarTodos();
    List<CatalogoItemDto> listarActivos();
    CatalogoItemDto obtenerPorId(Long id);
    CatalogoItemDto obtenerPorCodigoODescripcion(String valor);
    CatalogoItemDto crear(CatalogoCreacionDto dto);
    CatalogoItemDto actualizar(Long id, CatalogoActualizacionDto dto);
    void desactivar(Long id);
    boolean esValidoYActivo(String valor);
}
