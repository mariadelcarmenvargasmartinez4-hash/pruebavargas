package com.proyecto.servicios.util;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.ProductoDocument;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxResponse;
import com.proyecto.servicios.model.pago.PagoConciliacionRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioRequestDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Test Data Builder para crear entidades y DTOs del módulo de Pagos y Gestopago
 * de forma limpia y reutilizable para pruebas unitarias.
 */
public final class PagoTestDataBuilder {

    private PagoTestDataBuilder() {
    }

    /**
     * Builder preconfigurado para PagoServicioRequestDto con datos válidos por defecto.
     */
    public static PagoServicioRequestDto.PagoServicioRequestDtoBuilder unPagoRequest() {
        return PagoServicioRequestDto.builder()
                .numCuenta("1234567890123456")
                .idServicio(1L)
                .idProducto(101L)
                .monto(new BigDecimal("150.00"))
                .upe("1234567890")
                .bandera(true)
                .telefono("5598765432")
                .pin(TestCredentials.testPin());
    }

    /**
     * Instancia un PagoServicioRequestDto válido por defecto.
     */
    public static PagoServicioRequestDto requestValido() {
        return unPagoRequest().build();
    }

    /**
     * Retorna una CuentaEntity configurada para pruebas de pago con saldo suficiente.
     */
    public static CuentaEntity cuentaParaPago(BigDecimal saldo) {
        return CuentaEntity.builder()
                .id(1L)
                .numeroCuenta("1234567890123456")
                .saldo(saldo != null ? saldo : new BigDecimal("500.00"))
                .estatus("ACTIVA")
                .pinHash(TestCredentials.testPinHash())
                .fechaApertura(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }

    /**
     * Retorna un GestoPagoSendTxResponse exitoso (código 01).
     */
    public static GestoPagoSendTxResponse respuestaExitosaProveedor() {
        return GestoPagoSendTxResponse.builder()
                .codigo("01")
                .mensaje("Transacción Exitosa")
                .folioTransaccion("GP-TX-999")
                .autorizacion("AUTH-12345")
                .build();
    }

    /**
     * Retorna un GestoPagoSendTxResponse con rechazo/error del proveedor.
     */
    public static GestoPagoSendTxResponse respuestaRechazoProveedor(String codigo, String mensaje) {
        return GestoPagoSendTxResponse.builder()
                .codigo(codigo != null ? codigo : "99")
                .mensaje(mensaje != null ? mensaje : "Operación declinada")
                .folioTransaccion("GP-TX-ERR")
                .autorizacion("RECHAZADA")
                .build();
    }

    /**
     * Retorna un GestoPagoToken activo y vigente.
     */
    public static GestoPagoToken tokenActivo() {
        return GestoPagoToken.builder()
                .token("VALID_TOKEN")
                .activo(true)
                .expiresIn(3600L)
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }

    /**
     * Retorna un ProductoDocument de MongoDB para pruebas.
     */
    public static ProductoDocument productoDocumentValido() {
        return ProductoDocument.builder()
                .id("prod-101")
                .idProducto(101)
                .idServicio(1)
                .producto("Paquete Amigo 100")
                .precio("100.00")
                .build();
    }

    /**
     * Retorna un PagoConciliacionRequestDto válido.
     */
    public static PagoConciliacionRequestDto conciliacionRequestValido() {
        return PagoConciliacionRequestDto.builder()
                .upe("1234567890")
                .build();
    }
}
