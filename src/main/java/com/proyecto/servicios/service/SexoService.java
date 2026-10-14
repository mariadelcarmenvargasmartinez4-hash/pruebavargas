package com.proyecto.servicios.service;

import com.proyecto.servicios.model.catalogo.CatalogoActualizacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoConsultaFiltroRecord;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.model.common.PaginaResponseRecord;

import java.util.List;

public interface SexoService {

    PaginaResponseRecord<CatalogoItemDto> buscarPaginado(CatalogoConsultaFiltroRecord filtro);

    List<CatalogoItemDto> listarTodos();

    List<CatalogoItemDto> listarActivos();

    CatalogoItemDto obtenerPorId(Long id);

    CatalogoItemDto obtenerPorCodigoODescripcion(String valor);

    CatalogoItemDto crear(CatalogoCreacionDto dto);

    CatalogoItemDto actualizar(Long id, CatalogoActualizacionDto dto);

    void desactivar(Long id);

    boolean esValidoYActivo(String valor);
}
