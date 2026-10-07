package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogo.SexoEntity;
import com.proyecto.servicios.exception.CatalogoDuplicadoException;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.model.catalogo.CatalogoActualizacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.repositorys.catalogo.SexoRepository;
import com.proyecto.servicios.service.SexoService;
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
public class SexoServiceImpl implements SexoService {

    private final SexoRepository sexoRepository;

    public SexoServiceImpl(SexoRepository sexoRepository) {
        this.sexoRepository = sexoRepository;
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
        String codigo = dto.getCodigo().trim().toUpperCase();
        String descripcion = dto.getDescripcion().trim().toUpperCase();

        if (sexoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new CatalogoDuplicadoException("El código de sexo '" + codigo + "' ya está registrado");
        }
        if (sexoRepository.existsByDescripcionIgnoreCase(descripcion)) {
            throw new CatalogoDuplicadoException("La descripción de sexo '" + descripcion + "' ya está registrada");
        }

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
        SexoEntity entidad = sexoRepository.findById(id)
                .orElseThrow(() -> new CatalogoNoEncontradoException("Elemento de sexo no encontrado con ID " + id));

        Optional.ofNullable(dto.getDescripcion())
                .map(String::trim)
                .filter(Predicate.not(String::isBlank))
                .ifPresent(desc -> {
                    String descUpper = desc.toUpperCase();
                    if (!descUpper.equalsIgnoreCase(entidad.getDescripcion()) && sexoRepository.existsByDescripcionIgnoreCase(descUpper)) {
                        throw new CatalogoDuplicadoException("La descripción '" + descUpper + "' ya pertenece a otro registro");
                    }
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
        if (valor == null || valor.isBlank()) {
            return false;
        }
        String limpio = valor.trim().toUpperCase();
        return sexoRepository.existsByCodigoIgnoreCaseAndActivoTrue(limpio)
                || sexoRepository.existsByDescripcionIgnoreCaseAndActivoTrue(limpio);
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
