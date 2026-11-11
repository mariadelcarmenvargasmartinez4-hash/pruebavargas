package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.entity.catalogo.NacionalidadEntity;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.model.catalogo.CatalogoActualizacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoConsultaFiltroRecord;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.repositorys.catalogo.NacionalidadRepository;
import com.proyecto.servicios.service.NacionalidadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NacionalidadServiceImplTest {

    @Mock
    private NacionalidadRepository nacionalidadRepository;

    private NacionalidadService nacionalidadService;

    @BeforeEach
    void setUp() {
        nacionalidadService = new NacionalidadServiceImpl(nacionalidadRepository);
    }

    @Test
    void testNacionalidad_BuscarPaginado() {
        CatalogoConsultaFiltroRecord filtro = new CatalogoConsultaFiltroRecord(true, "MEX", 0, 10, "id", "ASC", 0);
        NacionalidadEntity entity = NacionalidadEntity.builder().id(1L).codigo("MEX").descripcion("MEXICANA").activo(true).build();
        Page<NacionalidadEntity> page = new PageImpl<>(List.of(entity));

        when(nacionalidadRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PaginaResponseRecord<CatalogoItemDto> resultado = nacionalidadService.buscarPaginado(filtro);
        assertEquals(1, resultado.totalElementos());
    }

    @Test
    void testNacionalidad_ListarTodosYActivos() {
        NacionalidadEntity entity = NacionalidadEntity.builder().id(1L).codigo("MEX").descripcion("MEXICANA").activo(true).build();
        when(nacionalidadRepository.findAll()).thenReturn(List.of(entity));
        when(nacionalidadRepository.findByActivoTrue()).thenReturn(List.of(entity));

        assertEquals(1, nacionalidadService.listarTodos().size());
        assertEquals(1, nacionalidadService.listarActivos().size());
    }

    @Test
    void testNacionalidad_ObtenerPorId() {
        NacionalidadEntity entity = NacionalidadEntity.builder().id(1L).codigo("MEX").descripcion("MEXICANA").activo(true).build();
        when(nacionalidadRepository.findById(1L)).thenReturn(Optional.of(entity));

        CatalogoItemDto item = nacionalidadService.obtenerPorId(1L);
        assertEquals("MEX", item.getCodigo());
    }

    @Test
    void testNacionalidad_ObtenerPorId_NoExiste_LanzaExcepcion() {
        when(nacionalidadRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(CatalogoNoEncontradoException.class, () -> nacionalidadService.obtenerPorId(99L));
    }

    @Test
    void testNacionalidad_ObtenerPorCodigoODescripcion() {
        NacionalidadEntity entity = NacionalidadEntity.builder().id(1L).codigo("MEX").descripcion("MEXICANA").activo(true).build();
        when(nacionalidadRepository.findByCodigoIgnoreCase("MEX")).thenReturn(Optional.of(entity));

        CatalogoItemDto item = nacionalidadService.obtenerPorCodigoODescripcion("MEX");
        assertEquals("MEXICANA", item.getDescripcion());
    }

    @Test
    void testNacionalidad_Crear_Exitoso() {
        CatalogoCreacionDto dto = CatalogoCreacionDto.builder().codigo("ESP").descripcion("ESPANYOLA").build();
        when(nacionalidadRepository.existsByCodigoIgnoreCase("ESP")).thenReturn(false);
        when(nacionalidadRepository.existsByDescripcionIgnoreCase("ESPANYOLA")).thenReturn(false);
        when(nacionalidadRepository.save(any(NacionalidadEntity.class))).thenAnswer(i -> {
            NacionalidadEntity n = i.getArgument(0);
            n.setId(2L);
            return n;
        });

        CatalogoItemDto res = nacionalidadService.crear(dto);
        assertEquals("ESP", res.getCodigo());
    }

    @Test
    void testNacionalidad_Actualizar_Exitoso() {
        NacionalidadEntity entity = NacionalidadEntity.builder().id(1L).codigo("MEX").descripcion("VIEJA").activo(true).build();
        when(nacionalidadRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(nacionalidadRepository.existsByDescripcionIgnoreCase("NUEVA")).thenReturn(false);
        when(nacionalidadRepository.save(any(NacionalidadEntity.class))).thenAnswer(i -> i.getArgument(0));

        CatalogoActualizacionDto dto = CatalogoActualizacionDto.builder().descripcion("NUEVA").activo(true).build();
        CatalogoItemDto res = nacionalidadService.actualizar(1L, dto);
        assertEquals("NUEVA", res.getDescripcion());
    }

    @Test
    void testNacionalidad_Desactivar() {
        NacionalidadEntity entity = NacionalidadEntity.builder().id(1L).codigo("MEX").descripcion("MEXICANA").activo(true).build();
        when(nacionalidadRepository.findById(1L)).thenReturn(Optional.of(entity));

        nacionalidadService.desactivar(1L);
        assertFalse(entity.getActivo());
        verify(nacionalidadRepository).save(entity);
    }

    @Test
    void testNacionalidad_EsValidoYActivo() {
        when(nacionalidadRepository.existsByCodigoIgnoreCaseAndActivoTrue("MEX")).thenReturn(true);
        assertTrue(nacionalidadService.esValidoYActivo("MEX"));
        assertFalse(nacionalidadService.esValidoYActivo(""));
        assertFalse(nacionalidadService.esValidoYActivo(null));
    }
}
