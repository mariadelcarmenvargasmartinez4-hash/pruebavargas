package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.model.auth.LoginRequestDto;
import com.proyecto.servicios.model.auth.LoginResponseDto;
import com.proyecto.servicios.service.AuthService;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new ValidadorCampos(), new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("Happy Path: POST /auth/login con credenciales correctas retorna HTTP 200 y Token")
    void loginExitosoRetorna200YToken() throws Exception {
        LoginRequestDto request = new LoginRequestDto("usuario.valido@test.com", TestCredentials.validPass());
        LoginResponseDto response = LoginResponseDto.builder()
                .token("jwt-test-token-valid")
                .nombreCompleto("Juan Perez")
                .clienteId(10L)
                .build();

        when(authService.iniciarSesion(any(LoginRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("jwt-test-token-valid")))
                .andExpect(jsonPath("$.nombreCompleto", is("Juan Perez")))
                .andExpect(jsonPath("$.clienteId", is(10)));
    }

    @Test
    @DisplayName("Seguridad: POST /auth/login con credenciales inválidas retorna HTTP 401 Unauthorized")
    void loginCredencialesInvalidasRetorna401() throws Exception {
        LoginRequestDto request = new LoginRequestDto("usuario.valido@test.com", TestCredentials.wrongPass());

        when(authService.iniciarSesion(any(LoginRequestDto.class)))
                .thenThrow(new CredencialesInvalidasException("El usuario o la contraseña proporcionados son incorrectos"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo", is(401)))
                .andExpect(jsonPath("$.mensaje", containsString("incorrectos")));
    }

    @Test
    @DisplayName("Seguridad: POST /auth/login con usuario desactivado retorna HTTP 403 Forbidden")
    void loginUsuarioInactivoRetorna403() throws Exception {
        LoginRequestDto request = new LoginRequestDto("usuario.inactivo@test.com", TestCredentials.validPass());

        when(authService.iniciarSesion(any(LoginRequestDto.class)))
                .thenThrow(new UsuarioInactivoException("El usuario se encuentra inactivo en el sistema"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo", is(403)))
                .andExpect(jsonPath("$.mensaje", containsString("inactivo")));
    }

    @Test
    @DisplayName("Validación: POST /auth/login con campos vacíos retorna HTTP 400 Bad Request")
    void loginCamposVaciosRetorna400() throws Exception {
        LoginRequestDto request = new LoginRequestDto("", "");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.errores.correo").isNotEmpty())
                .andExpect(jsonPath("$.errores.password").isNotEmpty());
    }

    @Test
    @DisplayName("Robustez: POST /auth/login con payload corrupto retorna HTTP 400 Bad Request")
    void loginPayloadCorruptoRetorna400() throws Exception {
        String payloadCorrupto = "{ username: 'incompleto', password ";

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadCorrupto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.mensaje", containsString("mal formados")));
    }
}
