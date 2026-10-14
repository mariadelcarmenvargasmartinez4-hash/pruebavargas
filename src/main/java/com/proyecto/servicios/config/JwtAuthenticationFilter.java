package com.proyecto.servicios.config;

import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import com.proyecto.servicios.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioAccesoRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UsuarioAccesoRepository usuarioRepository) {
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Optional.ofNullable(request.getHeader("Authorization"))
                .map(String::trim)
                .filter(auth -> SecurityContextHolder.getContext().getAuthentication() == null)
                .map(this::extraerTokenLimpio)
                .filter(Predicate.not(String::isBlank))
                .filter(jwtUtil::validarToken)
                .ifPresent(token -> {
                    String username = jwtUtil.extraerUsername(token);
                    usuarioRepository.findByUsername(username)
                            .filter(usuario -> Boolean.TRUE.equals(usuario.getActivo()))
                            .ifPresent(usuario -> SecurityContextHolder.getContext().setAuthentication(
                                    new UsernamePasswordAuthenticationToken(
                                            username, null, AuthorityUtils.createAuthorityList("ROLE_" + usuario.getRol()))));
                });

        filterChain.doFilter(request, response);
    }

    private String extraerTokenLimpio(String auth) {
        String token = auth.regionMatches(true, 0, "Bearer ", 0, 7) ? auth.substring(7).trim() : auth;
        return token.regionMatches(true, 0, "Bearer ", 0, 7) ? token.substring(7).trim() : token;
    }
}