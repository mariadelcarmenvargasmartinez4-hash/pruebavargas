package com.proyecto.servicios.security;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
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
    private final ClienteRepository clienteRepository;

    public SecurityService(UsuarioAccesoRepository usuarioRepository,
                           CuentaRepository cuentaRepository,
                           ClienteRepository clienteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
        this.clienteRepository = clienteRepository;
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
                            Optional<UsuarioAccesoEntity> usuarioOpt = usuarioRepository.findByUsername(username);
                            Optional<CuentaEntity> cuentaOpt = cuentaRepository.findByNumeroCuenta(nc);

                            cuentaOpt.ifPresent(cuenta -> usuarioOpt.ifPresent(usuario -> {
                                ClienteEntity titular = cuenta.getCliente();
                                ClienteEntity clienteAutenticado = usuario.getCliente();

                                DeclarativeAssertUtil.asegurar(
                                        titular != null && clienteAutenticado != null && titular.getId().equals(clienteAutenticado.getId()),
                                        () -> {
                                            log.warn("Violación de autorización en cuenta: Usuario '{}' intentó acceder a la cuenta {}",
                                                    username, nc);
                                            return new AccessDeniedException("Acceso denegado: No tiene permisos para consultar o manipular una cuenta de otro usuario");
                                        });
                            }));
                        }));
    }
}
