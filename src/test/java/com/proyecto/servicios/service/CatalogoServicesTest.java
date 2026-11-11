package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.catalogo.EstadoCivilEntity;
import com.proyecto.servicios.entity.catalogo.NacionalidadEntity;
import com.proyecto.servicios.entity.catalogo.SexoEntity;
import com.proyecto.servicios.exception.CatalogoDuplicadoException;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.model.catalogo.CatalogoActualizacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoConsultaFiltroRecord;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.repositorys.catalogo.EstadoCivilRepository;
import com.proyecto.servicios.repositorys.catalogo.NacionalidadRepository;
import com.proyecto.servicios.repositorys.catalogo.SexoRepository;
import com.proyecto.servicios.service.impl.EstadoCivilServiceImpl;
import com.proyecto.servicios.service.impl.NacionalidadServiceImpl;
import com.proyecto.servicios.service.impl.SexoServiceImpl;
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
public class CatalogoServicesTest {

    @Mock
    private SexoRepository sexoRepository;

    @Mock
    private NacionalidadRepository nacionalidadRepository;

    @Mock
    private EstadoCivilRepository estadoCivilRepository;

    private SexoService sexoService;
    private NacionalidadService nacionalidadService;
    private EstadoCivilService estadoCivilService;

    @BeforeEach
    void setUp() {
        sexoService = new SexoServiceImpl(sexoRepository);
        nacionalidadService = new NacionalidadServiceImpl(nacionalidadRepository);
        estadoCivilService = new EstadoCivilServiceImpl(estadoCivilRepository);
    }

    // ==========================================
    // PRUEBAS COMPLETAS PARA SEXO SERVICE
    // ==========================================
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

    // ==========================================
    // PRUEBAS COMPLETAS PARA NACIONALIDAD SERVICE
    // ==========================================
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
    }

    @Test
    void testNacionalidad_EsValidoYActivo() {
        when(nacionalidadRepository.existsByCodigoIgnoreCaseAndActivoTrue("MEX")).thenReturn(true);
        assertTrue(nacionalidadService.esValidoYActivo("MEX"));
        assertFalse(nacionalidadService.esValidoYActivo(""));
        assertFalse(nacionalidadService.esValidoYActivo(null));
    }

    // ==========================================
    // PRUEBAS COMPLETAS PARA ESTADO CIVIL SERVICE
    // ==========================================
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
