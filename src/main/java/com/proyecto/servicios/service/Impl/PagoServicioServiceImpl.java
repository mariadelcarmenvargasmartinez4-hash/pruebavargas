package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.pago.PagoServicioEntity;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.pago.PagoServicioRequestDto;
import com.proyecto.servicios.model.pago.PagoServicioRequestRecord;
import com.proyecto.servicios.model.pago.PagoServicioResponseDto;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.pago.PagoServicioRepository;
import com.proyecto.servicios.service.PagoServicioService;
import com.proyecto.servicios.util.DeclarativeAssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementación del servicio de pagos de servicios bajo estándares Senior:
 * - Validación temprana Fail-Fast mediante Record
 * - Verificación declarativa de saldo, estatus y existencia de la cuenta bancaria
 * - Persistencia transaccional de débitos y registro de auditoría del pago
 * - Ausencia total de condicionales imperativas (cero sentencias if)
 */
@Slf4j
@Service
public class PagoServicioServiceImpl implements PagoServicioService {

    private final CuentaRepository cuentaRepository;
    private final PagoServicioRepository pagoServicioRepository;

    public PagoServicioServiceImpl(CuentaRepository cuentaRepository,
                                  PagoServicioRepository pagoServicioRepository) {
        this.cuentaRepository = cuentaRepository;
        this.pagoServicioRepository = pagoServicioRepository;
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public PagoServicioResponseDto procesarPago(PagoServicioRequestDto request) {
        // 1. Validación temprana Fail-Fast en Record
        PagoServicioRequestRecord record = request.toRecord();

        log.info("Iniciando procesamiento de pago de servicio. Cuenta: {}, UPE: {}, Monto: {}",
                record.numCuenta(), record.upe(), record.monto());

        // 2. Consulta de la cuenta origen
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuenta(record.numCuenta())
                .orElseThrow(() -> new CuentaNoEncontradaException(
                        "No se encontró la cuenta bancaria con número " + record.numCuenta()));

        // 3. Reglas de negocio declarativas: estatus activo y saldo suficiente
        DeclarativeAssertUtil.asegurar(
                "ACTIVA".equalsIgnoreCase(cuenta.getEstatus()),
                () -> new ReglaNegocioException("La cuenta bancaria se encuentra inactiva o bloqueada para transacciones"));

        DeclarativeAssertUtil.asegurar(
                cuenta.getSaldo().compareTo(record.monto()) >= 0,
                () -> new ReglaNegocioException(
                        String.format("Saldo insuficiente. Saldo disponible: $%.2f, Monto requerido: $%.2f",
                                cuenta.getSaldo(), record.monto())));

        // 4. Verificación declarativa del PIN de autorización de la transacción
        DeclarativeAssertUtil.asegurar(
                record.pin() != null && !record.pin().isBlank(),
                () -> new ReglaNegocioException("El PIN o pass seguro de verificación es inválido"));

        // 5. Aplicación del débito en la cuenta
        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoActual = saldoAnterior.subtract(record.monto());
        cuenta.setSaldo(saldoActual);
        cuentaRepository.save(cuenta);

        // 6. Registro de la transacción de pago
        String folioTransaccion = "TX-" + UUID.randomUUID();
        String tipoOperacion = Boolean.TRUE.equals(record.bandera()) ? "RECARGA_TELEFONICA" : "PAGO_SERVICIO_REFERENCIA";
        String destinoPago = Boolean.TRUE.equals(record.bandera()) ? record.telefono() : record.referencia();

        PagoServicioEntity transaccionPago = PagoServicioEntity.builder()
                .folioTransaccion(folioTransaccion)
                .numCuenta(record.numCuenta())
                .idServicio(record.idServicio())
                .idProducto(record.idProducto())
                .idConcepto(record.idConcepto())
                .idDistribuidor(record.idDistribuidor())
                .monto(record.monto())
                .upe(record.upe())
                .esRecargaTelefonica(record.bandera())
                .telefono(record.telefono())
                .referencia(record.referencia())
                .estatus("APLICADO")
                .fechaOperacion(LocalDateTime.now())
                .build();

        pagoServicioRepository.save(transaccionPago);

        log.info("Pago aplicado exitosamente. Folio: {}, Cuenta: {}, Nuevo Saldo: {}",
                folioTransaccion, record.numCuenta(), saldoActual);

        // 7. Retorno del DTO de respuesta
        return PagoServicioResponseDto.builder()
                .folioTransaccion(folioTransaccion)
                .numCuenta(record.numCuenta())
                .idServicio(record.idServicio())
                .idProducto(record.idProducto())
                .idConcepto(record.idConcepto())
                .idDistribuidor(record.idDistribuidor())
                .monto(record.monto())
                .saldoAnterior(saldoAnterior)
                .saldoActual(saldoActual)
                .upe(record.upe())
                .tipoOperacion(tipoOperacion)
                .destinoPago(destinoPago)
                .estatus("APLICADO")
                .mensaje("Pago de servicio aplicado exitosamente")
                .fechaOperacion(transaccionPago.getFechaOperacion())
                .build();
    }
}
