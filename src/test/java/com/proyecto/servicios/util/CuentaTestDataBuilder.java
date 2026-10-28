package com.proyecto.servicios.util;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.model.cliente.CuentaBancariaDto;
import com.proyecto.servicios.model.cuenta.CuentaCreacionRequestDto;
import com.proyecto.servicios.model.cuenta.SaldoCuentaDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Test Data Builder para crear entidades y DTOs de Cuenta Bancaria
 * de forma limpia y reutilizable para pruebas unitarias.
 */
public final class CuentaTestDataBuilder {

    private CuentaTestDataBuilder() {
    }

    /**
     * Retorna un builder preconfigurado para CuentaEntity.
     */
    public static CuentaEntity.CuentaEntityBuilder unaCuentaEntity() {
        return CuentaEntity.builder()
                .id(10L)
                .numeroCuenta("4815162342")
                .clabe("012180004815162342")
                .tipoCuenta("DEBITO")
                .saldo(new BigDecimal("2000.00"))
                .moneda("MXN")
                .estatus("ACTIVA")
                .pinHash(TestCredentials.testPinHash())
                .fechaApertura(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now());
    }

    /**
     * Instancia una CuentaEntity válida asociada al cliente.
     */
    public static CuentaEntity cuentaValida(ClienteEntity cliente) {
        return unaCuentaEntity()
                .cliente(cliente != null ? cliente : ClienteTestDataBuilder.clienteValido())
                .build();
    }

    /**
     * Builder preconfigurado para CuentaCreacionRequestDto.
     */
    public static CuentaCreacionRequestDto.CuentaCreacionRequestDtoBuilder unaCuentaCreacionRequest() {
        return CuentaCreacionRequestDto.builder()
                .clienteId(1L)
                .saldoInicial(new BigDecimal("1000.00"))
                .tipoCuenta("DEBITO")
                .moneda("MXN");
    }

    /**
     * Instancia un CuentaCreacionRequestDto válido.
     */
    public static CuentaCreacionRequestDto creacionRequestValido() {
        return unaCuentaCreacionRequest().build();
    }

    /**
     * Retorna un CuentaBancariaDto estándar de respuesta.
     */
    public static CuentaBancariaDto cuentaBancariaDto() {
        return CuentaBancariaDto.builder()
                .idCuenta(10L)
                .numeroCuenta("4815162342")
                .clabe("012180004815162342")
                .tipoCuenta("DEBITO")
                .saldo(new BigDecimal("2000.00"))
                .moneda("MXN")
                .estatus("ACTIVA")
                .build();
    }

    /**
     * Retorna un SaldoCuentaDto estándar.
     */
    public static SaldoCuentaDto saldoCuentaDto() {
        return SaldoCuentaDto.builder()
                .numeroCuenta("4815162342")
                .saldo(new BigDecimal("2000.00"))
                .moneda("MXN")
                .build();
    }
}
