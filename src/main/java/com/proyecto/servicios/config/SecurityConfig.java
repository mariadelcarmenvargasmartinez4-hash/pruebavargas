package com.proyecto.servicios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración central de Spring Security:
 * - Política sin estado (Stateless JWT)
 * - Protección estricta de rutas privadas (catálogos, compras, cuentas, pagos, usuarios)
 * - Exposición pública restringida únicamente a login, 2FA, alta de cliente y documentación
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // 1. Rutas Públicas: Autenticación, 2FA, Registro inicial de cliente y Swagger OpenAPI
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/2fa/**", "/clientes").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()

                        // 2. Rutas Privadas Protegidas: Catálogo, compras (productos), historial/cuentas, pagos y administración
                        .requestMatchers("/api/catalogos/**").authenticated()
                        .requestMatchers("/productos/**").authenticated()
                        .requestMatchers("/cuentas/**").authenticated()
                        .requestMatchers("/pagos/**").authenticated()
                        .requestMatchers("/usuarios/**").authenticated()
                        .requestMatchers("/auth/logout", "/auth/sesion").authenticated()
                        .requestMatchers(HttpMethod.GET, "/clientes/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/clientes/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/clientes/**").authenticated()

                        // 3. Cualquier otra ruta no especificada requiere autenticación
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
