package com.proyecto.servicios.repositorys.catalogo;

import com.proyecto.servicios.entity.catalogo.NacionalidadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NacionalidadRepository extends JpaRepository<NacionalidadEntity, Long> {

    List<NacionalidadEntity> findByActivoTrue();

    Optional<NacionalidadEntity> findByCodigoIgnoreCase(String codigo);

    Optional<NacionalidadEntity> findByDescripcionIgnoreCase(String descripcion);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByDescripcionIgnoreCase(String descripcion);

    boolean existsByCodigoIgnoreCaseAndActivoTrue(String codigo);

    boolean existsByDescripcionIgnoreCaseAndActivoTrue(String descripcion);
}
