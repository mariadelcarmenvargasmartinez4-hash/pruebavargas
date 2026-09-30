package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Repositorio JPA para operaciones con la entidad ClienteEntity
@Repository
public interface ClienteRepository extends JpaRepository<ClienteEntity, Long> {

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreoElectronico(String correoElectronico);

    Optional<ClienteEntity> findByCurp(String curp);

    Optional<ClienteEntity> findByRfc(String rfc);

    Optional<ClienteEntity> findByCorreoElectronico(String correoElectronico);
}
