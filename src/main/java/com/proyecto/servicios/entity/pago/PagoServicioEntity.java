package com.proyecto.servicios.entity.pago;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Entidad JPA para el registro de transacciones de pago de servicios y recargas telefónicas.
 */
@Entity
@Table(name = "pagos_servicios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoServicioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folio_transaccion", nullable = false, unique = true, length = 36)
    private String folioTransaccion;

    @Column(name = "num_cuenta", nullable = false, length = 16)
    private String numCuenta;

    @Column(name = "id_servicio", nullable = false)
    private Long idServicio;

    @Column(name = "id_producto", nullable = false)
    private Long idProducto;

    @Column(name = "id_concepto")
    private Long idConcepto;

    @Column(name = "id_distribuidor")
    private Integer idDistribuidor;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "upe", nullable = false, length = 10)
    private String upe;

    @Column(name = "es_recarga_telefonica", nullable = false)
    private Boolean esRecargaTelefonica;

    @Column(name = "telefono", length = 15)
    private String telefono;

    @Column(name = "referencia", length = 50)
    private String referencia;

    @Column(name = "estatus", nullable = false, length = 20)
    @Builder.Default
    private String estatus = "APLICADO";

    @Column(name = "fecha_operacion", nullable = false, updatable = false)
    private LocalDateTime fechaOperacion;

    @PrePersist
    public void prePersist() {
        this.fechaOperacion = LocalDateTime.now();
        this.estatus = Optional.ofNullable(this.estatus).orElse("APLICADO");
    }
}
