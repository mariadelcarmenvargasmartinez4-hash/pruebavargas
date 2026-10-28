package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.controller.catalogo.EstadoCivilController;
import com.proyecto.servicios.exception.CatalogoDuplicadoException;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.model.catalogo.CatalogoActualizacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoConsultaFiltroRecord;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.service.EstadoCivilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class CatalogoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EstadoCivilService estadoCivilService;

    @InjectMocks
    private EstadoCivilController estadoCivilController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(estadoCivilController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("Happy Path: GET /api/catalogos/estados-civiles lista elementos y retorna HTTP 200 OK")
    void listarEstadosCivilesRetorna200() throws Exception {
        CatalogoItemDto item = CatalogoItemDto.builder()
                .id(1L)
                .codigo("SOLTERO")
                .descripcion("Soltero")
                .activo(true)
                .build();

        PaginaResponseRecord<CatalogoItemDto> pagina = new PaginaResponseRecord<>(
                List.of(item), 0, 10, 1L, 1, true, true);

        when(estadoCivilService.buscarPaginado(any(CatalogoConsultaFiltroRecord.class))).thenReturn(pagina);

        mockMvc.perform(get("/api/catalogos/estados-civiles")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(200)))
                .andExpect(jsonPath("$.datos.contenido[0].codigo", is("SOLTERO")));
    }

    @Test
    @DisplayName("Happy Path: POST /api/catalogos/estados-civiles crea item y retorna HTTP 201 Created")
    void crearEstadoCivilRetorna201() throws Exception {
        CatalogoCreacionDto request = new CatalogoCreacionDto("VIUDO", "Viudo");
        CatalogoItemDto response = CatalogoItemDto.builder()
                .id(5L)
                .codigo("VIUDO")
                .descripcion("Viudo")
                .activo(true)
                .build();

        when(estadoCivilService.crear(any(CatalogoCreacionDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/catalogos/estados-civiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo", is("VIUDO")));
    }

    @Test
    @DisplayName("Happy Path: PUT /api/catalogos/estados-civiles/{id} actualiza item y retorna HTTP 200 OK")
    void actualizarEstadoCivilRetorna200() throws Exception {
        CatalogoActualizacionDto request = new CatalogoActualizacionDto("Divorciado legalmente", true);
        CatalogoItemDto response = CatalogoItemDto.builder()
                .id(2L)
                .codigo("DIVORCIADO")
                .descripcion("Divorciado legalmente")
                .activo(true)
                .build();

        when(estadoCivilService.actualizar(eq(2L), any(CatalogoActualizacionDto.class))).thenReturn(response);

        mockMvc.perform(put("/api/catalogos/estados-civiles/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is("DIVORCIADO")))
                .andExpect(jsonPath("$.descripcion", is("Divorciado legalmente")));
    }

    @Test
    @DisplayName("Happy Path: DELETE /api/catalogos/estados-civiles/{id} da de baja y retorna HTTP 204 No Content")
    void eliminarEstadoCivilRetorna204() throws Exception {
        doNothing().when(estadoCivilService).desactivar(3L);

        mockMvc.perform(delete("/api/catalogos/estados-civiles/3"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Validación: Letras en ID numérico de catálogo retorna HTTP 400 Bad Request")
    void idConLetrasRetorna400() throws Exception {
        mockMvc.perform(get("/api/catalogos/estados-civiles/no-es-numero"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.mensaje", containsString("es inválido")));
    }

    @Test
    @DisplayName("Integridad: Código de catálogo duplicado retorna HTTP 409 Conflict")
    void catalogoDuplicadoRetorna409() throws Exception {
        CatalogoCreacionDto request = new CatalogoCreacionDto("SOLTERO", "Soltero duplicado");

        when(estadoCivilService.crear(any(CatalogoCreacionDto.class)))
                .thenThrow(new CatalogoDuplicadoException("El código 'SOLTERO' ya existe en el catálogo"));

        mockMvc.perform(post("/api/catalogos/estados-civiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is(409)))
                .andExpect(jsonPath("$.mensaje", containsString("ya existe")));
    }

    @Test
    @DisplayName("Seguridad: Elemento de catálogo no encontrado retorna HTTP 404 Not Found")
    void catalogoNoEncontradoRetorna404() throws Exception {
        when(estadoCivilService.obtenerPorId(999L))
                .thenThrow(new CatalogoNoEncontradoException("No se encontró el elemento con ID 999"));

        mockMvc.perform(get("/api/catalogos/estados-civiles/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo", is(404)))
                .andExpect(jsonPath("$.mensaje", containsString("No se encontró el elemento")));
    }
}
