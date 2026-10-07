package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogo.EstadoCivilEntity;
import com.proyecto.servicios.exception.CatalogoDuplicadoException;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.model.catalogo.CatalogoActualizacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.repositorys.catalogo.EstadoCivilRepository;
import com.proyecto.servicios.service.EstadoCivilService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        String codigo = dto.getCodigo().trim().toUpperCase();
        String descripcion = dto.getDescripcion().trim().toUpperCase();

        if (estadoCivilRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new CatalogoDuplicadoException("El código de estado civil '" + codigo + "' ya está registrado");
        }
        if (estadoCivilRepository.existsByDescripcionIgnoreCase(descripcion)) {
            throw new CatalogoDuplicadoException("La descripción de estado civil '" + descripcion + "' ya está registrada");
        }

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
        EstadoCivilEntity entidad = estadoCivilRepository.findById(id)
                .orElseThrow(() -> new CatalogoNoEncontradoException("Elemento de estado civil no encontrado con ID " + id));

        Optional.ofNullable(dto.getDescripcion())
                .map(String::trim)
                .filter(Predicate.not(String::isBlank))
                .ifPresent(desc -> {
                    String descUpper = desc.toUpperCase();
                    if (!descUpper.equalsIgnoreCase(entidad.getDescripcion()) && estadoCivilRepository.existsByDescripcionIgnoreCase(descUpper)) {
                        throw new CatalogoDuplicadoException("La descripción '" + descUpper + "' ya pertenece a otro registro");
                    }
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
        if (valor == null || valor.isBlank()) {
            return false;
        }
        String limpio = valor.trim().toUpperCase();
        return estadoCivilRepository.existsByCodigoIgnoreCaseAndActivoTrue(limpio)
                || estadoCivilRepository.existsByDescripcionIgnoreCaseAndActivoTrue(limpio);
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
