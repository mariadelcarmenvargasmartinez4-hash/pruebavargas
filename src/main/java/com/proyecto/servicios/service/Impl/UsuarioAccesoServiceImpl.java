package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.cliente.UsuarioAccesoDto;
import com.proyecto.servicios.model.cliente.UsuarioAgregarRequestDto;
import com.proyecto.servicios.model.cliente.UsuarioAgregarRequestRecord;
import com.proyecto.servicios.model.cliente.UsuarioConsultaFiltroRecord;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
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

        Page<UsuarioAccesoDto> pagina = usuarioRepository.findAll(spec, pageable).map(this::toDto);
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
        // Validación Fail-Fast en Record
        UsuarioAgregarRequestRecord.de(request);

        ClienteEntity cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException(
                        "Cliente no encontrado con ID " + request.getClienteId()));

        DeclarativeAssertUtil.asegurar(Boolean.TRUE.equals(cliente.getActivo()),
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

        return toDto(usuarioRepository.save(usuario));
    }

    private UsuarioAccesoDto toDto(UsuarioAccesoEntity usuario) {
        return UsuarioAccesoDto.builder()
                .idUsuario(usuario.getId())
                .username(usuario.getUsername())
                .activo(usuario.getActivo())
                .fechaCreacion(usuario.getFechaCreacion())
                .build();
    }
}