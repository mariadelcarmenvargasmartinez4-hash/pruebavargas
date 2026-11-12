package com.proyecto.servicios.security;

import com.proyecto.servicios.model.auth.SesionUsuarioDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio respaldado por el servidor para control estricto de sesión única (Single Session Control).
 * Impide que un mismo usuario mantenga sesiones concurrentes activas (por ejemplo, web y móvil simultáneos),
 * invalidando la sesión previa de forma inmediata ante un nuevo inicio de sesión.
 */
@Slf4j
@Service
public class UserSessionService {

    private final Map<String, SesionUsuarioDto> activeSessions = new ConcurrentHashMap<>();

    /**
     * Registra una nueva sesión para el usuario, invalidando cualquier sesión previa existente.
     *
     * @param username    Nombre de usuario (correo)
     * @param dispositivo Dispositivo de acceso (WEB, MOVIL)
     * @param ipAddress   Dirección IP origen
     * @return Identificador único de sesión generado (UUID)
     */
    public String registrarNuevaSesion(String username, String dispositivo, String ipAddress) {
        String userKey = username.trim().toLowerCase();
        String nuevoSessionId = UUID.randomUUID().toString();
        String dispLimpio = Optional.ofNullable(dispositivo)
                .filter(d -> !d.isBlank())
                .map(String::toUpperCase)
                .orElse("WEB");

        SesionUsuarioDto sesionAnterior = activeSessions.get(userKey);
        if (sesionAnterior != null && Boolean.TRUE.equals(sesionAnterior.getActiva())) {
            log.warn("Control de sesión única: Se invalida la sesión activa previa '{}' de usuario '{}' (desde {}) debido a nuevo ingreso desde '{}' (IP: {})",
                    sesionAnterior.getSessionId(), username, sesionAnterior.getDispositivo(), dispLimpio, ipAddress);
        }

        SesionUsuarioDto nuevaSesion = SesionUsuarioDto.builder()
                .sessionId(nuevoSessionId)
                .username(username)
                .dispositivo(dispLimpio)
                .ipAddress(Optional.ofNullable(ipAddress).orElse("Desconocida"))
                .fechaInicio(LocalDateTime.now())
                .ultimoAcceso(LocalDateTime.now())
                .activa(true)
                .build();

        activeSessions.put(userKey, nuevaSesion);
        log.info("Nueva sesión respaldada en servidor registrada para '{}': SessionId '{}' [{}]", username, nuevoSessionId, dispLimpio);
        return nuevoSessionId;
    }

    /**
     * Valida si el sessionId presentado en el token JWT coincide con la sesión activa única registrada en el servidor.
     */
    public boolean esSesionValida(String username, String sessionId) {
        if (username == null || sessionId == null || sessionId.isBlank()) {
            return false;
        }

        SesionUsuarioDto sesion = activeSessions.get(username.trim().toLowerCase());
        if (sesion == null || !Boolean.TRUE.equals(sesion.getActiva())) {
            log.debug("Sesión rechazada: No existe sesión activa en servidor para usuario '{}'", username);
            return false;
        }

        if (!sesion.getSessionId().equals(sessionId.trim())) {
            log.warn("Violación de sesión única para '{}': El token presenta sessionId '{}', pero la sesión activa en servidor es '{}' [{}]",
                    username, sessionId, sesion.getSessionId(), sesion.getDispositivo());
            return false;
        }

        sesion.setUltimoAcceso(LocalDateTime.now());
        return true;
    }

    /**
     * Invalida la sesión activa del usuario (utilizado en Logout o bloqueo).
     */
    public void invalidarSesion(String username) {
        if (username != null) {
            String userKey = username.trim().toLowerCase();
            SesionUsuarioDto sesion = activeSessions.remove(userKey);
            if (sesion != null) {
                sesion.setActiva(false);
                log.info("Sesión '{}' del usuario '{}' invalidada y cerrada en el servidor", sesion.getSessionId(), username);
            }
        }
    }

    /**
     * Obtiene los datos de la sesión activa del usuario.
     */
    public Optional<SesionUsuarioDto> obtenerSesion(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeSessions.get(username.trim().toLowerCase()))
                .filter(s -> Boolean.TRUE.equals(s.getActiva()));
    }
}
