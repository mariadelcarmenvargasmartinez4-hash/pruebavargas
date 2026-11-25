package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.entity.catalogo.EntidadFederativaEntity;
import com.proyecto.servicios.exception.CatalogoDuplicadoException;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.model.catalogo.CatalogoActualizacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoConsultaFiltroRecord;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.repositorys.catalogo.EntidadFederativaRepository;
import com.proyecto.servicios.service.EntidadFederativaService;
import com.proyecto.servicios.util.DeclarativeAssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EntidadFederativaServiceImpl implements EntidadFederativaService {
    private static final String MSG_ENTIDAD_NO_ENCONTRADA = "Elemento de entidad federativa no encontrado con ID ";

    private final EntidadFederativaRepository entidadFederativaRepository;

    public EntidadFederativaServiceImpl(EntidadFederativaRepository entidadFederativaRepository) {
        this.entidadFederativaRepository = entidadFederativaRepository;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public PaginaResponseRecord<CatalogoItemDto> buscarPaginado(CatalogoConsultaFiltroRecord filtro) {
        Sort sort = Sort.by(Sort.Direction.fromString(filtro.sortDir()), filtro.sortBy());
        Pageable pageable = PageRequest.of(filtro.page(), filtro.size(), sort);

        Specification<EntidadFederativaEntity> spec = (root, query, cb) -> {
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

        Page<CatalogoItemDto> page = entidadFederativaRepository.findAll(spec, pageable).map(this::mapearADto);
        return PaginaResponseRecord.de(page);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<CatalogoItemDto> listarTodos() {
        return entidadFederativaRepository.findAll().stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<CatalogoItemDto> listarActivos() {
        return entidadFederativaRepository.findByActivoTrue().stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public CatalogoItemDto obtenerPorId(Long id) {
        return entidadFederativaRepository.findById(id)
                .map(this::mapearADto)
                .orElseThrow(() -> new CatalogoNoEncontradoException(MSG_ENTIDAD_NO_ENCONTRADA + id));
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public CatalogoItemDto obtenerPorCodigoODescripcion(String valor) {
        return entidadFederativaRepository.findByCodigoIgnoreCase(valor.trim())
                .or(() -> entidadFederativaRepository.findByDescripcionIgnoreCase(valor.trim()))
                .map(this::mapearADto)
                .orElseThrow(() -> new CatalogoNoEncontradoException("Entidad federativa no encontrada con valor: " + valor));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public CatalogoItemDto crear(CatalogoCreacionDto dto) {
        String codigo = dto.getCodigo().trim().toUpperCase();
        String descripcion = dto.getDescripcion().trim().toUpperCase();

        DeclarativeAssertUtil.prohibir(entidadFederativaRepository.existsByCodigoIgnoreCase(codigo),
                () -> new CatalogoDuplicadoException("El código '" + codigo + "' ya existe en el catálogo de entidades federativas"));
        DeclarativeAssertUtil.prohibir(entidadFederativaRepository.existsByDescripcionIgnoreCase(descripcion),
                () -> new CatalogoDuplicadoException("La descripción '" + descripcion + "' ya existe en el catálogo de entidades federativas"));

        EntidadFederativaEntity entity = EntidadFederativaEntity.builder()
                .codigo(codigo)
                .descripcion(descripcion)
                .activo(true)
                .build();

        return mapearADto(entidadFederativaRepository.save(entity));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public CatalogoItemDto actualizar(Long id, CatalogoActualizacionDto dto) {
        EntidadFederativaEntity entity = entidadFederativaRepository.findById(id)
                .orElseThrow(() -> new CatalogoNoEncontradoException(MSG_ENTIDAD_NO_ENCONTRADA + id));

        Optional.ofNullable(dto.getDescripcion())
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(desc -> !desc.equalsIgnoreCase(entity.getDescripcion()))
                .ifPresent(desc -> {
                    DeclarativeAssertUtil.prohibir(entidadFederativaRepository.existsByDescripcionIgnoreCase(desc),
                            () -> new CatalogoDuplicadoException("La descripción '" + desc + "' ya existe"));
                    entity.setDescripcion(desc);
                });

        Optional.ofNullable(dto.getActivo()).ifPresent(entity::setActivo);

        return mapearADto(entidadFederativaRepository.save(entity));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public void desactivar(Long id) {
        EntidadFederativaEntity entity = entidadFederativaRepository.findById(id)
                .orElseThrow(() -> new CatalogoNoEncontradoException(MSG_ENTIDAD_NO_ENCONTRADA + id));
        entity.setActivo(false);
        entidadFederativaRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public boolean esValidoYActivo(String valor) {
        return Optional.ofNullable(valor)
                .map(String::trim)
                .filter(Predicate.not(String::isBlank))
                .map(v -> entidadFederativaRepository.existsByCodigoIgnoreCaseAndActivoTrue(v)
                        || entidadFederativaRepository.existsByDescripcionIgnoreCaseAndActivoTrue(v))
                .orElse(false);
    }

    private CatalogoItemDto mapearADto(EntidadFederativaEntity entity) {
        return CatalogoItemDto.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .descripcion(entity.getDescripcion())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .build();
    }
}
