package com.proyecto.servicios.security;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import com.proyecto.servicios.util.DeclarativeAssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Servicio de seguridad y autorización para control de acceso granular (Anti-IDOR)
 * implementado de forma declarativa sin condicionales imperativas 'if'.
 */
@Slf4j
@Service("securityService")
public class SecurityService {

    private final UsuarioAccesoRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;

    public SecurityService(UsuarioAccesoRepository usuarioRepository,
                           CuentaRepository cuentaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
    }

    public void validarAccesoCliente(Long clienteId) {
        Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .filter(auth -> !"anonymousUser".equals(auth.getPrincipal()))
                .filter(auth -> auth.getAuthorities().stream()
                        .noneMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
                                || a.getAuthority().equalsIgnoreCase("ROLE_EMPLEADO")))
                .ifPresent(auth -> {
                    String username = auth.getName();
                    UsuarioAccesoEntity usuario = usuarioRepository.findByUsername(username)
                            .orElseThrow(() -> new AccessDeniedException("Acceso denegado: Usuario no encontrado"));

                    DeclarativeAssertUtil.asegurar(
                            usuario.getCliente() != null && usuario.getCliente().getId().equals(clienteId),
                            () -> {
                                log.warn("Violación de autorización (IDOR detectado): Usuario '{}' intentó acceder a información del cliente ID {}",
                                        username, clienteId);
                                return new AccessDeniedException("Acceso denegado: No tiene permisos para consultar o manipular los datos de otro usuario");
                            });
                });
    }

    public void validarAccesoCuenta(String numeroCuenta) {
        Optional.ofNullable(numeroCuenta)
                .map(String::trim)
                .filter(nc -> !nc.isBlank())
                .ifPresent(nc -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                        .filter(Authentication::isAuthenticated)
                        .filter(auth -> !"anonymousUser".equals(auth.getPrincipal()))
                        .filter(auth -> auth.getAuthorities().stream()
                                .noneMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
                                        || a.getAuthority().equalsIgnoreCase("ROLE_EMPLEADO")))
                        .ifPresent(auth -> {
                            String username = auth.getName();
                            UsuarioAccesoEntity usuario = usuarioRepository.findByUsername(username)
                                    .orElseThrow(() -> new AccessDeniedException("Acceso denegado: Usuario no encontrado"));

                            cuentaRepository.findByNumeroCuenta(nc).ifPresent(cuenta -> {
                                ClienteEntity titular = cuenta.getCliente();
                                ClienteEntity clienteAutenticado = usuario.getCliente();

                                DeclarativeAssertUtil.asegurar(
                                        titular != null && clienteAutenticado != null && titular.getId().equals(clienteAutenticado.getId()),
                                        () -> {
                                            log.warn("Violación de autorización en cuenta (IDOR detectado): Usuario '{}' intentó acceder a la cuenta {}",
                                                    username, nc);
                                            return new AccessDeniedException("Acceso denegado: No tiene permisos para consultar o manipular una cuenta de otro usuario");
                                        });
                            });
                        }));
    }

    public void validarAccesoCurp(String curp) {
        Optional.ofNullable(curp)
                .map(String::trim)
                .filter(c -> !c.isBlank())
                .ifPresent(c -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                        .filter(Authentication::isAuthenticated)
                        .filter(auth -> !"anonymousUser".equals(auth.getPrincipal()))
                        .filter(auth -> auth.getAuthorities().stream()
                                .noneMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
                                        || a.getAuthority().equalsIgnoreCase("ROLE_EMPLEADO")))
                        .ifPresent(auth -> {
                            String username = auth.getName();
                            UsuarioAccesoEntity usuario = usuarioRepository.findByUsername(username)
                                    .orElseThrow(() -> new AccessDeniedException("Acceso denegado: Usuario no encontrado"));

                            DeclarativeAssertUtil.asegurar(
                                    usuario.getCliente() != null && c.equalsIgnoreCase(usuario.getCliente().getCurp()),
                                    () -> {
                                        log.warn("Violación de autorización en CURP (IDOR detectado): Usuario '{}' intentó consultar CURP {}",
                                                username, c);
                                        return new AccessDeniedException("Acceso denegado: No tiene permisos para consultar información de otro CURP");
                                    });
                        }));
    }

    public void validarAccesoRfc(String rfc) {
        Optional.ofNullable(rfc)
                .map(String::trim)
                .filter(r -> !r.isBlank())
                .ifPresent(r -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                        .filter(Authentication::isAuthenticated)
                        .filter(auth -> !"anonymousUser".equals(auth.getPrincipal()))
                        .filter(auth -> auth.getAuthorities().stream()
                                .noneMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
                                        || a.getAuthority().equalsIgnoreCase("ROLE_EMPLEADO")))
                        .ifPresent(auth -> {
                            String username = auth.getName();
                            UsuarioAccesoEntity usuario = usuarioRepository.findByUsername(username)
                                    .orElseThrow(() -> new AccessDeniedException("Acceso denegado: Usuario no encontrado"));

                            DeclarativeAssertUtil.asegurar(
                                    usuario.getCliente() != null && r.equalsIgnoreCase(usuario.getCliente().getRfc()),
                                    () -> {
                                        log.warn("Violación de autorización en RFC (IDOR detectado): Usuario '{}' intentó consultar RFC {}",
                                                username, r);
                                        return new AccessDeniedException("Acceso denegado: No tiene permisos para consultar información de otro RFC");
                                    });
                        }));
    }

    public void validarAccesoCorreo(String correo) {
        Optional.ofNullable(correo)
                .map(String::trim)
                .filter(co -> !co.isBlank())
                .ifPresent(co -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                        .filter(Authentication::isAuthenticated)
                        .filter(auth -> !"anonymousUser".equals(auth.getPrincipal()))
                        .filter(auth -> auth.getAuthorities().stream()
                                .noneMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
                                        || a.getAuthority().equalsIgnoreCase("ROLE_EMPLEADO")))
                        .ifPresent(auth -> {
                            String username = auth.getName();
                            UsuarioAccesoEntity usuario = usuarioRepository.findByUsername(username)
                                    .orElseThrow(() -> new AccessDeniedException("Acceso denegado: Usuario no encontrado"));

                            DeclarativeAssertUtil.asegurar(
                                    usuario.getCliente() != null && co.equalsIgnoreCase(usuario.getCliente().getCorreoElectronico()),
                                    () -> {
                                        log.warn("Violación de autorización en Correo (IDOR detectado): Usuario '{}' intentó consultar correo {}",
                                                username, co);
                                        return new AccessDeniedException("Acceso denegado: No tiene permisos para consultar información de otro correo electrónico");
                                    });
                        }));
    }
}
