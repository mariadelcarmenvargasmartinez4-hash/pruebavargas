package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.cliente.CuentaBancariaDto;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.model.cuenta.CuentaActualizacionRequestDto;
import com.proyecto.servicios.model.cuenta.CuentaActualizacionRequestRecord;
import com.proyecto.servicios.model.cuenta.CuentaConsultaFiltroRecord;
import com.proyecto.servicios.model.cuenta.CuentaCreacionRequestDto;
import com.proyecto.servicios.model.cuenta.CuentaCreacionRequestRecord;
import com.proyecto.servicios.model.cuenta.SaldoCuentaDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.util.DeclarativeAssertUtil;
import com.proyecto.servicios.util.GeneradorCuentaBancariaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

// Implementación de la lógica de negocio para cuentas bancarias desacoplada y sin declaraciones if
@Slf4j
@Service
public class CuentaServiceImpl implements CuentaService {
    private static final String ESTATUS_ACTIVA = "ACTIVA";
    private static final String ESTATUS_CANCELADA = "CANCELADA";
    private static final String MSG_CUENTA_NO_ENCONTRADA = "Cuenta bancaria no encontrada con número ";


    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;
    private final GeneradorCuentaBancariaUtil generadorCuentaUtil;

    @Value("${banco.cuenta.saldo-inicial-default:500.00}")
    private BigDecimal saldoInicialPorDefecto;

    public CuentaServiceImpl(CuentaRepository cuentaRepository,
                             ClienteRepository clienteRepository,
                             GeneradorCuentaBancariaUtil generadorCuentaUtil) {
        this.cuentaRepository = cuentaRepository;
        this.clienteRepository = clienteRepository;
        this.generadorCuentaUtil = generadorCuentaUtil;
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public CuentaBancariaDto crearCuenta(CuentaCreacionRequestDto dto) {
        // Validación Fail-Fast en Record
        CuentaCreacionRequestRecord.de(dto);

        log.info("Creando cuenta bancaria para el cliente ID: {}", dto.getClienteId());

        ClienteEntity cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID " + dto.getClienteId()));

        // Regla declarativa: Solo los clientes activos podrán tener cuentas activas
        DeclarativeAssertUtil.asegurar(Boolean.TRUE.equals(cliente.getActivo()),
                "No se puede crear una cuenta bancaria para un cliente inactivo");

        BigDecimal saldoInicial = Optional.ofNullable(dto.getSaldoInicial())
                .orElseGet(() -> Optional.ofNullable(saldoInicialPorDefecto).orElse(new BigDecimal("500.00")));

        String numeroCuenta = generarNumeroCuentaUnico();
        String clabe = generadorCuentaUtil.generarClabe(numeroCuenta);
        String tipoCuenta = Optional.ofNullable(dto.getTipoCuenta())
                .filter(Predicate.not(String::isBlank))
                .map(String::toUpperCase)
                .orElse("DEBITO");
        String moneda = Optional.ofNullable(dto.getMoneda())
                .filter(Predicate.not(String::isBlank))
                .map(String::toUpperCase)
                .orElse("MXN");

        CuentaEntity cuenta = CuentaEntity.builder()
                .cliente(cliente)
                .numeroCuenta(numeroCuenta)
                .clabe(clabe)
                .tipoCuenta(tipoCuenta)
                .saldo(saldoInicial)
                .moneda(moneda)
                .estatus(ESTATUS_ACTIVA)
                .build();

        CuentaEntity cuentaGuardada = cuentaRepository.save(cuenta);
        log.info("Cuenta bancaria creada exitosamente con número: {}", cuentaGuardada.getNumeroCuenta());

        return mapearCuentaADto(cuentaGuardada);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public CuentaBancariaDto obtenerCuentaPorNumero(String numeroCuenta) {
        String cuentaLimpia = numeroCuenta.trim();
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuenta(cuentaLimpia)
                .orElseThrow(() -> new CuentaNoEncontradaException(MSG_CUENTA_NO_ENCONTRADA + cuentaLimpia));

        return mapearCuentaADto(cuenta);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public PaginaResponseRecord<CuentaBancariaDto> buscarCuentasPaginadas(CuentaConsultaFiltroRecord filtro) {
        Sort sort = Sort.by(Sort.Direction.fromString(filtro.sortDir()), filtro.sortBy());
        Pageable pageable = PageRequest.of(filtro.page(), filtro.size(), sort);

        Specification<CuentaEntity> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            Optional.ofNullable(filtro.numeroCuenta())
                    .filter(Predicate.not(String::isBlank))
                    .map(nc -> cb.equal(root.get("numeroCuenta"), nc))
                    .ifPresent(predicates::add);

            Optional.ofNullable(filtro.clienteId())
                    .map(cid -> cb.equal(root.get("cliente").get("id"), cid))
                    .ifPresent(predicates::add);

            Optional.ofNullable(filtro.estatus())
                    .filter(Predicate.not(String::isBlank))
                    .map(est -> cb.equal(cb.upper(root.get("estatus")), est))
                    .ifPresent(predicates::add);

            Optional.ofNullable(filtro.soloActivas())
                    .filter(Boolean::booleanValue)
                    .map(act -> cb.equal(cb.upper(root.get("estatus")), ESTATUS_ACTIVA))
                    .ifPresent(predicates::add);

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<CuentaBancariaDto> dtoPage = cuentaRepository.findAll(spec, pageable)
                .map(this::mapearCuentaADto);

        return PaginaResponseRecord.de(dtoPage);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public SaldoCuentaDto obtenerSaldoCuenta(String numeroCuenta) {
        CuentaBancariaDto cuenta = obtenerCuentaPorNumero(numeroCuenta);

        return SaldoCuentaDto.builder()
                .numeroCuenta(cuenta.getNumeroCuenta())
                .clabe(cuenta.getClabe())
                .titular(cuenta.getTitular())
                .saldo(cuenta.getSaldo())
                .moneda(cuenta.getMoneda())
                .estatus(cuenta.getEstatus())
                .fechaConsulta(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public CuentaBancariaDto actualizarCuenta(String numeroCuenta, CuentaActualizacionRequestDto dto) {
        // Validación Fail-Fast en Record
        CuentaActualizacionRequestRecord.de(dto);

        String cuentaLimpia = numeroCuenta.trim();
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuenta(cuentaLimpia)
                .orElseThrow(() -> new CuentaNoEncontradaException(MSG_CUENTA_NO_ENCONTRADA + cuentaLimpia));

        // Reglas de negocio declarativas
        DeclarativeAssertUtil.asegurar(Boolean.TRUE.equals(cuenta.getCliente().getActivo()),
                "No se puede modificar una cuenta bancaria perteneciente a un cliente inactivo");

        DeclarativeAssertUtil.prohibir(ESTATUS_CANCELADA.equalsIgnoreCase(cuenta.getEstatus()),
                "No se puede modificar una cuenta bancaria en estatus CANCELADA");

        DeclarativeAssertUtil.prohibir("INACTIVA".equalsIgnoreCase(cuenta.getEstatus())
                        && (dto.getEstatus() == null || !ESTATUS_ACTIVA.equalsIgnoreCase(dto.getEstatus())),
                "No se puede modificar una cuenta inactiva sin reactivarla primero a estatus ACTIVA");

        Optional.ofNullable(dto.getEstatus())
                .filter(Predicate.not(String::isBlank))
                .ifPresent(est -> {
                    Optional.of(est)
                            .filter(ESTATUS_ACTIVA::equalsIgnoreCase)
                            .ifPresent(a -> DeclarativeAssertUtil.asegurar(Boolean.TRUE.equals(cuenta.getCliente().getActivo()),
                                    "Solo los clientes activos pueden tener cuentas activas"));
                    cuenta.setEstatus(est.toUpperCase());
                });

        Optional.ofNullable(dto.getTipoCuenta())
                .filter(Predicate.not(String::isBlank))
                .ifPresent(tc -> cuenta.setTipoCuenta(tc.toUpperCase()));

        Optional.ofNullable(dto.getSaldo())
                .ifPresent(cuenta::setSaldo);

        CuentaEntity cuentaActualizada = cuentaRepository.save(cuenta);
        log.info("Cuenta bancaria {} actualizada exitosamente", numeroCuenta);

        return mapearCuentaADto(cuentaActualizada);
    }

    private String generarNumeroCuentaUnico() {
        String cuenta;
        int intentos = 0;
        do {
            cuenta = generadorCuentaUtil.generarNumeroCuenta();
            intentos++;
            DeclarativeAssertUtil.prohibir(intentos > 10, "No se pudo generar un número de cuenta único");
        } while (cuentaRepository.existsByNumeroCuenta(cuenta));
        return cuenta;
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public void desactivarCuenta(String numeroCuenta) {
        String cuentaLimpia = numeroCuenta.trim();
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuenta(cuentaLimpia)
                .orElseThrow(() -> new CuentaNoEncontradaException(MSG_CUENTA_NO_ENCONTRADA + cuentaLimpia));

        DeclarativeAssertUtil.prohibir(ESTATUS_CANCELADA.equalsIgnoreCase(cuenta.getEstatus()),
                () -> new ReglaNegocioException("La cuenta bancaria " + cuentaLimpia + " ya se encuentra cancelada"));

        cuenta.setEstatus(ESTATUS_CANCELADA);
        cuentaRepository.save(cuenta);
        log.info("Cuenta bancaria {} dada de baja lógicamente con estatus CANCELADA", cuentaLimpia);
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public CuentaBancariaDto reactivarCuenta(String numeroCuenta) {
        String cuentaLimpia = numeroCuenta.trim();
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuenta(cuentaLimpia)
                .orElseThrow(() -> new CuentaNoEncontradaException(MSG_CUENTA_NO_ENCONTRADA + cuentaLimpia));

        DeclarativeAssertUtil.asegurar(Boolean.TRUE.equals(cuenta.getCliente().getActivo()),
                "No se puede reactivar una cuenta perteneciente a un cliente inactivo");

        DeclarativeAssertUtil.prohibir(ESTATUS_ACTIVA.equalsIgnoreCase(cuenta.getEstatus()),
                () -> new ReglaNegocioException("La cuenta bancaria " + cuentaLimpia + " ya se encuentra activa"));

        cuenta.setEstatus(ESTATUS_ACTIVA);
        CuentaEntity guardada = cuentaRepository.save(cuenta);
        log.info("Cuenta bancaria {} reactivada exitosamente a estatus ACTIVA", cuentaLimpia);
        return mapearCuentaADto(guardada);
    }

    private CuentaBancariaDto mapearCuentaADto(CuentaEntity cuenta) {
        String titular = Optional.ofNullable(cuenta.getCliente())
                .map(c -> (Optional.ofNullable(c.getNombre()).orElse("") + " " +
                        Optional.ofNullable(c.getApellidoPaterno()).orElse("") + " " +
                        Optional.ofNullable(c.getApellidoMaterno()).orElse("")).trim())
                .orElse(null);

        return CuentaBancariaDto.builder()
                .idCuenta(cuenta.getId())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .clabe(cuenta.getClabe())
                .tipoCuenta(cuenta.getTipoCuenta())
                .saldo(cuenta.getSaldo())
                .moneda(cuenta.getMoneda())
                .estatus(cuenta.getEstatus())
                .fechaApertura(cuenta.getFechaApertura())
                .clienteId(Optional.ofNullable(cuenta.getCliente()).map(ClienteEntity::getId).orElse(null))
                .titular(titular)
                .build();
    }
}
