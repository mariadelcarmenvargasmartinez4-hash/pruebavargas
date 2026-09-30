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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Entidad JPA para cuentas bancarias asociadas a clientes
@Entity
@Table(name = "cuentas_bancarias")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaBancariaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    @Column(name = "numero_cuenta", nullable = false, unique = true, length = 16)
    private String numeroCuenta;

    @Column(name = "clabe", nullable = false, unique = true, length = 18)
    private String clabe;

    @Column(name = "tipo_cuenta", nullable = false, length = 30)
    @Builder.Default
    private String tipoCuenta = "DEBITO";

    @Column(name = "saldo", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(name = "moneda", nullable = false, length = 3)
    @Builder.Default
    private String moneda = "MXN";

    @Column(name = "estatus", nullable = false, length = 20)
    @Builder.Default
    private String estatus = "ACTIVA";

    @Column(name = "fecha_apertura", nullable = false, updatable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void prePersist() {
        this.fechaApertura = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
        if (this.tipoCuenta == null) {
            this.tipoCuenta = "DEBITO";
        }
        if (this.saldo == null) {
            this.saldo = BigDecimal.ZERO;
        }
        if (this.moneda == null) {
            this.moneda = "MXN";
        }
        if (this.estatus == null) {
            this.estatus = "ACTIVA";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
