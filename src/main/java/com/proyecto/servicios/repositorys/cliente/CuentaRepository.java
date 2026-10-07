package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Repositorio JPA para operaciones con la entidad CuentaEntity
@Repository
public interface CuentaRepository extends JpaRepository<CuentaEntity, Long> {

    boolean existsByNumeroCuenta(String numeroCuenta);

    boolean existsByClabe(String clabe);

    Optional<CuentaEntity> findByNumeroCuenta(String numeroCuenta);

    Optional<CuentaEntity> findByClabe(String clabe);

    List<CuentaEntity> findByClienteId(Long clienteId);

    // Consultar cuentas por estatus (por ejemplo: ACTIVA, INACTIVA, CANCELADA)
    List<CuentaEntity> findByEstatusIgnoreCase(String estatus);

    // Consultar cuentas activas
    List<CuentaEntity> findByEstatusIgnoreCaseOrderByIdDesc(String estatus);
}
