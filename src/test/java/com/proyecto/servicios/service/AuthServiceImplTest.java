package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.model.auth.LoginRequestDto;
import com.proyecto.servicios.model.auth.LoginResponseDto;
import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import com.proyecto.servicios.service.Impl.AuthServiceImpl;
import com.proyecto.servicios.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setUp() {
        request = new LoginRequestDto("cliente@example.com", "Segura123#");
        ClienteEntity cliente = ClienteEntity.builder()
                .id(7L)
                .nombre("Ana")
                .apellidoPaterno("Lopez")
                .apellidoMaterno("Garcia")
                .activo(true)
                .build();
        usuario = UsuarioAccesoEntity.builder()
                .cliente(cliente)
                .username("cliente@example.com")
                .passwordHash("bcrypt-hash")
                .activo(true)
                .build();
    }

    @Test
    void iniciarSesionConCredencialesValidasGeneraToken() {
        when(usuarioRepository.findByUsername("cliente@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Segura123#", "bcrypt-hash")).thenReturn(true);
        when(jwtUtil.generarToken("cliente@example.com", 7L)).thenReturn("jwt-token");
        when(jwtUtil.getExpirationSeconds()).thenReturn(3600L);

        LoginResponseDto response = authService.iniciarSesion(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("Ana Lopez Garcia", response.getNombreCompleto());
        assertEquals(7L, response.getClienteId());
        verify(passwordEncoder).matches("Segura123#", "bcrypt-hash");
    }

    @Test
    void iniciarSesionRechazaContrasenaIncorrecta() {
        when(usuarioRepository.findByUsername("cliente@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Segura123#", "bcrypt-hash")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> authService.iniciarSesion(request));
    }

    @Test
    void iniciarSesionRechazaUsuarioInactivo() {
        usuario.setActivo(false);
        when(usuarioRepository.findByUsername("cliente@example.com")).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioInactivoException.class, () -> authService.iniciarSesion(request));
    }
}