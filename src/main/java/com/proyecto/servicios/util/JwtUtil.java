package com.proyecto.servicios.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// Utileria para generacion y validacion de tokens JWT
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    // Genera un token JWT con el username como subject y el clienteId como claim
    public String generarToken(String username, Long clienteId) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("clienteId", clienteId)
                .setIssuedAt(ahora)
                .setExpiration(expiracion)
                .signWith(getSigningKey())
                .compact();
    }

    // Extrae el username (subject) del token
    public String extraerUsername(String token) {
        return extraerClaims(token).getSubject();
    }

    // Extrae el clienteId del token
    public Long extraerClienteId(String token) {
        return extraerClaims(token).get("clienteId", Long.class);
    }

    // Obtiene el tiempo de expiracion en segundos
    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    // Valida que el token sea correcto y no haya expirado
    public boolean validarToken(String token) {
        try {
            extraerClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expirado: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Token JWT mal formado: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Token JWT no soportado: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("Firma de token JWT invalida: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Token JWT vacio o nulo: {}", e.getMessage());
        }
        return false;
    }

    // Extrae todos los claims del token
    private Claims extraerClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Obtiene la clave de firma a partir del secret
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
