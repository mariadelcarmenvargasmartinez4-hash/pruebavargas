package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.entity.catalogo.SexoEntity;
import com.proyecto.servicios.exception.CatalogoDuplicadoException;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.model.catalogo.CatalogoActualizacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoConsultaFiltroRecord;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.repositorys.catalogo.SexoRepository;
import com.proyecto.servicios.service.SexoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SexoServiceImplTest {

    @Mock
    private SexoRepository sexoRepository;

    private SexoService sexoService;

    @BeforeEach
    void setUp() {
        sexoService = new SexoServiceImpl(sexoRepository);
    }

    @Test
    void testSexo_BuscarPaginado() {
        CatalogoConsultaFiltroRecord filtro = new CatalogoConsultaFiltroRecord(true, "MASC", 0, 10, "id", "ASC", 0);
        SexoEntity entity = SexoEntity.builder().id(1L).codigo("MASC").descripcion("MASCULINO").activo(true).build();
        Page<SexoEntity> page = new PageImpl<>(List.of(entity));

        when(sexoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PaginaResponseRecord<CatalogoItemDto> resultado = sexoService.buscarPaginado(filtro);

        assertNotNull(resultado);
        assertEquals(1, resultado.totalElementos());
    }

    @Test
    void testSexo_ListarTodos() {
        SexoEntity entity = SexoEntity.builder().id(1L).codigo("MASC").descripcion("MASCULINO").activo(true).build();
        when(sexoRepository.findAll()).thenReturn(List.of(entity));

        List<CatalogoItemDto> res = sexoService.listarTodos();
        assertEquals(1, res.size());
    }

    @Test
    void testSexo_ListarActivos() {
        SexoEntity entity = SexoEntity.builder().id(1L).codigo("MASC").descripcion("MASCULINO").activo(true).fechaCreacion(LocalDateTime.now()).build();
        when(sexoRepository.findByActivoTrue()).thenReturn(List.of(entity));

        List<CatalogoItemDto> resultado = sexoService.listarActivos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("MASC", resultado.get(0).getCodigo());
        assertEquals("MASCULINO", resultado.get(0).getDescripcion());
    }

    @Test
    void testSexo_ObtenerPorId_Exitoso() {
        SexoEntity entity = SexoEntity.builder().id(1L).codigo("MASC").descripcion("MASCULINO").activo(true).build();
        when(sexoRepository.findById(1L)).thenReturn(Optional.of(entity));

        CatalogoItemDto item = sexoService.obtenerPorId(1L);
        assertEquals("MASC", item.getCodigo());
    }

    @Test
    void testSexo_ObtenerPorId_NoExiste_LanzaExcepcion() {
        when(sexoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(CatalogoNoEncontradoException.class, () -> sexoService.obtenerPorId(99L));
    }

    @Test
    void testSexo_ObtenerPorCodigoODescripcion_PorCodigo() {
        SexoEntity entity = SexoEntity.builder().id(1L).codigo("MASC").descripcion("MASCULINO").activo(true).build();
        when(sexoRepository.findByCodigoIgnoreCase("MASC")).thenReturn(Optional.of(entity));

        CatalogoItemDto item = sexoService.obtenerPorCodigoODescripcion("MASC");
        assertEquals("MASCULINO", item.getDescripcion());
    }

    @Test
    void testSexo_ObtenerPorCodigoODescripcion_PorDescripcion() {
        SexoEntity entity = SexoEntity.builder().id(1L).codigo("MASC").descripcion("MASCULINO").activo(true).build();
        when(sexoRepository.findByCodigoIgnoreCase("MASCULINO")).thenReturn(Optional.empty());
        when(sexoRepository.findByDescripcionIgnoreCase("MASCULINO")).thenReturn(Optional.of(entity));

        CatalogoItemDto item = sexoService.obtenerPorCodigoODescripcion("MASCULINO");
        assertEquals("MASC", item.getCodigo());
    }

    @Test
    void testSexo_ObtenerPorCodigoODescripcion_NoExiste_LanzaExcepcion() {
        when(sexoRepository.findByCodigoIgnoreCase("NO_EXISTE")).thenReturn(Optional.empty());
        when(sexoRepository.findByDescripcionIgnoreCase("NO_EXISTE")).thenReturn(Optional.empty());

        assertThrows(CatalogoNoEncontradoException.class, () -> sexoService.obtenerPorCodigoODescripcion("NO_EXISTE"));
    }

    @Test
    void testSexo_Crear_Exitoso() {
        CatalogoCreacionDto dto = CatalogoCreacionDto.builder().codigo("OTRO").descripcion("OTRO GENERO").build();
        when(sexoRepository.existsByCodigoIgnoreCase(anyString())).thenReturn(false);
        when(sexoRepository.existsByDescripcionIgnoreCase(anyString())).thenReturn(false);
        when(sexoRepository.save(any(SexoEntity.class))).thenAnswer(inv -> {
            SexoEntity s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        CatalogoItemDto creado = sexoService.crear(dto);

        assertNotNull(creado);
        assertEquals("OTRO", creado.getCodigo());
        assertEquals("OTRO GENERO", creado.getDescripcion());
        verify(sexoRepository).save(any(SexoEntity.class));
    }

    @Test
    void testSexo_Crear_CodigoDuplicado_LanzaExcepcion() {
        CatalogoCreacionDto dto = CatalogoCreacionDto.builder().codigo("MASC").descripcion("MASCULINO").build();
        when(sexoRepository.existsByCodigoIgnoreCase("MASC")).thenReturn(true);

        assertThrows(CatalogoDuplicadoException.class, () -> sexoService.crear(dto));
    }

    @Test
    void testSexo_Crear_DescripcionDuplicada_LanzaExcepcion() {
        CatalogoCreacionDto dto = CatalogoCreacionDto.builder().codigo("NEW").descripcion("MASCULINO").build();
        when(sexoRepository.existsByCodigoIgnoreCase("NEW")).thenReturn(false);
        when(sexoRepository.existsByDescripcionIgnoreCase("MASCULINO")).thenReturn(true);

        assertThrows(CatalogoDuplicadoException.class, () -> sexoService.crear(dto));
    }

    @Test
    void testSexo_Actualizar_Exitoso() {
        SexoEntity entity = SexoEntity.builder().id(1L).codigo("MASC").descripcion("VIEJA").activo(true).build();
        when(sexoRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sexoRepository.existsByDescripcionIgnoreCase("NUEVA")).thenReturn(false);
        when(sexoRepository.save(any(SexoEntity.class))).thenAnswer(i -> i.getArgument(0));

        CatalogoActualizacionDto dto = CatalogoActualizacionDto.builder().descripcion("NUEVA").activo(true).build();
        CatalogoItemDto res = sexoService.actualizar(1L, dto);

        assertEquals("NUEVA", res.getDescripcion());
    }

    @Test
    void testSexo_Actualizar_DescripcionDuplicada_LanzaExcepcion() {
        SexoEntity entity = SexoEntity.builder().id(1L).codigo("MASC").descripcion("MASCULINO").activo(true).build();
        when(sexoRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sexoRepository.existsByDescripcionIgnoreCase("FEMENINO")).thenReturn(true);

        CatalogoActualizacionDto dto = CatalogoActualizacionDto.builder().descripcion("FEMENINO").build();
        assertThrows(CatalogoDuplicadoException.class, () -> sexoService.actualizar(1L, dto));
    }

    @Test
    void testSexo_Desactivar() {
        SexoEntity entity = SexoEntity.builder().id(1L).codigo("MASC").descripcion("MASCULINO").activo(true).build();
        when(sexoRepository.findById(1L)).thenReturn(Optional.of(entity));

        sexoService.desactivar(1L);

        assertFalse(entity.getActivo());
        verify(sexoRepository).save(entity);
    }

    @Test
    void testSexo_EsValidoYActivo() {
        when(sexoRepository.existsByCodigoIgnoreCaseAndActivoTrue("MASC")).thenReturn(true);
        assertTrue(sexoService.esValidoYActivo("MASC"));
        assertFalse(sexoService.esValidoYActivo(""));
        assertFalse(sexoService.esValidoYActivo(null));
    }
}
