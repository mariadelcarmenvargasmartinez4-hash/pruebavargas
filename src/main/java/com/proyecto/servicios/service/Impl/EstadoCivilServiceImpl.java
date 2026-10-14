package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogo.EstadoCivilEntity;
import com.proyecto.servicios.exception.CatalogoDuplicadoException;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.model.catalogo.CatalogoActualizacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoConsultaFiltroRecord;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.repositorys.catalogo.EstadoCivilRepository;
import com.proyecto.servicios.service.EstadoCivilService;
import com.proyecto.servicios.util.DeclarativeAssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EstadoCivilServiceImpl implements EstadoCivilService {

    private final EstadoCivilRepository estadoCivilRepository;

    public EstadoCivilServiceImpl(EstadoCivilRepository estadoCivilRepository) {
        this.estadoCivilRepository = estadoCivilRepository;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public PaginaResponseRecord<CatalogoItemDto> buscarPaginado(CatalogoConsultaFiltroRecord filtro) {
        Sort sort = Sort.by(Sort.Direction.fromString(filtro.sortDir()), filtro.sortBy());
        Pageable pageable = PageRequest.of(filtro.page(), filtro.size(), sort);

        Specification<EstadoCivilEntity> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            Optional.ofNullable(filtro.soloActivos())
                    .filter(Boolean::booleanValue)
                    .map(a -> cb.isTrue(root.get("activo")))
                    .ifPresent(predicates::add);

            Optional.ofNullable(filtro.busqueda())
                    .filter(Predicate.not(String::isBlank))
                    .map(b -> cb.or(
                            cb.like(cb.lower(root.get("codigo")), "%" + b.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("descripcion")), "%" + b.toLowerCase() + "%")))
                    .ifPresent(predicates::add);

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<CatalogoItemDto> page = estadoCivilRepository.findAll(spec, pageable).map(this::mapearADto);
        return PaginaResponseRecord.de(page);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<CatalogoItemDto> listarTodos() {
        return estadoCivilRepository.findAll().stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<CatalogoItemDto> listarActivos() {
        return estadoCivilRepository.findByActivoTrue().stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public CatalogoItemDto obtenerPorId(Long id) {
        return estadoCivilRepository.findById(id)
                .map(this::mapearADto)
                .orElseThrow(() -> new CatalogoNoEncontradoException("Elemento de estado civil no encontrado con ID " + id));
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public CatalogoItemDto obtenerPorCodigoODescripcion(String valor) {
        String limpio = valor.trim().toUpperCase();
        return estadoCivilRepository.findByCodigoIgnoreCase(limpio)
                .or(() -> estadoCivilRepository.findByDescripcionIgnoreCase(limpio))
                .map(this::mapearADto)
                .orElseThrow(() -> new CatalogoNoEncontradoException("Estado civil no encontrado para el valor '" + valor + "'"));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public CatalogoItemDto crear(CatalogoCreacionDto dto) {
        dto.toRecord(); // Validación temprana Fail-Fast en Record
        String codigo = dto.getCodigo().trim().toUpperCase();
        String descripcion = dto.getDescripcion().trim().toUpperCase();

        DeclarativeAssertUtil.prohibir(estadoCivilRepository.existsByCodigoIgnoreCase(codigo),
                () -> new CatalogoDuplicadoException("El código de estado civil '" + codigo + "' ya está registrado"));

        DeclarativeAssertUtil.prohibir(estadoCivilRepository.existsByDescripcionIgnoreCase(descripcion),
                () -> new CatalogoDuplicadoException("La descripción de estado civil '" + descripcion + "' ya está registrada"));

        EstadoCivilEntity entidad = EstadoCivilEntity.builder()
                .codigo(codigo)
                .descripcion(descripcion)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        return mapearADto(estadoCivilRepository.save(entidad));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public CatalogoItemDto actualizar(Long id, CatalogoActualizacionDto dto) {
        dto.toRecord(); // Validación temprana Fail-Fast en Record
        EstadoCivilEntity entidad = estadoCivilRepository.findById(id)
                .orElseThrow(() -> new CatalogoNoEncontradoException("Elemento de estado civil no encontrado con ID " + id));

        Optional.ofNullable(dto.getDescripcion())
                .map(String::trim)
                .filter(Predicate.not(String::isBlank))
                .ifPresent(desc -> {
                    String descUpper = desc.toUpperCase();
                    DeclarativeAssertUtil.prohibir(!descUpper.equalsIgnoreCase(entidad.getDescripcion()) && estadoCivilRepository.existsByDescripcionIgnoreCase(descUpper),
                            () -> new CatalogoDuplicadoException("La descripción '" + descUpper + "' ya pertenece a otro registro"));
                    entidad.setDescripcion(descUpper);
                });

        Optional.ofNullable(dto.getActivo()).ifPresent(entidad::setActivo);

        return mapearADto(estadoCivilRepository.save(entidad));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public void desactivar(Long id) {
        EstadoCivilEntity entidad = estadoCivilRepository.findById(id)
                .orElseThrow(() -> new CatalogoNoEncontradoException("Elemento de estado civil no encontrado con ID " + id));
        entidad.setActivo(false);
        estadoCivilRepository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public boolean esValidoYActivo(String valor) {
        return Optional.ofNullable(valor)
                .map(String::trim)
                .filter(Predicate.not(String::isBlank))
                .map(String::toUpperCase)
                .map(limpio -> estadoCivilRepository.existsByCodigoIgnoreCaseAndActivoTrue(limpio)
                        || estadoCivilRepository.existsByDescripcionIgnoreCaseAndActivoTrue(limpio))
                .orElse(false);
    }

    private CatalogoItemDto mapearADto(EstadoCivilEntity entity) {
        return CatalogoItemDto.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .descripcion(entity.getDescripcion())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .build();
    }
}
