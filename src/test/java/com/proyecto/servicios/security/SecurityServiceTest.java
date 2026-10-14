package com.proyecto.servicios.security;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    private UsuarioAccesoRepository usuarioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testValidarAccesoCliente_MismoCliente_PermiteAcceso() {
        String username = "cliente@test.com";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))));

        ClienteEntity cliente = ClienteEntity.builder().id(10L).build();
        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder().username(username).cliente(cliente).build();

        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(usuario));

        assertDoesNotThrow(() -> securityService.validarAccesoCliente(10L));
    }

    @Test
    void testValidarAccesoCliente_DiferenteCliente_LanzaAccessDeniedException() {
        String username = "cliente@test.com";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))));

        ClienteEntity cliente = ClienteEntity.builder().id(10L).build();
        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder().username(username).cliente(cliente).build();

        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(usuario));

        assertThrows(AccessDeniedException.class, () -> securityService.validarAccesoCliente(99L));
    }

    @Test
    void testValidarAccesoCliente_Admin_PermiteAccesoCualquierId() {
        String username = "admin@banco.com";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        assertDoesNotThrow(() -> securityService.validarAccesoCliente(999L));
    }

    @Test
    void testValidarAccesoCuenta_TitularLegitimo_PermiteAcceso() {
        String username = "cliente@test.com";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))));

        ClienteEntity cliente = ClienteEntity.builder().id(10L).build();
        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder().username(username).cliente(cliente).build();
        CuentaEntity cuenta = CuentaEntity.builder().numeroCuenta("4815162342").cliente(cliente).build();

        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("4815162342")).thenReturn(Optional.of(cuenta));

        assertDoesNotThrow(() -> securityService.validarAccesoCuenta("4815162342"));
    }

    @Test
    void testValidarAccesoCuenta_CuentaAjena_LanzaAccessDeniedException() {
        String username = "atacante@test.com";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))));

        ClienteEntity clienteAtacante = ClienteEntity.builder().id(10L).build();
        ClienteEntity clienteVictima = ClienteEntity.builder().id(20L).build();

        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder().username(username).cliente(clienteAtacante).build();
        CuentaEntity cuentaVictima = CuentaEntity.builder().numeroCuenta("4815162342").cliente(clienteVictima).build();

        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("4815162342")).thenReturn(Optional.of(cuentaVictima));

        assertThrows(AccessDeniedException.class, () -> securityService.validarAccesoCuenta("4815162342"));
    }
}
