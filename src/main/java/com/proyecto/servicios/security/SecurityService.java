package com.proyecto.servicios.security;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Servicio de seguridad y autorización para control de acceso granular (Anti-IDOR).
 * Garantiza que cada usuario autenticado únicamente pueda consultar o modificar
 * los recursos (clientes y cuentas) sobre los que tiene permisos legítimos.
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

    /**
     * Valida que el usuario autenticado tenga permisos sobre el identificador de cliente provisto.
     * Los usuarios con roles ADMIN o EMPLEADO tienen acceso global.
     * Los clientes estándar (CLIENTE) solo pueden consultar o manipular sus propios datos.
     *
     * @param clienteId ID del cliente objetivo
     * @throws AccessDeniedException si no cuenta con autorización
     */
    public void validarAccesoCliente(Long clienteId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return;
        }

        // Permisos elevados (Admin / Empleado)
        boolean esPrivilegiado = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
                        || a.getAuthority().equalsIgnoreCase("ROLE_EMPLEADO"));
        if (esPrivilegiado) {
            return;
        }

        String username = auth.getName();
        Optional<UsuarioAccesoEntity> usuarioOpt = usuarioRepository.findByUsername(username);

        if (usuarioOpt.isEmpty() || usuarioOpt.get().getCliente() == null
                || !usuarioOpt.get().getCliente().getId().equals(clienteId)) {
            log.warn("Violación de autorización (IDOR detectado): Usuario '{}' intentó acceder a información del cliente ID {}",
                    username, clienteId);
            throw new AccessDeniedException("Acceso denegado: No tiene permisos para consultar o manipular los datos de otro usuario");
        }
    }

    /**
     * Valida que el usuario autenticado sea titular de la cuenta bancaria especificada.
     *
     * @param numeroCuenta Número de cuenta a validar
     * @throws AccessDeniedException si no cuenta con autorización
     */
    public void validarAccesoCuenta(String numeroCuenta) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return;
        }

        boolean esPrivilegiado = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
                        || a.getAuthority().equalsIgnoreCase("ROLE_EMPLEADO"));
        if (esPrivilegiado) {
            return;
        }

        if (numeroCuenta == null || numeroCuenta.isBlank()) {
            return;
        }

        String username = auth.getName();
        Optional<UsuarioAccesoEntity> usuarioOpt = usuarioRepository.findByUsername(username);
        Optional<CuentaEntity> cuentaOpt = cuentaRepository.findByNumeroCuenta(numeroCuenta.trim());

        if (cuentaOpt.isPresent() && usuarioOpt.isPresent()) {
            ClienteEntity titular = cuentaOpt.get().getCliente();
            ClienteEntity clienteAutenticado = usuarioOpt.get().getCliente();
            if (titular == null || clienteAutenticado == null || !titular.getId().equals(clienteAutenticado.getId())) {
                log.warn("Violación de autorización en cuenta bancaria: Usuario '{}' intentó acceder a la cuenta {}",
                        username, numeroCuenta);
                throw new AccessDeniedException("Acceso denegado: No tiene permisos para consultar o manipular una cuenta de otro usuario");
            }
        }
    }
}
