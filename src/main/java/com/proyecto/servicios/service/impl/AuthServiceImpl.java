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
import com.proyecto.servicios.service.AuthService;
import com.proyecto.servicios.util.DeclarativeAssertUtil;
import com.proyecto.servicios.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioAccesoRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserSessionService userSessionService;
    private final TwoFactorAuthService twoFactorAuthService;

    public AuthServiceImpl(UsuarioAccesoRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           UserSessionService userSessionService,
                           TwoFactorAuthService twoFactorAuthService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userSessionService = userSessionService;
        this.twoFactorAuthService = twoFactorAuthService;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDto iniciarSesion(LoginRequestDto request) {
        request.toRecord(); // Validación temprana Fail-Fast en Record

        UsuarioAccesoEntity usuario = usuarioRepository.findByUsername(request.getCorreo().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contrasena incorrectos"));

        DeclarativeAssertUtil.asegurar(Boolean.TRUE.equals(usuario.getActivo()),
                () -> new UsuarioInactivoException("El usuario esta inactivo"));

        ClienteEntity cliente = usuario.getCliente();
        DeclarativeAssertUtil.asegurar(Boolean.TRUE.equals(cliente.getActivo()),
                () -> new UsuarioInactivoException("El cliente asociado esta inactivo"));

        DeclarativeAssertUtil.asegurar(passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash()),
                () -> new CredencialesInvalidasException("Correo o contrasena incorrectos"));

        String dispositivo = Optional.ofNullable(request.getDispositivo()).filter(d -> !d.isBlank()).orElse("WEB");
        String sessionId = userSessionService.registrarNuevaSesion(usuario.getUsername(), dispositivo, "IP-Cliente");

        String nombreCompleto = obtenerNombreCompleto(cliente);

        return LoginResponseDto.builder()
                .codigo(0)
                .mensaje("Autenticacion exitosa")
                .token(jwtUtil.generarToken(usuario.getUsername(), cliente.getId(), sessionId))
                .tipoToken("Bearer")
                .username(usuario.getUsername())
                .clienteId(cliente.getId())
                .nombreCompleto(nombreCompleto)
                .expiresIn(jwtUtil.getExpirationSeconds())
                .fechaAutenticacion(LocalDateTime.now())
                .sessionId(sessionId)
                .requiere2fa(false)
                .build();
    }

    @Override
    public TwoFactorRespuestaDto solicitarTwoFactor(TwoFactorSolicitudDto request) {
        String usernameLimpio = request.getUsername().trim().toLowerCase(Locale.ROOT);
        UsuarioAccesoEntity usuario = usuarioRepository.findByUsername(usernameLimpio)
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario no encontrado"));

        DeclarativeAssertUtil.asegurar(Boolean.TRUE.equals(usuario.getActivo()),
                () -> new UsuarioInactivoException("El usuario esta inactivo"));

        twoFactorAuthService.generarPinTemporal(usernameLimpio);

        return TwoFactorRespuestaDto.builder()
                .codigo(0)
                .mensaje("PIN temporal 2FA generado y enviado exitosamente. Válido por 5 minutos.")
                .expiracionSegundos(twoFactorAuthService.getExpiracionSegundos())
                .fechaExpiracion(LocalDateTime.now().plusSeconds(twoFactorAuthService.getExpiracionSegundos()))
                .verificado(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDto verificarTwoFactor(TwoFactorVerificacionDto request) {
        String usernameLimpio = request.getUsername().trim().toLowerCase(Locale.ROOT);
        twoFactorAuthService.validarPinTemporal(usernameLimpio, request.getPin());

        UsuarioAccesoEntity usuario = usuarioRepository.findByUsername(usernameLimpio)
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario no encontrado"));

        ClienteEntity cliente = usuario.getCliente();
        DeclarativeAssertUtil.asegurar(Boolean.TRUE.equals(cliente.getActivo()),
                () -> new UsuarioInactivoException("El cliente asociado esta inactivo"));

        String dispositivo = Optional.ofNullable(request.getDispositivo()).filter(d -> !d.isBlank()).orElse("WEB");
        String sessionId = userSessionService.registrarNuevaSesion(usuario.getUsername(), dispositivo, "IP-2FA");

        return LoginResponseDto.builder()
                .codigo(0)
                .mensaje("Autenticación multifactor (2FA) completada con éxito")
                .token(jwtUtil.generarToken(usuario.getUsername(), cliente.getId(), sessionId))
                .tipoToken("Bearer")
                .username(usuario.getUsername())
                .clienteId(cliente.getId())
                .nombreCompleto(obtenerNombreCompleto(cliente))
                .expiresIn(jwtUtil.getExpirationSeconds())
                .fechaAutenticacion(LocalDateTime.now())
                .sessionId(sessionId)
                .requiere2fa(false)
                .build();
    }

    @Override
    public void cerrarSesion(String username) {
        userSessionService.invalidarSesion(username);
    }

    @Override
    public SesionUsuarioDto obtenerSesionActiva(String username) {
        return userSessionService.obtenerSesion(username)
                .orElseThrow(() -> new CredencialesInvalidasException("No existe ninguna sesión activa para el usuario"));
    }

    private String obtenerNombreCompleto(ClienteEntity cliente) {
        return Stream.of(cliente.getNombre(), cliente.getSegundoNombre(),
                        cliente.getApellidoPaterno(), cliente.getApellidoMaterno())
                .filter(parte -> parte != null && !parte.isBlank())
                .collect(Collectors.joining(" "));
    }
}
