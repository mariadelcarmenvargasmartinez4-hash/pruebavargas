package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Repositorio JPA para operaciones con la entidad UsuarioAccesoEntity
@Repository
public interface UsuarioAccesoRepository extends JpaRepository<UsuarioAccesoEntity, Long> {

    boolean existsByUsername(String username);

    boolean existsByClienteId(Long clienteId);

    Optional<UsuarioAccesoEntity> findByUsername(String username);

    Optional<UsuarioAccesoEntity> findByClienteId(Long clienteId);

    List<UsuarioAccesoEntity> findByActivoTrue();

    List<UsuarioAccesoEntity> findByActivoFalse();

    List<UsuarioAccesoEntity> findByUsernameContainingIgnoreCase(String username);

    List<UsuarioAccesoEntity> findByUsernameContainingIgnoreCaseAndActivo(String username, Boolean activo);
}
