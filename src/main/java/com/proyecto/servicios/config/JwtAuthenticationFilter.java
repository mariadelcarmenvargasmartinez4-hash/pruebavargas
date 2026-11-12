package com.proyecto.servicios.config;

import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import com.proyecto.servicios.security.UserSessionService;
import com.proyecto.servicios.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Filtro de autenticación JWT con validación estricta de sesión única respaldada por el servidor.
 * Verifica la firma del token y comprueba que el 'sessionId' pertenezca a la única sesión activa del usuario.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioAccesoRepository usuarioRepository;
    private final UserSessionService userSessionService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   UsuarioAccesoRepository usuarioRepository,
                                   UserSessionService userSessionService) {
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
        this.userSessionService = userSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && !authHeader.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = extraerTokenLimpio(authHeader.trim());

            if (!token.isBlank() && jwtUtil.validarToken(token)) {
                String username = jwtUtil.extraerUsername(token);
                String sessionId = jwtUtil.extraerSessionId(token);

                // Control de sesión única: Validar si la sesión sigue activa en el servidor
                if (sessionId != null && !sessionId.isBlank() && !userSessionService.esSesionValida(username, sessionId)) {
                    log.warn("Petición rechazada (401): Sesión concurrente no autorizada o cerrada para usuario '{}'", username);
                    responderSesionInvalida(response);
                    return;
                }

                usuarioRepository.findByUsername(username)
                        .filter(usuario -> Boolean.TRUE.equals(usuario.getActivo()))
                        .ifPresent(usuario -> {
                            String rolRaw = Optional.ofNullable(usuario.getRol()).filter(r -> !r.isBlank()).orElse("CLIENTE");
                            String authority = rolRaw.startsWith("ROLE_") ? rolRaw : "ROLE_" + rolRaw;
                            SecurityContextHolder.getContext().setAuthentication(
                                    new UsernamePasswordAuthenticationToken(
                                            username, null, AuthorityUtils.createAuthorityList(authority)));
                        });
            }
        }

        filterChain.doFilter(request, response);
    }

    private void responderSesionInvalida(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"codigo\":401,\"mensaje\":\"Sesión finalizada: Se ha iniciado sesión desde otro dispositivo o la sesión fue cerrada en el servidor.\"}");
    }

    private String extraerTokenLimpio(String auth) {
        String token = auth.regionMatches(true, 0, "Bearer ", 0, 7) ? auth.substring(7).trim() : auth;
        return token.regionMatches(true, 0, "Bearer ", 0, 7) ? token.substring(7).trim() : token;
    }
}
