package com.proyecto.servicios.repositorys.catalogo;

import com.proyecto.servicios.entity.catalogo.SexoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SexoRepository extends JpaRepository<SexoEntity, Long> {

    List<SexoEntity> findByActivoTrue();

    Optional<SexoEntity> findByCodigoIgnoreCase(String codigo);

    Optional<SexoEntity> findByDescripcionIgnoreCase(String descripcion);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByDescripcionIgnoreCase(String descripcion);

    boolean existsByCodigoIgnoreCaseAndActivoTrue(String codigo);

    boolean existsByDescripcionIgnoreCaseAndActivoTrue(String descripcion);
}
