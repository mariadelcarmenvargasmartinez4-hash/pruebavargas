package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.entity.catalogo.EstadoCivilEntity;
import com.proyecto.servicios.exception.CatalogoDuplicadoException;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.model.catalogo.CatalogoActualizacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoConsultaFiltroRecord;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.repositorys.catalogo.EstadoCivilRepository;
import com.proyecto.servicios.service.EstadoCivilService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstadoCivilServiceImplTest {

    @Mock
    private EstadoCivilRepository estadoCivilRepository;

    private EstadoCivilService estadoCivilService;

    @BeforeEach
    void setUp() {
        estadoCivilService = new EstadoCivilServiceImpl(estadoCivilRepository);
    }

    @Test
    void testEstadoCivil_BuscarPaginado() {
        CatalogoConsultaFiltroRecord filtro = new CatalogoConsultaFiltroRecord(true, "SOL", 0, 10, "id", "ASC", 0);
        EstadoCivilEntity entity = EstadoCivilEntity.builder().id(1L).codigo("SOL").descripcion("SOLTERO").activo(true).build();
        Page<EstadoCivilEntity> page = new PageImpl<>(List.of(entity));

        when(estadoCivilRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PaginaResponseRecord<CatalogoItemDto> resultado = estadoCivilService.buscarPaginado(filtro);
        assertEquals(1, resultado.totalElementos());
    }

    @Test
    void testEstadoCivil_ListarTodosYActivos() {
        EstadoCivilEntity entity = EstadoCivilEntity.builder().id(1L).codigo("SOL").descripcion("SOLTERO").activo(true).build();
        when(estadoCivilRepository.findAll()).thenReturn(List.of(entity));
        when(estadoCivilRepository.findByActivoTrue()).thenReturn(List.of(entity));

        assertEquals(1, estadoCivilService.listarTodos().size());
        assertEquals(1, estadoCivilService.listarActivos().size());
    }

    @Test
    void testEstadoCivil_ObtenerPorId() {
        EstadoCivilEntity entity = EstadoCivilEntity.builder().id(1L).codigo("SOL").descripcion("SOLTERO").activo(true).build();
        when(estadoCivilRepository.findById(1L)).thenReturn(Optional.of(entity));

        CatalogoItemDto item = estadoCivilService.obtenerPorId(1L);
        assertEquals("SOL", item.getCodigo());
    }

    @Test
    void testEstadoCivil_ObtenerPorId_NoExiste_LanzaExcepcion() {
        when(estadoCivilRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(CatalogoNoEncontradoException.class, () -> estadoCivilService.obtenerPorId(99L));
    }

    @Test
    void testEstadoCivil_ObtenerPorCodigoODescripcion() {
        EstadoCivilEntity entity = EstadoCivilEntity.builder().id(1L).codigo("SOL").descripcion("SOLTERO").activo(true).build();
        when(estadoCivilRepository.findByCodigoIgnoreCase("SOL")).thenReturn(Optional.of(entity));

        CatalogoItemDto item = estadoCivilService.obtenerPorCodigoODescripcion("SOL");
        assertEquals("SOLTERO", item.getDescripcion());
    }

    @Test
    void testEstadoCivil_Crear_Exitoso() {
        CatalogoCreacionDto dto = CatalogoCreacionDto.builder().codigo("CAS").descripcion("CASADO").build();
        when(estadoCivilRepository.existsByCodigoIgnoreCase("CAS")).thenReturn(false);
        when(estadoCivilRepository.existsByDescripcionIgnoreCase("CASADO")).thenReturn(false);
        when(estadoCivilRepository.save(any(EstadoCivilEntity.class))).thenAnswer(i -> {
            EstadoCivilEntity e = i.getArgument(0);
            e.setId(2L);
            return e;
        });

        CatalogoItemDto res = estadoCivilService.crear(dto);
        assertEquals("CAS", res.getCodigo());
    }

    @Test
    void testEstadoCivil_Actualizar_Exitoso() {
        EstadoCivilEntity entity = EstadoCivilEntity.builder().id(1L).codigo("SOL").descripcion("VIEJA").activo(true).build();
        when(estadoCivilRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(estadoCivilRepository.existsByDescripcionIgnoreCase("NUEVA")).thenReturn(false);
        when(estadoCivilRepository.save(any(EstadoCivilEntity.class))).thenAnswer(i -> i.getArgument(0));

        CatalogoActualizacionDto dto = CatalogoActualizacionDto.builder().descripcion("NUEVA").activo(true).build();
        CatalogoItemDto res = estadoCivilService.actualizar(1L, dto);
        assertEquals("NUEVA", res.getDescripcion());
    }

    @Test
    void testEstadoCivil_Desactivar() {
        EstadoCivilEntity entity = EstadoCivilEntity.builder().id(1L).codigo("SOL").descripcion("SOLTERO").activo(true).build();
        when(estadoCivilRepository.findById(1L)).thenReturn(Optional.of(entity));

        estadoCivilService.desactivar(1L);

        assertFalse(entity.getActivo());
        verify(estadoCivilRepository).save(entity);
    }

    @Test
    void testEstadoCivil_EsValidoYActivo() {
        when(estadoCivilRepository.existsByCodigoIgnoreCaseAndActivoTrue("SOL")).thenReturn(true);
        assertTrue(estadoCivilService.esValidoYActivo("SOL"));
        assertFalse(estadoCivilService.esValidoYActivo(""));
        assertFalse(estadoCivilService.esValidoYActivo(null));
    }
}
