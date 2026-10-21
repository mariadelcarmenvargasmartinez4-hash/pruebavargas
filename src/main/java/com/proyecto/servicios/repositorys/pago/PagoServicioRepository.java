package com.proyecto.servicios.repositorys.pago;

import com.proyecto.servicios.entity.pago.PagoServicioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para operaciones sobre la entidad PagoServicioEntity.
 */
@Repository
public interface PagoServicioRepository extends JpaRepository<PagoServicioEntity, Long>, JpaSpecificationExecutor<PagoServicioEntity> {

    Optional<PagoServicioEntity> findByFolioTransaccion(String folioTransaccion);

    Optional<PagoServicioEntity> findByUpe(String upe);

    List<PagoServicioEntity> findByNumCuenta(String numCuenta);
}
