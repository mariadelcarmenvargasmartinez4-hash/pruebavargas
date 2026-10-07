package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.catalogo.EstadoCivilEntity;
import com.proyecto.servicios.entity.catalogo.NacionalidadEntity;
import com.proyecto.servicios.entity.catalogo.SexoEntity;
import com.proyecto.servicios.exception.CatalogoDuplicadoException;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.repositorys.catalogo.EstadoCivilRepository;
import com.proyecto.servicios.repositorys.catalogo.NacionalidadRepository;
import com.proyecto.servicios.repositorys.catalogo.SexoRepository;
import com.proyecto.servicios.service.Impl.EstadoCivilServiceImpl;
import com.proyecto.servicios.service.Impl.NacionalidadServiceImpl;
import com.proyecto.servicios.service.Impl.SexoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    void testSexo_ListarActivos() {
        SexoEntity entity = SexoEntity.builder()
                .id(1L)
                .codigo("MASC")
                .descripcion("MASCULINO")
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();
        when(sexoRepository.findByActivoTrue()).thenReturn(List.of(entity));

        List<CatalogoItemDto> resultado = sexoService.listarActivos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("MASC", resultado.get(0).getCodigo());
        assertEquals("MASCULINO", resultado.get(0).getDescripcion());
    }

    @Test
    void testSexo_Crear_Exitoso() {
        CatalogoCreacionDto dto = CatalogoCreacionDto.builder()
                .codigo("OTRO")
                .descripcion("OTRO GENERO")
                .build();
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
    void testSexo_Crear_Duplicado_LanzaExcepcion() {
        CatalogoCreacionDto dto = CatalogoCreacionDto.builder()
                .codigo("MASC")
                .descripcion("MASCULINO")
                .build();
        when(sexoRepository.existsByCodigoIgnoreCase("MASC")).thenReturn(true);

        assertThrows(CatalogoDuplicadoException.class, () -> sexoService.crear(dto));
    }

    @Test
    void testNacionalidad_EsValidoYActivo() {
        when(nacionalidadRepository.existsByCodigoIgnoreCaseAndActivoTrue("MEX")).thenReturn(true);

        assertTrue(nacionalidadService.esValidoYActivo("MEX"));
        assertFalse(nacionalidadService.esValidoYActivo(""));
        assertFalse(nacionalidadService.esValidoYActivo(null));
    }

    @Test
    void testEstadoCivil_ObtenerPorId_NoExiste_LanzaExcepcion() {
        when(estadoCivilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CatalogoNoEncontradoException.class, () -> estadoCivilService.obtenerPorId(99L));
    }

    @Test
    void testEstadoCivil_Desactivar() {
        EstadoCivilEntity entity = EstadoCivilEntity.builder()
                .id(1L)
                .codigo("SOL")
                .descripcion("SOLTERO")
                .activo(true)
                .build();
        when(estadoCivilRepository.findById(1L)).thenReturn(Optional.of(entity));

        estadoCivilService.desactivar(1L);

        assertFalse(entity.getActivo());
        verify(estadoCivilRepository).save(entity);
    }
}
