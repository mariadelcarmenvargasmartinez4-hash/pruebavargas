package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogo.SexoEntity;
import com.proyecto.servicios.exception.CatalogoDuplicadoException;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.model.catalogo.CatalogoActualizacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoConsultaFiltroRecord;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.repositorys.catalogo.SexoRepository;
import com.proyecto.servicios.service.SexoService;
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
public class SexoServiceImpl implements SexoService {

    private final SexoRepository sexoRepository;

    public SexoServiceImpl(SexoRepository sexoRepository) {
        this.sexoRepository = sexoRepository;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public PaginaResponseRecord<CatalogoItemDto> buscarPaginado(CatalogoConsultaFiltroRecord filtro) {
        Sort sort = Sort.by(Sort.Direction.fromString(filtro.sortDir()), filtro.sortBy());
        Pageable pageable = PageRequest.of(filtro.page(), filtro.size(), sort);

        Specification<SexoEntity> spec = (root, query, cb) -> {
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

        Page<CatalogoItemDto> page = sexoRepository.findAll(spec, pageable).map(this::mapearADto);
        return PaginaResponseRecord.de(page);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<CatalogoItemDto> listarTodos() {
        return sexoRepository.findAll().stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<CatalogoItemDto> listarActivos() {
        return sexoRepository.findByActivoTrue().stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public CatalogoItemDto obtenerPorId(Long id) {
        return sexoRepository.findById(id)
                .map(this::mapearADto)
                .orElseThrow(() -> new CatalogoNoEncontradoException("Elemento de sexo no encontrado con ID " + id));
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public CatalogoItemDto obtenerPorCodigoODescripcion(String valor) {
        String limpio = valor.trim().toUpperCase();
        return sexoRepository.findByCodigoIgnoreCase(limpio)
                .or(() -> sexoRepository.findByDescripcionIgnoreCase(limpio))
                .map(this::mapearADto)
                .orElseThrow(() -> new CatalogoNoEncontradoException("Sexo no encontrado para el valor '" + valor + "'"));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public CatalogoItemDto crear(CatalogoCreacionDto dto) {
        dto.toRecord(); // Validación temprana Fail-Fast en Record
        String codigo = dto.getCodigo().trim().toUpperCase();
        String descripcion = dto.getDescripcion().trim().toUpperCase();

        DeclarativeAssertUtil.prohibir(sexoRepository.existsByCodigoIgnoreCase(codigo),
                () -> new CatalogoDuplicadoException("El código de sexo '" + codigo + "' ya está registrado"));

        DeclarativeAssertUtil.prohibir(sexoRepository.existsByDescripcionIgnoreCase(descripcion),
                () -> new CatalogoDuplicadoException("La descripción de sexo '" + descripcion + "' ya está registrada"));

        SexoEntity entidad = SexoEntity.builder()
                .codigo(codigo)
                .descripcion(descripcion)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        return mapearADto(sexoRepository.save(entidad));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public CatalogoItemDto actualizar(Long id, CatalogoActualizacionDto dto) {
        dto.toRecord(); // Validación temprana Fail-Fast en Record
        SexoEntity entidad = sexoRepository.findById(id)
                .orElseThrow(() -> new CatalogoNoEncontradoException("Elemento de sexo no encontrado con ID " + id));

        Optional.ofNullable(dto.getDescripcion())
                .map(String::trim)
                .filter(Predicate.not(String::isBlank))
                .ifPresent(desc -> {
                    String descUpper = desc.toUpperCase();
                    DeclarativeAssertUtil.prohibir(!descUpper.equalsIgnoreCase(entidad.getDescripcion()) && sexoRepository.existsByDescripcionIgnoreCase(descUpper),
                            () -> new CatalogoDuplicadoException("La descripción '" + descUpper + "' ya pertenece a otro registro"));
                    entidad.setDescripcion(descUpper);
                });

        Optional.ofNullable(dto.getActivo()).ifPresent(entidad::setActivo);

        return mapearADto(sexoRepository.save(entidad));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public void desactivar(Long id) {
        SexoEntity entidad = sexoRepository.findById(id)
                .orElseThrow(() -> new CatalogoNoEncontradoException("Elemento de sexo no encontrado con ID " + id));
        entidad.setActivo(false);
        sexoRepository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public boolean esValidoYActivo(String valor) {
        return Optional.ofNullable(valor)
                .map(String::trim)
                .filter(Predicate.not(String::isBlank))
                .map(String::toUpperCase)
                .map(limpio -> sexoRepository.existsByCodigoIgnoreCaseAndActivoTrue(limpio)
                        || sexoRepository.existsByDescripcionIgnoreCaseAndActivoTrue(limpio))
                .orElse(false);
    }

    private CatalogoItemDto mapearADto(SexoEntity entity) {
        return CatalogoItemDto.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .descripcion(entity.getDescripcion())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .build();
    }
}
