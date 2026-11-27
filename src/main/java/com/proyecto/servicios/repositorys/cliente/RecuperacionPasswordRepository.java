package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.RecuperacionPasswordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecuperacionPasswordRepository extends JpaRepository<RecuperacionPasswordEntity, Long> {

    List<RecuperacionPasswordEntity> findByUsuarioIdAndUsadoFalse(Long usuarioId);

    Optional<RecuperacionPasswordEntity> findFirstByUsuarioIdAndUsadoFalseOrderByFechaCreacionDesc(Long usuarioId);
}
