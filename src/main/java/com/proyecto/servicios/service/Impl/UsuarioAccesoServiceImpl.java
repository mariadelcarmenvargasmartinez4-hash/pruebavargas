package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.PasswordInvalidaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.exception.UsuarioYaExisteException;
import com.proyecto.servicios.model.cliente.UsuarioAccesoDto;
import com.proyecto.servicios.model.cliente.UsuarioAgregarRequestDto;
import com.proyecto.servicios.model.cliente.UsuarioAgregarRequestRecord;
import com.proyecto.servicios.model.cliente.UsuarioConsultaFiltroRecord;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.model.usuario.UsuarioActualizacionRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioActualizacionRequestRecord;
import com.proyecto.servicios.model.usuario.UsuarioBusquedaRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioBusquedaRequestRecord;
import com.proyecto.servicios.model.usuario.UsuarioCambioPasswordRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioCambioPasswordRequestRecord;
import com.proyecto.servicios.model.usuario.UsuarioCreacionRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioCreacionRequestRecord;
import com.proyecto.servicios.model.usuario.UsuarioDetalleRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioDetalleRequestRecord;
import com.proyecto.servicios.model.usuario.UsuarioEstadoRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioEstadoRequestRecord;
import com.proyecto.servicios.model.usuario.UsuarioResponseDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import com.proyecto.servicios.service.UsuarioAccesoService;
import com.proyecto.servicios.util.DeclarativeAssertUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Implementación del servicio de administración de usuarios bajo estándares Senior:
 * - Desacoplamiento estricto y separación por capas
 * - Validación temprana Fail-Fast mediante Records
 * - Búsqueda dinámica con JPA Specification combinando múltiples filtros opcionales
 * - Enfoque declarativo y funcional (ausencia de sentencias if imperativas)
 */
@Service
public class UsuarioAccesoServiceImpl implements UsuarioAccesoService {

    private final UsuarioAccesoRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioAccesoServiceImpl(UsuarioAccesoRepository usuarioRepository,
                                    ClienteRepository clienteRepository,
                                    PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public UsuarioResponseDto crearUsuario(UsuarioCreacionRequestDto request) {
        UsuarioCreacionRequestRecord record = request.toRecord();

        DeclarativeAssertUtil.prohibir(
                usuarioRepository.existsByUsername(record.username()),
                () -> new UsuarioYaExisteException("El nombre de usuario '" + record.username() + "' ya se encuentra registrado"));

        ClienteEntity cliente = Optional.ofNullable(record.clienteId())
                .map(id -> clienteRepository.findById(id)
                        .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID " + id)))
                .orElse(null);

        Optional.ofNullable(cliente).ifPresent(c -> {
            DeclarativeAssertUtil.asegurar(
                    Boolean.TRUE.equals(c.getActivo()),
                    () -> new ReglaNegocioException("No se puede crear un usuario para un cliente inactivo"));
            DeclarativeAssertUtil.prohibir(
                    usuarioRepository.existsByClienteId(c.getId()),
                    () -> new ReglaNegocioException("El cliente con ID " + c.getId() + " ya tiene un usuario de acceso asignado"));
        });

        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder()
                .cliente(cliente)
                .username(record.username())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol(record.rol())
                .activo(true)
                .build();

        return toResponseDto(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public PaginaResponseRecord<UsuarioResponseDto> buscarUsuariosDinamico(UsuarioBusquedaRequestDto filtro) {
        UsuarioBusquedaRequestRecord record = filtro.toRecord();

        Sort sort = Sort.by(Sort.Direction.fromString(record.sortDir()), record.sortBy());
        Pageable pageable = PageRequest.of(record.page(), record.size(), sort);

        Specification<UsuarioAccesoEntity> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            Optional.ofNullable(record.id())
                    .map(id -> cb.equal(root.get("id"), id))
                    .ifPresent(predicates::add);

            Optional.ofNullable(record.username())
                    .filter(Predicate.not(String::isBlank))
                    .map(u -> cb.like(cb.lower(root.get("username")), "%" + u.toLowerCase() + "%"))
                    .ifPresent(predicates::add);

            Optional.ofNullable(record.rol())
                    .filter(Predicate.not(String::isBlank))
                    .map(r -> cb.equal(root.get("rol"), r))
                    .ifPresent(predicates::add);

            Optional.ofNullable(record.activo())
                    .map(a -> cb.equal(root.get("activo"), a))
                    .ifPresent(predicates::add);

            Optional.ofNullable(record.clienteId())
                    .map(cId -> cb.equal(root.get("cliente").get("id"), cId))
                    .ifPresent(predicates::add);

            Optional.ofNullable(record.fechaInicio())
                    .map(fi -> cb.greaterThanOrEqualTo(root.get("fechaCreacion"), fi))
                    .ifPresent(predicates::add);

            Optional.ofNullable(record.fechaFin())
                    .map(ff -> cb.lessThanOrEqualTo(root.get("fechaCreacion"), ff))
                    .ifPresent(predicates::add);

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<UsuarioResponseDto> pagina = usuarioRepository.findAll(spec, pageable).map(this::toResponseDto);
        return PaginaResponseRecord.de(pagina);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public UsuarioResponseDto obtenerDetalleUsuario(UsuarioDetalleRequestDto request) {
        UsuarioDetalleRequestRecord record = request.toRecord();

        UsuarioAccesoEntity usuario = Optional.ofNullable(record.id())
                .flatMap(usuarioRepository::findById)
                .or(() -> Optional.ofNullable(record.username()).flatMap(usuarioRepository::findByUsername))
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con los criterios especificados"));

        return toResponseDto(usuario);
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public UsuarioResponseDto actualizarUsuario(UsuarioActualizacionRequestDto request) {
        UsuarioActualizacionRequestRecord record = request.toRecord();

        UsuarioAccesoEntity usuario = usuarioRepository.findById(record.id())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID " + record.id()));

        Optional.ofNullable(record.username())
                .filter(Predicate.not(u -> u.equalsIgnoreCase(usuario.getUsername())))
                .ifPresent(u -> {
                    DeclarativeAssertUtil.prohibir(
                            usuarioRepository.existsByUsername(u),
                            () -> new UsuarioYaExisteException("El nombre de usuario '" + u + "' ya se encuentra registrado por otro usuario"));
                    usuario.setUsername(u);
                });

        Optional.ofNullable(record.rol())
                .ifPresent(usuario::setRol);

        return toResponseDto(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public void cambiarPassword(UsuarioCambioPasswordRequestDto request) {
        UsuarioCambioPasswordRequestRecord record = request.toRecord();

        UsuarioAccesoEntity usuario = usuarioRepository.findById(record.id())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID " + record.id()));

        DeclarativeAssertUtil.asegurar(
                passwordEncoder.matches(record.passwordActual(), usuario.getPasswordHash()),
                () -> new PasswordInvalidaException("La contraseña actual proporcionada no coincide con los registros"));

        usuario.setPasswordHash(passwordEncoder.encode(record.nuevaPassword()));
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public UsuarioResponseDto desactivarUsuario(UsuarioEstadoRequestDto request) {
        UsuarioEstadoRequestRecord record = request.toRecord();

        UsuarioAccesoEntity usuario = usuarioRepository.findById(record.id())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID " + record.id()));

        DeclarativeAssertUtil.asegurar(
                Boolean.TRUE.equals(usuario.getActivo()),
                () -> new ReglaNegocioException("El usuario con ID " + record.id() + " ya se encuentra inactivo"));

        usuario.setActivo(false);
        return toResponseDto(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public UsuarioResponseDto reactivarUsuario(UsuarioEstadoRequestDto request) {
        UsuarioEstadoRequestRecord record = request.toRecord();

        UsuarioAccesoEntity usuario = usuarioRepository.findById(record.id())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID " + record.id()));

        DeclarativeAssertUtil.prohibir(
                Boolean.TRUE.equals(usuario.getActivo()),
                () -> new ReglaNegocioException("El usuario con ID " + record.id() + " ya se encuentra activo"));

        usuario.setActivo(true);
        return toResponseDto(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public PaginaResponseRecord<UsuarioAccesoDto> buscarUsuariosPaginados(UsuarioConsultaFiltroRecord filtro) {
        Sort sort = Sort.by(Sort.Direction.fromString(filtro.sortDir()), filtro.sortBy());
        Pageable pageable = PageRequest.of(filtro.page(), filtro.size(), sort);

        Specification<UsuarioAccesoEntity> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            Optional.ofNullable(filtro.correo())
                    .filter(Predicate.not(String::isBlank))
                    .map(c -> cb.like(cb.lower(root.get("username")), "%" + c.toLowerCase() + "%"))
                    .ifPresent(predicates::add);

            Optional.ofNullable(filtro.activo())
                    .map(a -> cb.equal(root.get("activo"), a))
                    .ifPresent(predicates::add);

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<UsuarioAccesoDto> pagina = usuarioRepository.findAll(spec, pageable).map(this::toLegacyDto);
        return PaginaResponseRecord.de(pagina);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<UsuarioAccesoDto> filtrarUsuarios(String correo, Boolean activo) {
        UsuarioConsultaFiltroRecord filtro = new UsuarioConsultaFiltroRecord(correo, activo, 0, 100, "id", "DESC", null);
        return buscarUsuariosPaginados(filtro).contenido();
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public UsuarioAccesoDto agregarUsuario(UsuarioAgregarRequestDto request) {
        UsuarioAgregarRequestRecord.de(request);

        ClienteEntity cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException(
                        "Cliente no encontrado con ID " + request.getClienteId()));

        DeclarativeAssertUtil.asegurar(
                Boolean.TRUE.equals(cliente.getActivo()),
                () -> new ReglaNegocioException("No se puede crear un usuario para un cliente inactivo"));

        DeclarativeAssertUtil.prohibir(
                usuarioRepository.existsByClienteId(cliente.getId()) || usuarioRepository.existsByUsername(cliente.getCorreoElectronico()),
                () -> new CorreoDuplicadoException("El cliente ya tiene un usuario de acceso asociado"));

        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder()
                .cliente(cliente)
                .username(cliente.getCorreoElectronico())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol("CLIENTE")
                .activo(true)
                .build();

        return toLegacyDto(usuarioRepository.save(usuario));
    }

    private UsuarioResponseDto toResponseDto(UsuarioAccesoEntity usuario) {
        return UsuarioResponseDto.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .rol(usuario.getRol())
                .activo(usuario.getActivo())
                .clienteId(Optional.ofNullable(usuario.getCliente()).map(ClienteEntity::getId).orElse(null))
                .fechaCreacion(usuario.getFechaCreacion())
                .fechaActualizacion(usuario.getFechaActualizacion())
                .build();
    }

    private UsuarioAccesoDto toLegacyDto(UsuarioAccesoEntity usuario) {
        return UsuarioAccesoDto.builder()
                .idUsuario(usuario.getId())
                .username(usuario.getUsername())
                .activo(usuario.getActivo())
                .fechaCreacion(usuario.getFechaCreacion())
                .build();
    }
}