package com.proyecto.servicios.entity.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(name = "catalogo_nacionalidades")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NacionalidadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(name = "descripcion", nullable = false, unique = true, length = 100)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {
        this.codigo = Optional.ofNullable(this.codigo).map(String::trim).map(String::toUpperCase).orElse(null);
        this.descripcion = Optional.ofNullable(this.descripcion).map(String::trim).map(String::toUpperCase).orElse(null);
        this.activo = Optional.ofNullable(this.activo).orElse(true);
        this.fechaCreacion = LocalDateTime.now();
    }
}

