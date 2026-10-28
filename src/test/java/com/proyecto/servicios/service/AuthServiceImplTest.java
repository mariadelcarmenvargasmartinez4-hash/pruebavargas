package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.model.auth.LoginRequestDto;
import com.proyecto.servicios.model.auth.LoginResponseDto;
import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import com.proyecto.servicios.service.Impl.AuthServiceImpl;
import com.proyecto.servicios.util.ClienteTestDataBuilder;
import com.proyecto.servicios.util.JwtUtil;
import com.proyecto.servicios.util.TestCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para AuthServiceImpl con datos construidos mediante Test Data Builders.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioAccesoRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginRequestDto request;
    private UsuarioAccesoEntity usuario;
    private ClienteEntity cliente;

    @BeforeEach
    void setUp() {
        cliente = ClienteTestDataBuilder.clienteValido();
        usuario = ClienteTestDataBuilder.usuarioValido(cliente);
        request = new LoginRequestDto(usuario.getUsername(), TestCredentials.validPass());
    }

    @Test
    @DisplayName("Iniciar sesión con credenciales válidas genera JWT y datos de usuario")
    void iniciarSesionConCredencialesValidasGeneraToken() {
        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(TestCredentials.validPass(), TestCredentials.testPassHash())).thenReturn(true);
        when(jwtUtil.generarToken(usuario.getUsername(), cliente.getId())).thenReturn("jwt-token");
        when(jwtUtil.getExpirationSeconds()).thenReturn(3600L);

        LoginResponseDto response = authService.iniciarSesion(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("Juan Carlos Perez Lopez", response.getNombreCompleto());
        assertEquals(cliente.getId(), response.getClienteId());
        verify(passwordEncoder).matches(TestCredentials.validPass(), TestCredentials.testPassHash());
    }

    @Test
    @DisplayName("Iniciar sesión con contraseña errónea lanza CredencialesInvalidasException")
    void iniciarSesionRechazaContrasenaIncorrecta() {
        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(TestCredentials.validPass(), TestCredentials.testPassHash())).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> authService.iniciarSesion(request));
    }

    @Test
    @DisplayName("Iniciar sesión con usuario inactivo lanza UsuarioInactivoException")
    void iniciarSesionRechazaUsuarioInactivo() {
        usuario.setActivo(false);
        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioInactivoException.class, () -> authService.iniciarSesion(request));
    }
}
