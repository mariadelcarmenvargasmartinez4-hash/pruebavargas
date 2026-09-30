package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.CuentaBancariaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Repositorio JPA para operaciones con la entidad CuentaBancariaEntity
@Repository
public interface CuentaBancariaRepository extends JpaRepository<CuentaBancariaEntity, Long> {

    boolean existsByNumeroCuenta(String numeroCuenta);

    boolean existsByClabe(String clabe);

    Optional<CuentaBancariaEntity> findByNumeroCuenta(String numeroCuenta);

    Optional<CuentaBancariaEntity> findByClabe(String clabe);

    List<CuentaBancariaEntity> findByClienteId(Long clienteId);
}
