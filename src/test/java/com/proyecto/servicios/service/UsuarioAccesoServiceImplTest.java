package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.model.cliente.UsuarioAccesoDto;
import com.proyecto.servicios.model.cliente.UsuarioAgregarRequestDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import com.proyecto.servicios.service.Impl.UsuarioAccesoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioAccesoServiceImplTest {

    @Mock
    private UsuarioAccesoRepository usuarioRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioAccesoServiceImpl usuarioService;

    @Test
    void agregarUsuarioPersisteLaContrasenaCifradaYAsignacionCliente() {
        ClienteEntity cliente = ClienteEntity.builder()
                .id(7L)
                .correoElectronico("cliente@example.com")
                .activo(true)
                .build();
        UsuarioAgregarRequestDto request = new UsuarioAgregarRequestDto(7L, "Segura123#");
        UsuarioAccesoEntity usuarioGuardado = UsuarioAccesoEntity.builder()
                .id(12L)
                .cliente(cliente)
                .username("cliente@example.com")
                .passwordHash("bcrypt-hash")
                .rol("CLIENTE")
                .activo(true)
                .build();

        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.existsByClienteId(7L)).thenReturn(false);
        when(usuarioRepository.existsByUsername("cliente@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Segura123#")).thenReturn("bcrypt-hash");
        when(usuarioRepository.save(any(UsuarioAccesoEntity.class))).thenReturn(usuarioGuardado);

        UsuarioAccesoDto response = usuarioService.agregarUsuario(request);

        assertEquals("cliente@example.com", response.getUsername());
        assertEquals(true, response.getActivo());
        verify(passwordEncoder).encode("Segura123#");
        verify(usuarioRepository).save(any(UsuarioAccesoEntity.class));
    }
}