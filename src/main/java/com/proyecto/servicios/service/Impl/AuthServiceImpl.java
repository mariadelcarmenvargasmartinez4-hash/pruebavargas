package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.model.auth.LoginRequestDto;
import com.proyecto.servicios.model.auth.LoginResponseDto;
import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import com.proyecto.servicios.service.AuthService;
import com.proyecto.servicios.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioAccesoRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UsuarioAccesoRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDto iniciarSesion(LoginRequestDto request) {
        UsuarioAccesoEntity usuario = usuarioRepository.findByUsername(request.getCorreo().trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new CredencialesInvalidasException("Correo o contrasena incorrectos"));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new UsuarioInactivoException("El usuario esta inactivo");
        }

        ClienteEntity cliente = usuario.getCliente();
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new UsuarioInactivoException("El cliente asociado esta inactivo");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("Correo o contrasena incorrectos");
        }

        String nombreCompleto = Stream.of(cliente.getNombre(), cliente.getSegundoNombre(),
                        cliente.getApellidoPaterno(), cliente.getApellidoMaterno())
                .filter(parte -> parte != null && !parte.isBlank())
                .collect(Collectors.joining(" "));

        return LoginResponseDto.builder()
                .codigo(0)
                .mensaje("Autenticacion exitosa")
                .token(jwtUtil.generarToken(usuario.getUsername(), cliente.getId()))
                .tipoToken("Bearer")
                .username(usuario.getUsername())
                .clienteId(cliente.getId())
                .nombreCompleto(nombreCompleto)
                .expiresIn(jwtUtil.getExpirationSeconds())
                .fechaAutenticacion(LocalDateTime.now())
                .build();
    }
}