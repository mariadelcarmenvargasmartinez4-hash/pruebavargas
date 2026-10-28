package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.exception.PasswordInvalidaException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.exception.UsuarioYaExisteException;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.model.usuario.UsuarioActualizacionRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioBusquedaRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioCambioPasswordRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioCreacionRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioDetalleRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioEstadoRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioResponseDto;
import com.proyecto.servicios.service.UsuarioAccesoService;
import com.proyecto.servicios.util.TestCredentials;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuarioAccesoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UsuarioAccesoService usuarioService;

    @InjectMocks
    private UsuarioAccesoController usuarioController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController)
                .setControllerAdvice(new ValidadorCampos(), new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("POST /usuarios - Registrar usuario exitosamente retorna HTTP 201")
    void crearUsuarioExitosoRetorna201() throws Exception {
        UsuarioCreacionRequestDto request = UsuarioCreacionRequestDto.builder()
                .username("nuevo.usuario@empresa.com")
                .password(TestCredentials.altPass())
                .rol("CLIENTE")
                .clienteId(1L)
                .build();

        UsuarioResponseDto response = UsuarioResponseDto.builder()
                .id(10L)
                .username("nuevo.usuario@empresa.com")
                .rol("CLIENTE")
                .activo(true)
                .clienteId(1L)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        when(usuarioService.crearUsuario(any(UsuarioCreacionRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo", is(200)))
                .andExpect(jsonPath("$.mensaje", is("Usuario registrado exitosamente")))
                .andExpect(jsonPath("$.datos.id", is(10)))
                .andExpect(jsonPath("$.datos.username", is("nuevo.usuario@empresa.com")));
    }

    @Test
    @DisplayName("POST /usuarios - Datos inválidos detonan validación temprana HTTP 400")
    void crearUsuarioInvalidoRetorna400() throws Exception {
        UsuarioCreacionRequestDto request = UsuarioCreacionRequestDto.builder()
                .username("correo-invalido")
                .password("corta")
                .rol("SUPERADMIN")
                .build();

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.errores.username").isNotEmpty())
                .andExpect(jsonPath("$.errores.password").isNotEmpty());
    }

    @Test
    @DisplayName("POST /usuarios - Usuario duplicado retorna HTTP 409 Conflict")
    void crearUsuarioDuplicadoRetorna409() throws Exception {
        UsuarioCreacionRequestDto request = UsuarioCreacionRequestDto.builder()
                .username("existente@empresa.com")
                .password(TestCredentials.altPass())
                .rol("CLIENTE")
                .build();

        when(usuarioService.crearUsuario(any(UsuarioCreacionRequestDto.class)))
                .thenThrow(new UsuarioYaExisteException("El nombre de usuario 'existente@empresa.com' ya se encuentra registrado"));

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is(409)))
                .andExpect(jsonPath("$.mensaje", containsString("ya se encuentra registrado")));
    }

    @Test
    @DisplayName("POST /usuarios/consulta - Consulta dinámica paginada retorna HTTP 200")
    void consultarUsuariosDinamicoRetorna200() throws Exception {
        UsuarioBusquedaRequestDto filtro = UsuarioBusquedaRequestDto.builder()
                .username("admin")
                .activo(true)
                .page(0)
                .size(10)
                .sortBy("id")
                .sortDir("DESC")
                .build();

        UsuarioResponseDto usuario = UsuarioResponseDto.builder()
                .id(1L)
                .username("admin@empresa.com")
                .rol("ADMIN")
                .activo(true)
                .build();

        PaginaResponseRecord<UsuarioResponseDto> pagina = new PaginaResponseRecord<>(
                List.of(usuario), 0, 10, 1L, 1, true, true);

        when(usuarioService.buscarUsuariosDinamico(any(UsuarioBusquedaRequestDto.class))).thenReturn(pagina);

        mockMvc.perform(post("/usuarios/consulta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filtro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(200)))
                .andExpect(jsonPath("$.datos.totalElementos", is(1)))
                .andExpect(jsonPath("$.datos.contenido[0].username", is("admin@empresa.com")));
    }

    @Test
    @DisplayName("POST /usuarios/detalle - Detalle de usuario por ID en RequestBody retorna HTTP 200")
    void obtenerDetalleRetorna200() throws Exception {
        UsuarioDetalleRequestDto request = UsuarioDetalleRequestDto.builder()
                .id(5L)
                .build();

        UsuarioResponseDto usuario = UsuarioResponseDto.builder()
                .id(5L)
                .username("cliente@empresa.com")
                .rol("CLIENTE")
                .activo(true)
                .build();

        when(usuarioService.obtenerDetalleUsuario(any(UsuarioDetalleRequestDto.class))).thenReturn(usuario);

        mockMvc.perform(post("/usuarios/detalle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(200)))
                .andExpect(jsonPath("$.datos.id", is(5)));
    }

    @Test
    @DisplayName("POST /usuarios/detalle - Usuario inexistente retorna HTTP 404")
    void obtenerDetalleInexistenteRetorna404() throws Exception {
        UsuarioDetalleRequestDto request = UsuarioDetalleRequestDto.builder()
                .id(999L)
                .build();

        when(usuarioService.obtenerDetalleUsuario(any(UsuarioDetalleRequestDto.class)))
                .thenThrow(new UsuarioNoEncontradoException("Usuario no encontrado con los criterios especificados"));

        mockMvc.perform(post("/usuarios/detalle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo", is(404)))
                .andExpect(jsonPath("$.mensaje", containsString("Usuario no encontrado")));
    }

    @Test
    @DisplayName("PUT /usuarios - Actualización de usuario retorna HTTP 200")
    void actualizarUsuarioRetorna200() throws Exception {
        UsuarioActualizacionRequestDto request = UsuarioActualizacionRequestDto.builder()
                .id(3L)
                .username("nuevo.email@empresa.com")
                .rol("ADMIN")
                .build();

        UsuarioResponseDto response = UsuarioResponseDto.builder()
                .id(3L)
                .username("nuevo.email@empresa.com")
                .rol("ADMIN")
                .activo(true)
                .build();

        when(usuarioService.actualizarUsuario(any(UsuarioActualizacionRequestDto.class))).thenReturn(response);

        mockMvc.perform(put("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(200)))
                .andExpect(jsonPath("$.datos.rol", is("ADMIN")));
    }

    @Test
    @DisplayName("PATCH /usuarios/password - Cambio de contraseña exitoso retorna HTTP 200")
    void cambiarPasswordRetorna200() throws Exception {
        UsuarioCambioPasswordRequestDto request = UsuarioCambioPasswordRequestDto.builder()
                .id(2L)
                .passwordActual(TestCredentials.oldPass())
                .nuevaPassword(TestCredentials.newPass())
                .build();

        doNothing().when(usuarioService).cambiarPassword(any(UsuarioCambioPasswordRequestDto.class));

        mockMvc.perform(patch("/usuarios/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(200)))
                .andExpect(jsonPath("$.mensaje", is("Contraseña actualizada exitosamente")));
    }

    @Test
    @DisplayName("PATCH /usuarios/password - Contraseña actual incorrecta retorna HTTP 400")
    void cambiarPasswordIncorrectaRetorna400() throws Exception {
        UsuarioCambioPasswordRequestDto request = UsuarioCambioPasswordRequestDto.builder()
                .id(2L)
                .passwordActual(TestCredentials.wrongPass())
                .nuevaPassword(TestCredentials.newPass())
                .build();

        doThrow(new PasswordInvalidaException("La contraseña actual proporcionada no coincide con los registros"))
                .when(usuarioService).cambiarPassword(any(UsuarioCambioPasswordRequestDto.class));

        mockMvc.perform(patch("/usuarios/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.mensaje", containsString("no coincide con los registros")));
    }

    @Test
    @DisplayName("PATCH /usuarios/desactivar - Desactivación lógica de usuario retorna HTTP 200")
    void desactivarUsuarioRetorna200() throws Exception {
        UsuarioEstadoRequestDto request = UsuarioEstadoRequestDto.builder()
                .id(4L)
                .motivo("Baja por inactividad")
                .build();

        UsuarioResponseDto response = UsuarioResponseDto.builder()
                .id(4L)
                .username("usuario@empresa.com")
                .activo(false)
                .build();

        when(usuarioService.desactivarUsuario(any(UsuarioEstadoRequestDto.class))).thenReturn(response);

        mockMvc.perform(patch("/usuarios/desactivar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(200)))
                .andExpect(jsonPath("$.datos.activo", is(false)));
    }

    @Test
    @DisplayName("PATCH /usuarios/reactivar - Reactivación de usuario retorna HTTP 200")
    void reactivarUsuarioRetorna200() throws Exception {
        UsuarioEstadoRequestDto request = UsuarioEstadoRequestDto.builder()
                .id(4L)
                .motivo("Reactivación solicitada")
                .build();

        UsuarioResponseDto response = UsuarioResponseDto.builder()
                .id(4L)
                .username("usuario@empresa.com")
                .activo(true)
                .build();

        when(usuarioService.reactivarUsuario(any(UsuarioEstadoRequestDto.class))).thenReturn(response);

        mockMvc.perform(patch("/usuarios/reactivar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(200)))
                .andExpect(jsonPath("$.datos.activo", is(true)));
    }
}
