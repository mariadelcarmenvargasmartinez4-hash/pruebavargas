package com.proyecto.servicios.repositorys.catalogo;

import com.proyecto.servicios.entity.catalogo.EstadoCivilEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoCivilRepository extends JpaRepository<EstadoCivilEntity, Long> {

    List<EstadoCivilEntity> findByActivoTrue();

    Optional<EstadoCivilEntity> findByCodigoIgnoreCase(String codigo);

    Optional<EstadoCivilEntity> findByDescripcionIgnoreCase(String descripcion);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByDescripcionIgnoreCase(String descripcion);

    boolean existsByCodigoIgnoreCaseAndActivoTrue(String codigo);

    boolean existsByDescripcionIgnoreCaseAndActivoTrue(String descripcion);
}
