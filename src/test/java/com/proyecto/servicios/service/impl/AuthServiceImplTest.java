package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.model.auth.LoginRequestDto;
import com.proyecto.servicios.model.auth.LoginResponseDto;
import com.proyecto.servicios.model.auth.SesionUsuarioDto;
import com.proyecto.servicios.model.auth.TwoFactorRespuestaDto;
import com.proyecto.servicios.model.auth.TwoFactorSolicitudDto;
import com.proyecto.servicios.model.auth.TwoFactorVerificacionDto;
import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import com.proyecto.servicios.security.TwoFactorAuthService;
import com.proyecto.servicios.security.UserSessionService;
import com.proyecto.servicios.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para AuthServiceImpl con datos construidos mediante Test Data Builders,
 * cobertura completa de autenticación, control de sesión única y 2FA.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioAccesoRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private TwoFactorAuthService twoFactorAuthService;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginRequestDto request;
    private UsuarioAccesoEntity usuario;
    private ClienteEntity cliente;

    @BeforeEach
    void setUp() {
        cliente = ClienteTestDataBuilder.clienteValido();
        usuario = ClienteTestDataBuilder.usuarioValido(cliente);
        request = LoginRequestDto.builder()
                .correo(usuario.getUsername())
                .password(TestCredentials.validPass())
                .dispositivo("WEB")
                .build();
    }

    @Test
    @DisplayName("Iniciar sesión con credenciales válidas genera JWT y datos de sesión única")
    void iniciarSesionConCredencialesValidasGeneraToken() {
        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(TestCredentials.validPass(), TestCredentials.testPassHash())).thenReturn(true);
        when(userSessionService.registrarNuevaSesion(anyString(), anyString(), anyString())).thenReturn("session-uuid-123");
        when(jwtUtil.generarToken(usuario.getUsername(), cliente.getId(), "session-uuid-123")).thenReturn("jwt-token");
        when(jwtUtil.getExpirationSeconds()).thenReturn(3600L);

        LoginResponseDto response = authService.iniciarSesion(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("Juan Carlos Perez Lopez", response.getNombreCompleto());
        assertEquals(cliente.getId(), response.getClienteId());
        assertEquals("session-uuid-123", response.getSessionId());
        verify(userSessionService).registrarNuevaSesion(usuario.getUsername(), "WEB", "IP-Cliente");
    }

    @Test
    @DisplayName("Iniciar sesión con contraseña incorrecta lanza CredencialesInvalidasException")
    void iniciarSesionConPasswordInvalidoLanzaExcepcion() {
        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(TestCredentials.validPass(), TestCredentials.testPassHash())).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> authService.iniciarSesion(request));
    }

    @Test
    @DisplayName("Iniciar sesión con usuario inexistente lanza CredencialesInvalidasException")
    void iniciarSesionConUsuarioInexistenteLanzaExcepcion() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> authService.iniciarSesion(request));
    }

    @Test
    @DisplayName("Iniciar sesión con usuario inactivo lanza UsuarioInactivoException")
    void iniciarSesionConUsuarioInactivoLanzaExcepcion() {
        usuario.setActivo(false);
        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioInactivoException.class, () -> authService.iniciarSesion(request));
    }

    @Test
    @DisplayName("Iniciar sesión con cliente inactivo lanza UsuarioInactivoException")
    void iniciarSesionConClienteInactivoLanzaExcepcion() {
        cliente.setActivo(false);
        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioInactivoException.class, () -> authService.iniciarSesion(request));
    }

    @Test
    @DisplayName("Solicitud de PIN temporal 2FA genera código y retorna tiempo de validez")
    void solicitarTwoFactor_Exitoso() {
        TwoFactorSolicitudDto dto = TwoFactorSolicitudDto.builder().username(usuario.getUsername()).build();
        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(twoFactorAuthService.generarPinTemporal(usuario.getUsername())).thenReturn("481516");
        when(twoFactorAuthService.getExpiracionSegundos()).thenReturn(300L);

        TwoFactorRespuestaDto response = authService.solicitarTwoFactor(dto);

        assertNotNull(response);
        assertEquals(0, response.getCodigo());
        assertEquals(300L, response.getExpiracionSegundos());
    }

    @Test
    @DisplayName("Verificación de PIN 2FA exitosa genera JWT con sesión única")
    void verificarTwoFactor_Exitoso() {
        TwoFactorVerificacionDto dto = TwoFactorVerificacionDto.builder()
                .username(usuario.getUsername())
                .pin("481516")
                .dispositivo("MOVIL")
                .build();

        when(twoFactorAuthService.validarPinTemporal(usuario.getUsername(), "481516")).thenReturn(true);
        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(userSessionService.registrarNuevaSesion(usuario.getUsername(), "MOVIL", "IP-2FA")).thenReturn("session-2fa-789");
        when(jwtUtil.generarToken(usuario.getUsername(), cliente.getId(), "session-2fa-789")).thenReturn("jwt-2fa-token");
        when(jwtUtil.getExpirationSeconds()).thenReturn(3600L);

        LoginResponseDto response = authService.verificarTwoFactor(dto);

        assertNotNull(response);
        assertEquals("jwt-2fa-token", response.getToken());
        assertEquals("session-2fa-789", response.getSessionId());
    }

    @Test
    @DisplayName("Cerrar sesión invalida la sesión activa en el servidor")
    void cerrarSesion_InvalidaEnServidor() {
        authService.cerrarSesion(usuario.getUsername());
        verify(userSessionService).invalidarSesion(usuario.getUsername());
    }

    @Test
    @DisplayName("Obtener sesión activa retorna la sesión registrada")
    void obtenerSesionActiva_Exitoso() {
        SesionUsuarioDto sesion = SesionUsuarioDto.builder()
                .sessionId("uuid-test")
                .username(usuario.getUsername())
                .dispositivo("WEB")
                .activa(true)
                .fechaInicio(LocalDateTime.now())
                .ultimoAcceso(LocalDateTime.now())
                .build();

        when(userSessionService.obtenerSesion(usuario.getUsername())).thenReturn(Optional.of(sesion));

        SesionUsuarioDto res = authService.obtenerSesionActiva(usuario.getUsername());
        assertNotNull(res);
        assertEquals("uuid-test", res.getSessionId());
    }
}
