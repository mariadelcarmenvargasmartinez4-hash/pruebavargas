package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.DomicilioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Repositorio JPA para operaciones con la entidad DomicilioEntity
@Repository
public interface DomicilioRepository extends JpaRepository<DomicilioEntity, Long> {

    Optional<DomicilioEntity> findByClienteId(Long clienteId);
}
