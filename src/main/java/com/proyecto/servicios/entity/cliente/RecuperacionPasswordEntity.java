package com.proyecto.servicios.entity.cliente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "recuperaciones_password")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecuperacionPasswordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioAccesoEntity usuario;

    @Column(name = "codigo_hash", nullable = false, length = 120)
    private String codigoHash;

    @Column(name = "reset_token_hash", length = 120)
    private String resetTokenHash;

    @Column(name = "intentos_fallidos", nullable = false)
    @Builder.Default
    private Integer intentosFallidos = 0;

    @Column(name = "max_intentos", nullable = false)
    @Builder.Default
    private Integer maxIntentos = 3;

    @Column(name = "verificado", nullable = false)
    @Builder.Default
    private Boolean verificado = false;

    @Column(name = "usado", nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_expiracion_reset")
    private LocalDateTime fechaExpiracionReset;

    @PrePersist
    public void prePersist() {
        this.fechaCreacion = Optional.ofNullable(this.fechaCreacion).orElseGet(LocalDateTime::now);
        this.intentosFallidos = Optional.ofNullable(this.intentosFallidos).orElse(0);
        this.maxIntentos = Optional.ofNullable(this.maxIntentos).orElse(3);
        this.verificado = Optional.ofNullable(this.verificado).orElse(false);
        this.usado = Optional.ofNullable(this.usado).orElse(false);
    }
}
