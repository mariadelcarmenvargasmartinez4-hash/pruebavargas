package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.cliente.CuentaBancariaDto;
import com.proyecto.servicios.model.cuenta.CuentaActualizacionRequestDto;
import com.proyecto.servicios.model.cuenta.CuentaCreacionRequestDto;
import com.proyecto.servicios.model.cuenta.SaldoCuentaDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.util.GeneradorCuentaBancariaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// Implementacion de la logica de negocio para cuentas bancarias
@Slf4j
@Service
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;
    private final GeneradorCuentaBancariaUtil generadorCuentaUtil;

    @Value("${banco.cuenta.saldo-inicial-default:500.00}")
    private BigDecimal saldoInicialPorDefecto;

    // Inyeccion de dependencias por constructor
    public CuentaServiceImpl(CuentaRepository cuentaRepository,
                             ClienteRepository clienteRepository,
                             GeneradorCuentaBancariaUtil generadorCuentaUtil) {
        this.cuentaRepository = cuentaRepository;
        this.clienteRepository = clienteRepository;
        this.generadorCuentaUtil = generadorCuentaUtil;
    }

    // Crea una cuenta bancaria asociada a un cliente activo
    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public CuentaBancariaDto crearCuenta(CuentaCreacionRequestDto dto) {
        log.info("Creando cuenta bancaria para el cliente ID: {}", dto.getClienteId());

        ClienteEntity cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID " + dto.getClienteId()));

        // Regla de negocio: Solo los clientes activos podran tener cuentas activas
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            log.warn("Intento de apertura de cuenta para cliente inactivo ID: {}", dto.getClienteId());
            throw new ReglaNegocioException("No se puede crear una cuenta bancaria para un cliente inactivo");
        }

        // Regla de negocio: El saldo inicial no puede ser negativo
        if (dto.getSaldoInicial() != null && dto.getSaldoInicial().compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El saldo inicial no puede ser negativo");
        }

        BigDecimal saldoInicial = (dto.getSaldoInicial() != null)
                ? dto.getSaldoInicial()
                : (saldoInicialPorDefecto != null ? saldoInicialPorDefecto : new BigDecimal("500.00"));

        String numeroCuenta = generarNumeroCuentaUnico();
        String clabe = generadorCuentaUtil.generarClabe(numeroCuenta);
        String tipoCuenta = (dto.getTipoCuenta() != null && !dto.getTipoCuenta().isBlank()) ? dto.getTipoCuenta().toUpperCase() : "DEBITO";
        String moneda = (dto.getMoneda() != null && !dto.getMoneda().isBlank()) ? dto.getMoneda().toUpperCase() : "MXN";

        CuentaEntity cuenta = CuentaEntity.builder()
                .cliente(cliente)
                .numeroCuenta(numeroCuenta)
                .clabe(clabe)
                .tipoCuenta(tipoCuenta)
                .saldo(saldoInicial)
                .moneda(moneda)
                .estatus("ACTIVA")
                .build();

        CuentaEntity cuentaGuardada = cuentaRepository.save(cuenta);
        log.info("Cuenta bancaria creada exitosamente con numero: {}", cuentaGuardada.getNumeroCuenta());

        return mapearCuentaADto(cuentaGuardada);
    }

    // Consulta cuenta bancaria por numero de cuenta
    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public CuentaBancariaDto obtenerCuentaPorNumero(String numeroCuenta) {
        String cuentaLimpia = numeroCuenta.trim();
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuenta(cuentaLimpia)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta bancaria no encontrada con numero " + cuentaLimpia));

        return mapearCuentaADto(cuenta);
    }

    // Consulta cuentas bancarias asociadas a un cliente
    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<CuentaBancariaDto> obtenerCuentasPorClienteId(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ClienteNoEncontradoException("Cliente no encontrado con ID " + clienteId);
        }
        return cuentaRepository.findByClienteId(clienteId).stream()
                .map(this::mapearCuentaADto)
                .collect(Collectors.toList());
    }

    // Consulta cuentas bancarias por estatus
    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<CuentaBancariaDto> obtenerCuentasPorEstatus(String estatus) {
        return cuentaRepository.findByEstatusIgnoreCase(estatus.trim()).stream()
                .map(this::mapearCuentaADto)
                .collect(Collectors.toList());
    }

    // Consulta cuentas bancarias activas
    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<CuentaBancariaDto> obtenerCuentasActivas() {
        return cuentaRepository.findByEstatusIgnoreCaseOrderByIdDesc("ACTIVA").stream()
                .map(this::mapearCuentaADto)
                .collect(Collectors.toList());
    }

    // Consulta de saldo disponible de una cuenta
    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public SaldoCuentaDto obtenerSaldoCuenta(String numeroCuenta) {
        String cuentaLimpia = numeroCuenta.trim();
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuenta(cuentaLimpia)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta bancaria no encontrada con numero " + cuentaLimpia));

        ClienteEntity cliente = cuenta.getCliente();
        String titular = cliente.getNombre() + " " + cliente.getApellidoPaterno() + " " + cliente.getApellidoMaterno();

        return SaldoCuentaDto.builder()
                .numeroCuenta(cuenta.getNumeroCuenta())
                .clabe(cuenta.getClabe())
                .titular(titular.trim())
                .saldo(cuenta.getSaldo())
                .moneda(cuenta.getMoneda())
                .estatus(cuenta.getEstatus())
                .fechaConsulta(LocalDateTime.now())
                .build();
    }

    // Actualizacion parcial de una cuenta (saldo, estatus, tipo) sin modificar numero de cuenta
    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public CuentaBancariaDto actualizarCuenta(String numeroCuenta, CuentaActualizacionRequestDto dto) {
        String cuentaLimpia = numeroCuenta.trim();
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuenta(cuentaLimpia)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta bancaria no encontrada con numero " + cuentaLimpia));

        // Regla: No se puede activar una cuenta si el cliente esta inactivo
        if (dto.getEstatus() != null && dto.getEstatus().equalsIgnoreCase("ACTIVA")) {
            if (!Boolean.TRUE.equals(cuenta.getCliente().getActivo())) {
                throw new ReglaNegocioException("Solo los clientes activos pueden tener cuentas activas");
            }
            cuenta.setEstatus("ACTIVA");
        } else if (dto.getEstatus() != null && !dto.getEstatus().isBlank()) {
            cuenta.setEstatus(dto.getEstatus().toUpperCase());
        }

        if (dto.getTipoCuenta() != null && !dto.getTipoCuenta().isBlank()) {
            cuenta.setTipoCuenta(dto.getTipoCuenta().toUpperCase());
        }

        if (dto.getSaldo() != null) {
            if (dto.getSaldo().compareTo(BigDecimal.ZERO) < 0) {
                throw new ReglaNegocioException("El saldo no puede ser negativo");
            }
            cuenta.setSaldo(dto.getSaldo());
        }

        CuentaEntity cuentaActualizada = cuentaRepository.save(cuenta);
        log.info("Cuenta bancaria {} actualizada exitosamente", numeroCuenta);

        return mapearCuentaADto(cuentaActualizada);
    }

    // Genera un numero de cuenta bancaria unico de 10 digitos
    private String generarNumeroCuentaUnico() {
        String cuenta;
        int intentos = 0;
        do {
            cuenta = generadorCuentaUtil.generarNumeroCuenta();
            intentos++;
            if (intentos > 10) {
                throw new ReglaNegocioException("No se pudo generar un numero de cuenta unico");
            }
        } while (cuentaRepository.existsByNumeroCuenta(cuenta));
        return cuenta;
    }

    // Mapea CuentaEntity a CuentaBancariaDto
    private CuentaBancariaDto mapearCuentaADto(CuentaEntity cuenta) {
        return CuentaBancariaDto.builder()
                .idCuenta(cuenta.getId())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .clabe(cuenta.getClabe())
                .tipoCuenta(cuenta.getTipoCuenta())
                .saldo(cuenta.getSaldo())
                .moneda(cuenta.getMoneda())
                .estatus(cuenta.getEstatus())
                .fechaApertura(cuenta.getFechaApertura())
                .build();
    }
}
