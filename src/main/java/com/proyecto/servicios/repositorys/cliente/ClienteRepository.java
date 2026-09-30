package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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

    // Consultas por filtros de nombre y apellidos
    List<ClienteEntity> findByNombreContainingIgnoreCase(String nombre);

    List<ClienteEntity> findByApellidoPaternoContainingIgnoreCase(String apellidoPaterno);

    List<ClienteEntity> findByApellidoMaternoContainingIgnoreCase(String apellidoMaterno);

    // Consultar clientes activos
    List<ClienteEntity> findByActivoTrue();

    // Consultar clientes registrados en un rango de fechas
    @Query("SELECT c FROM ClienteEntity c WHERE c.fechaCreacion BETWEEN :fechaInicio AND :fechaFin")
    List<ClienteEntity> findByFechaCreacionBetween(@Param("fechaInicio") LocalDateTime fechaInicio,
                                                  @Param("fechaFin") LocalDateTime fechaFin);
}
