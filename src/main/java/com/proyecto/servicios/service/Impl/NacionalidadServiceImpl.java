package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogo.NacionalidadEntity;
import com.proyecto.servicios.exception.CatalogoDuplicadoException;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.model.catalogo.CatalogoActualizacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoCreacionDto;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.repositorys.catalogo.NacionalidadRepository;
import com.proyecto.servicios.service.NacionalidadService;
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
public class NacionalidadServiceImpl implements NacionalidadService {

    private final NacionalidadRepository nacionalidadRepository;

    public NacionalidadServiceImpl(NacionalidadRepository nacionalidadRepository) {
        this.nacionalidadRepository = nacionalidadRepository;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<CatalogoItemDto> listarTodos() {
        return nacionalidadRepository.findAll().stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<CatalogoItemDto> listarActivos() {
        return nacionalidadRepository.findByActivoTrue().stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public CatalogoItemDto obtenerPorId(Long id) {
        return nacionalidadRepository.findById(id)
                .map(this::mapearADto)
                .orElseThrow(() -> new CatalogoNoEncontradoException("Elemento de nacionalidad no encontrado con ID " + id));
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public CatalogoItemDto obtenerPorCodigoODescripcion(String valor) {
        String limpio = valor.trim().toUpperCase();
        return nacionalidadRepository.findByCodigoIgnoreCase(limpio)
                .or(() -> nacionalidadRepository.findByDescripcionIgnoreCase(limpio))
                .map(this::mapearADto)
                .orElseThrow(() -> new CatalogoNoEncontradoException("Nacionalidad no encontrada para el valor '" + valor + "'"));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public CatalogoItemDto crear(CatalogoCreacionDto dto) {
        String codigo = dto.getCodigo().trim().toUpperCase();
        String descripcion = dto.getDescripcion().trim().toUpperCase();

        if (nacionalidadRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new CatalogoDuplicadoException("El código de nacionalidad '" + codigo + "' ya está registrado");
        }
        if (nacionalidadRepository.existsByDescripcionIgnoreCase(descripcion)) {
            throw new CatalogoDuplicadoException("La descripción de nacionalidad '" + descripcion + "' ya está registrada");
        }

        NacionalidadEntity entidad = NacionalidadEntity.builder()
                .codigo(codigo)
                .descripcion(descripcion)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        return mapearADto(nacionalidadRepository.save(entidad));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public CatalogoItemDto actualizar(Long id, CatalogoActualizacionDto dto) {
        NacionalidadEntity entidad = nacionalidadRepository.findById(id)
                .orElseThrow(() -> new CatalogoNoEncontradoException("Elemento de nacionalidad no encontrado con ID " + id));

        Optional.ofNullable(dto.getDescripcion())
                .map(String::trim)
                .filter(Predicate.not(String::isBlank))
                .ifPresent(desc -> {
                    String descUpper = desc.toUpperCase();
                    if (!descUpper.equalsIgnoreCase(entidad.getDescripcion()) && nacionalidadRepository.existsByDescripcionIgnoreCase(descUpper)) {
                        throw new CatalogoDuplicadoException("La descripción '" + descUpper + "' ya pertenece a otro registro");
                    }
                    entidad.setDescripcion(descUpper);
                });

        Optional.ofNullable(dto.getActivo()).ifPresent(entidad::setActivo);

        return mapearADto(nacionalidadRepository.save(entidad));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public void desactivar(Long id) {
        NacionalidadEntity entidad = nacionalidadRepository.findById(id)
                .orElseThrow(() -> new CatalogoNoEncontradoException("Elemento de nacionalidad no encontrado con ID " + id));
        entidad.setActivo(false);
        nacionalidadRepository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public boolean esValidoYActivo(String valor) {
        if (valor == null || valor.isBlank()) {
            return false;
        }
        String limpio = valor.trim().toUpperCase();
        return nacionalidadRepository.existsByCodigoIgnoreCaseAndActivoTrue(limpio)
                || nacionalidadRepository.existsByDescripcionIgnoreCaseAndActivoTrue(limpio);
    }

    private CatalogoItemDto mapearADto(NacionalidadEntity entity) {
        return CatalogoItemDto.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .descripcion(entity.getDescripcion())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .build();
    }
}
