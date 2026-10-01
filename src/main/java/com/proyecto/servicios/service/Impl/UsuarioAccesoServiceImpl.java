package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.cliente.UsuarioAccesoDto;
import com.proyecto.servicios.model.cliente.UsuarioAgregarRequestDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import com.proyecto.servicios.service.UsuarioAccesoService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioAccesoServiceImpl implements UsuarioAccesoService {

    private final UsuarioAccesoRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioAccesoServiceImpl(UsuarioAccesoRepository usuarioRepository,
                                    ClienteRepository clienteRepository,
                                    PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<UsuarioAccesoDto> filtrarUsuarios(String correo, Boolean activo) {
        List<UsuarioAccesoEntity> usuarios;
        boolean filtraCorreo = correo != null && !correo.isBlank();
        if (filtraCorreo && activo != null) {
            usuarios = usuarioRepository.findByUsernameContainingIgnoreCaseAndActivo(correo.trim(), activo);
        } else if (filtraCorreo) {
            usuarios = usuarioRepository.findByUsernameContainingIgnoreCase(correo.trim());
        } else if (Boolean.TRUE.equals(activo)) {
            usuarios = usuarioRepository.findByActivoTrue();
        } else if (Boolean.FALSE.equals(activo)) {
            usuarios = usuarioRepository.findByActivoFalse();
        } else {
            usuarios = usuarioRepository.findAll();
        }
        return usuarios.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public UsuarioAccesoDto agregarUsuario(UsuarioAgregarRequestDto request) {
        ClienteEntity cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException(
                        "Cliente no encontrado con ID " + request.getClienteId()));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException("No se puede crear un usuario para un cliente inactivo");
        }
        if (usuarioRepository.existsByClienteId(cliente.getId())
                || usuarioRepository.existsByUsername(cliente.getCorreoElectronico())) {
            throw new CorreoDuplicadoException("El cliente ya tiene un usuario de acceso asociado");
        }

        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder()
                .cliente(cliente)
                .username(cliente.getCorreoElectronico())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol("CLIENTE")
                .activo(true)
                .build();
        return toDto(usuarioRepository.save(usuario));
    }

    private UsuarioAccesoDto toDto(UsuarioAccesoEntity usuario) {
        return UsuarioAccesoDto.builder()
                .idUsuario(usuario.getId())
                .username(usuario.getUsername())
                .activo(usuario.getActivo())
                .fechaCreacion(usuario.getFechaCreacion())
                .build();
    }
}