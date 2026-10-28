package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Repositorio JPA para operaciones con la entidad CuentaEntity
@Repository
public interface CuentaRepository extends JpaRepository<CuentaEntity, Long>, JpaSpecificationExecutor<CuentaEntity> {

    boolean existsByNumeroCuenta(String numeroCuenta);

    boolean existsByClabe(String clabe);

    Optional<CuentaEntity> findByNumeroCuenta(String numeroCuenta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CuentaEntity c WHERE c.numeroCuenta = :numeroCuenta")
    Optional<CuentaEntity> findByNumeroCuentaWithLock(@Param("numeroCuenta") String numeroCuenta);

    Optional<CuentaEntity> findByClabe(String clabe);

    List<CuentaEntity> findByClienteId(Long clienteId);

    // Consultar cuentas por estatus (por ejemplo: ACTIVA, INACTIVA, CANCELADA)
    List<CuentaEntity> findByEstatusIgnoreCase(String estatus);

    // Consultar cuentas activas
    List<CuentaEntity> findByEstatusIgnoreCaseOrderByIdDesc(String estatus);
}
