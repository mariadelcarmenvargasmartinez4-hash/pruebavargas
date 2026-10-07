package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.DomicilioEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.model.cliente.ClienteActualizacionRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroResponseDto;
import com.proyecto.servicios.model.cliente.CuentaBancariaDto;
import com.proyecto.servicios.model.cliente.DomicilioDto;
import com.proyecto.servicios.model.cliente.UsuarioAccesoDto;
import com.proyecto.servicios.model.util.MetricasTextoDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.DomicilioRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.service.EstadoCivilService;
import com.proyecto.servicios.service.NacionalidadService;
import com.proyecto.servicios.service.SexoService;
import com.proyecto.servicios.util.GeneradorCuentaBancariaUtil;
import com.proyecto.servicios.util.MetricasTextoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// Implementación de la lógica de negocio para clientes desacoplada, con validación declarativa y sin ifs anidados
@Slf4j
@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final DomicilioRepository domicilioRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioAccesoRepository usuarioAccesoRepository;
    private final GeneradorCuentaBancariaUtil generadorCuentaUtil;
    private final PasswordEncoder passwordEncoder;
    private final SexoService sexoService;
    private final NacionalidadService nacionalidadService;
    private final EstadoCivilService estadoCivilService;
    private final MetricasTextoUtil metricasTextoUtil;

    @Value("${banco.cuenta.saldo-inicial-default:500.00}")
    private BigDecimal saldoInicialPorDefecto;

    public ClienteServiceImpl(ClienteRepository clienteRepository,
                              DomicilioRepository domicilioRepository,
                              CuentaRepository cuentaRepository,
                              UsuarioAccesoRepository usuarioAccesoRepository,
                              GeneradorCuentaBancariaUtil generadorCuentaUtil,
                              PasswordEncoder passwordEncoder,
                              SexoService sexoService,
                              NacionalidadService nacionalidadService,
                              EstadoCivilService estadoCivilService,
                              MetricasTextoUtil metricasTextoUtil) {
        this.clienteRepository = clienteRepository;
        this.domicilioRepository = domicilioRepository;
        this.cuentaRepository = cuentaRepository;
        this.usuarioAccesoRepository = usuarioAccesoRepository;
        this.generadorCuentaUtil = generadorCuentaUtil;
        this.passwordEncoder = passwordEncoder;
        this.sexoService = sexoService;
        this.nacionalidadService = nacionalidadService;
        this.estadoCivilService = estadoCivilService;
        this.metricasTextoUtil = metricasTextoUtil;
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto registrarCliente(ClienteRegistroRequestDto dto) {
        log.info("Iniciando registro de cliente persona física con CURP: {}", dto.getCurp());

        // 1. Reglas de negocio iniciales (Guard Clauses sin ifs anidados)
        validarMayoriaDeEdad(dto.getFechaNacimiento());
        validarCatalogos(dto.getSexo(), dto.getNacionalidad(), dto.getEstadoCivil());

        String curp = dto.getCurp().trim().toUpperCase();
        validarCurpUnico(curp);

        String rfc = dto.getRfc().trim().toUpperCase();
        validarRfcUnico(rfc);

        String correo = dto.getCorreoElectronico().trim().toLowerCase();
        validarCorreoUnico(correo);

        // 2. Registro de métricas de texto y caracteres en campos alfanuméricos clave
        registrarMetricasTexto(curp, rfc);

        // 3. Saldo inicial
        BigDecimal saldoInicial = Optional.ofNullable(dto.getSaldoInicial())
                .filter(saldo -> saldo.compareTo(BigDecimal.ZERO) >= 0)
                .orElse(Optional.ofNullable(saldoInicialPorDefecto).orElse(new BigDecimal("500.00")));

        // 4. Persistencia de ClienteEntity
        ClienteEntity cliente = construirClienteEntity(dto, curp, rfc, correo);
        ClienteEntity clienteGuardado = clienteRepository.save(cliente);

        // 5. Persistencia de DomicilioEntity
        DomicilioEntity domicilio = construirDomicilioEntity(dto, clienteGuardado);
        DomicilioEntity domicilioGuardado = domicilioRepository.save(domicilio);
        clienteGuardado.setDomicilio(domicilioGuardado);

        // 6. Creación automática de Cuenta Bancaria
        CuentaEntity cuenta = construirCuentaEntity(dto, clienteGuardado, saldoInicial);
        CuentaEntity cuentaGuardada = cuentaRepository.save(cuenta);

        // 7. Creación automática de Usuario de Acceso
        UsuarioAccesoEntity usuario = construirUsuarioEntity(dto, clienteGuardado, correo);
        UsuarioAccesoEntity usuarioGuardado = usuarioAccesoRepository.save(usuario);
        clienteGuardado.setUsuarioAcceso(usuarioGuardado);

        log.info("Cliente registrado exitosamente. ID: {}, cuenta bancaria: {}, usuario: {}",
                clienteGuardado.getId(), cuentaGuardada.getNumeroCuenta(), usuarioGuardado.getUsername());

        return mapearARespuestaDto(clienteGuardado, domicilioGuardado, List.of(cuentaGuardada), cuentaGuardada, usuarioGuardado);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<ClienteRegistroResponseDto> listarClientes() {
        return clienteRepository.findAll().stream()
                .map(this::construirRespuestaCompleta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<ClienteRegistroResponseDto> listarClientesActivos() {
        return clienteRepository.findByActivoTrue().stream()
                .map(this::construirRespuestaCompleta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto obtenerClientePorId(Long id) {
        return clienteRepository.findById(id)
                .map(this::construirRespuestaCompleta)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID " + id));
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto obtenerClientePorCurp(String curp) {
        String curpLimpio = curp.trim().toUpperCase();
        return clienteRepository.findByCurp(curpLimpio)
                .map(this::construirRespuestaCompleta)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con CURP " + curpLimpio));
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto obtenerClientePorRfc(String rfc) {
        String rfcLimpio = rfc.trim().toUpperCase();
        return clienteRepository.findByRfc(rfcLimpio)
                .map(this::construirRespuestaCompleta)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con RFC " + rfcLimpio));
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto obtenerClientePorCorreo(String correo) {
        String correoLimpio = correo.trim().toLowerCase();
        return clienteRepository.findByCorreoElectronico(correoLimpio)
                .map(this::construirRespuestaCompleta)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con correo " + correoLimpio));
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto obtenerClientePorNumeroCuenta(String numeroCuenta) {
        String cuentaLimpia = numeroCuenta.trim();
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuenta(cuentaLimpia)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta bancaria no encontrada con número " + cuentaLimpia));

        return construirRespuestaCompleta(cuenta.getCliente());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<ClienteRegistroResponseDto> buscarClientesPorNombre(String nombre) {
        return clienteRepository.findByNombreContainingIgnoreCase(nombre.trim()).stream()
                .map(this::construirRespuestaCompleta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<ClienteRegistroResponseDto> buscarClientesPorApellidoPaterno(String apellidoPaterno) {
        return clienteRepository.findByApellidoPaternoContainingIgnoreCase(apellidoPaterno.trim()).stream()
                .map(this::construirRespuestaCompleta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<ClienteRegistroResponseDto> buscarClientesPorApellidoMaterno(String apellidoMaterno) {
        return clienteRepository.findByApellidoMaternoContainingIgnoreCase(apellidoMaterno.trim()).stream()
                .map(this::construirRespuestaCompleta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<ClienteRegistroResponseDto> buscarClientesPorRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return clienteRepository.findByFechaCreacionBetween(fechaInicio, fechaFin).stream()
                .map(this::construirRespuestaCompleta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto actualizarCliente(Long id, ClienteActualizacionRequestDto dto) {
        log.info("Iniciando actualización de cliente con ID: {}", id);

        ClienteEntity cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID " + id));

        actualizarDatosPersonales(cliente, dto);
        actualizarDatosContacto(cliente, dto);
        actualizarDatosLaborales(cliente, dto);
        actualizarDomicilio(cliente, dto);

        ClienteEntity actualizado = clienteRepository.save(cliente);
        log.info("Cliente ID {} actualizado exitosamente", id);
        return construirRespuestaCompleta(actualizado);
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public void desactivarCliente(Long id) {
        log.info("Ejecutando baja lógica para cliente con ID: {}", id);

        ClienteEntity cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID " + id));

        cliente.setActivo(false);
        clienteRepository.save(cliente);

        cuentaRepository.findByClienteId(cliente.getId()).forEach(cuenta -> {
            cuenta.setEstatus("INACTIVA");
            cuentaRepository.save(cuenta);
        });

        usuarioAccesoRepository.findByClienteId(cliente.getId()).ifPresent(usuario -> {
            usuario.setActivo(false);
            usuarioAccesoRepository.save(usuario);
        });

        log.info("Cliente ID {}, sus cuentas y usuario han sido desactivados (baja lógica)", id);
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto reactivarCliente(Long id) {
        log.info("Reactivando cliente con ID: {}", id);

        ClienteEntity cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID " + id));

        cliente.setActivo(true);
        clienteRepository.save(cliente);

        usuarioAccesoRepository.findByClienteId(cliente.getId()).ifPresent(usuario -> {
            usuario.setActivo(true);
            usuarioAccesoRepository.save(usuario);
        });

        return construirRespuestaCompleta(cliente);
    }

    // --- Métodos atómicos de actualización sin ifs anidados ---

    private void actualizarDatosPersonales(ClienteEntity cliente, ClienteActualizacionRequestDto dto) {
        Optional.ofNullable(dto.getFechaNacimiento()).ifPresent(fecha -> {
            validarMayoriaDeEdad(fecha);
            cliente.setFechaNacimiento(fecha);
        });

        Optional.ofNullable(dto.getNombre()).filter(Predicate.not(String::isBlank)).ifPresent(cliente::setNombre);
        Optional.ofNullable(dto.getSegundoNombre()).ifPresent(cliente::setSegundoNombre);
        Optional.ofNullable(dto.getApellidoPaterno()).filter(Predicate.not(String::isBlank)).ifPresent(cliente::setApellidoPaterno);
        Optional.ofNullable(dto.getApellidoMaterno()).filter(Predicate.not(String::isBlank)).ifPresent(cliente::setApellidoMaterno);

        Optional.ofNullable(dto.getSexo()).filter(Predicate.not(String::isBlank)).ifPresent(s -> {
            if (!sexoService.esValidoYActivo(s)) {
                throw new ReglaNegocioException("El sexo '" + s + "' no es válido en el catálogo");
            }
            cliente.setSexo(s.toUpperCase());
        });

        Optional.ofNullable(dto.getNacionalidad()).filter(Predicate.not(String::isBlank)).ifPresent(n -> {
            if (!nacionalidadService.esValidoYActivo(n)) {
                throw new ReglaNegocioException("La nacionalidad '" + n + "' no es válida en el catálogo");
            }
            cliente.setNacionalidad(n.toUpperCase());
        });

        Optional.ofNullable(dto.getEstadoCivil()).filter(Predicate.not(String::isBlank)).ifPresent(ec -> {
            if (!estadoCivilService.esValidoYActivo(ec)) {
                throw new ReglaNegocioException("El estado civil '" + ec + "' no es válido en el catálogo");
            }
            cliente.setEstadoCivil(ec.toUpperCase());
        });
    }

    private void actualizarDatosContacto(ClienteEntity cliente, ClienteActualizacionRequestDto dto) {
        Optional.ofNullable(dto.getCorreoElectronico())
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(correo -> !correo.equalsIgnoreCase(cliente.getCorreoElectronico()))
                .ifPresent(nuevoCorreo -> {
                    validarCorreoUnico(nuevoCorreo);
                    cliente.setCorreoElectronico(nuevoCorreo);
                });

        Optional.ofNullable(dto.getTelefonoMovil()).filter(Predicate.not(String::isBlank)).ifPresent(cliente::setTelefonoMovil);
        Optional.ofNullable(dto.getTelefonoAlternativo()).ifPresent(cliente::setTelefonoAlternativo);
    }

    private void actualizarDatosLaborales(ClienteEntity cliente, ClienteActualizacionRequestDto dto) {
        Optional.ofNullable(dto.getOcupacion()).filter(Predicate.not(String::isBlank)).ifPresent(cliente::setOcupacion);
        Optional.ofNullable(dto.getEmpresa()).filter(Predicate.not(String::isBlank)).ifPresent(cliente::setEmpresa);
        Optional.ofNullable(dto.getIngresoMensual()).ifPresent(ingreso -> {
            if (ingreso.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ReglaNegocioException("El ingreso mensual debe ser mayor a cero");
            }
            cliente.setIngresoMensual(ingreso);
        });
    }

    private void actualizarDomicilio(ClienteEntity cliente, ClienteActualizacionRequestDto dto) {
        DomicilioEntity domicilio = domicilioRepository.findByClienteId(cliente.getId())
                .orElseGet(() -> DomicilioEntity.builder().cliente(cliente).build());

        Optional.ofNullable(dto.getCalle()).filter(Predicate.not(String::isBlank)).ifPresent(domicilio::setCalle);
        Optional.ofNullable(dto.getNumeroExterior()).filter(Predicate.not(String::isBlank)).ifPresent(domicilio::setNumeroExterior);
        Optional.ofNullable(dto.getNumeroInterior()).ifPresent(domicilio::setNumeroInterior);
        Optional.ofNullable(dto.getColonia()).filter(Predicate.not(String::isBlank)).ifPresent(domicilio::setColonia);
        Optional.ofNullable(dto.getMunicipio()).filter(Predicate.not(String::isBlank)).ifPresent(domicilio::setMunicipio);
        Optional.ofNullable(dto.getEstado()).filter(Predicate.not(String::isBlank)).ifPresent(domicilio::setEstado);
        Optional.ofNullable(dto.getCodigoPostal()).filter(Predicate.not(String::isBlank)).ifPresent(domicilio::setCodigoPostal);
        Optional.ofNullable(dto.getPais()).filter(Predicate.not(String::isBlank)).ifPresent(domicilio::setPais);

        Optional.ofNullable(domicilio.getCalle()).ifPresent(c -> domicilioRepository.save(domicilio));
    }

    // --- Validaciones y Guard Clauses ---

    private void validarMayoriaDeEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new ReglaNegocioException("La fecha de nacimiento es obligatoria");
        }
        if (fechaNacimiento.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de nacimiento no puede ser una fecha futura");
        }
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < 18) {
            throw new ReglaNegocioException("El cliente debe ser mayor de edad (18 años o más)");
        }
        if (edad > 100) {
            throw new ReglaNegocioException("La fecha de nacimiento no debe exceder los 100 años");
        }
    }

    private void validarCatalogos(String sexo, String nacionalidad, String estadoCivil) {
        if (!sexoService.esValidoYActivo(sexo)) {
            throw new ReglaNegocioException("El sexo '" + sexo + "' no es válido o no está activo en el catálogo");
        }
        if (!nacionalidadService.esValidoYActivo(nacionalidad)) {
            throw new ReglaNegocioException("La nacionalidad '" + nacionalidad + "' no es válida o no está activa en el catálogo");
        }
        if (!estadoCivilService.esValidoYActivo(estadoCivil)) {
            throw new ReglaNegocioException("El estado civil '" + estadoCivil + "' no es válido o no está activo en el catálogo");
        }
    }

    private void validarCurpUnico(String curp) {
        if (clienteRepository.existsByCurp(curp)) {
            throw new CurpDuplicadaException("El CURP '" + curp + "' ya está registrado");
        }
    }

    private void validarRfcUnico(String rfc) {
        if (clienteRepository.existsByRfc(rfc)) {
            throw new RfcDuplicadoException("El RFC '" + rfc + "' ya está registrado");
        }
    }

    private void validarCorreoUnico(String correo) {
        if (clienteRepository.existsByCorreoElectronico(correo)) {
            throw new ClienteYaRegistradoException("El correo electrónico ya está registrado");
        }
    }

    private void registrarMetricasTexto(String curp, String rfc) {
        MetricasTextoDto metricasCurp = metricasTextoUtil.analizar("curp", curp);
        MetricasTextoDto metricasRfc = metricasTextoUtil.analizar("rfc", rfc);
        log.info("Métricas calculadas - CURP total: {}, letras: {}, dígitos: {}; RFC total: {}, letras: {}, dígitos: {}",
                metricasCurp.getTotalCaracteres(), metricasCurp.getTotalLetras(), metricasCurp.getTotalDigitos(),
                metricasRfc.getTotalCaracteres(), metricasRfc.getTotalLetras(), metricasRfc.getTotalDigitos());
    }

    // --- Constructores de Entidades ---

    private ClienteEntity construirClienteEntity(ClienteRegistroRequestDto dto, String curp, String rfc, String correo) {
        return ClienteEntity.builder()
                .nombre(dto.getNombre().trim())
                .segundoNombre(Optional.ofNullable(dto.getSegundoNombre()).map(String::trim).orElse(null))
                .apellidoPaterno(dto.getApellidoPaterno().trim())
                .apellidoMaterno(dto.getApellidoMaterno().trim())
                .fechaNacimiento(dto.getFechaNacimiento())
                .curp(curp)
                .rfc(rfc)
                .sexo(dto.getSexo().trim().toUpperCase())
                .nacionalidad(dto.getNacionalidad().trim().toUpperCase())
                .estadoCivil(dto.getEstadoCivil().trim().toUpperCase())
                .correoElectronico(correo)
                .telefonoMovil(dto.getTelefonoMovil().trim())
                .telefonoAlternativo(Optional.ofNullable(dto.getTelefonoAlternativo()).map(String::trim).filter(Predicate.not(String::isBlank)).orElse(null))
                .ocupacion(dto.getOcupacion().trim())
                .empresa(dto.getEmpresa().trim())
                .ingresoMensual(dto.getIngresoMensual())
                .activo(true)
                .build();
    }

    private DomicilioEntity construirDomicilioEntity(ClienteRegistroRequestDto dto, ClienteEntity cliente) {
        return DomicilioEntity.builder()
                .cliente(cliente)
                .calle(dto.getCalle().trim())
                .numeroExterior(dto.getNumeroExterior().trim())
                .numeroInterior(Optional.ofNullable(dto.getNumeroInterior()).map(String::trim).filter(Predicate.not(String::isBlank)).orElse(null))
                .colonia(dto.getColonia().trim())
                .municipio(dto.getMunicipio().trim())
                .estado(dto.getEstado().trim())
                .codigoPostal(dto.getCodigoPostal().trim())
                .pais(dto.getPais().trim())
                .build();
    }

    private CuentaEntity construirCuentaEntity(ClienteRegistroRequestDto dto, ClienteEntity cliente, BigDecimal saldoInicial) {
        String numeroCuenta = generarNumeroCuentaUnico();
        String clabe = generadorCuentaUtil.generarClabe(numeroCuenta);
        String tipoCuenta = Optional.ofNullable(dto.getTipoCuenta())
                .filter(Predicate.not(String::isBlank))
                .map(String::toUpperCase)
                .orElse("DEBITO");

        return CuentaEntity.builder()
                .cliente(cliente)
                .numeroCuenta(numeroCuenta)
                .clabe(clabe)
                .tipoCuenta(tipoCuenta)
                .saldo(saldoInicial)
                .moneda("MXN")
                .estatus("ACTIVA")
                .build();
    }

    private UsuarioAccesoEntity construirUsuarioEntity(ClienteRegistroRequestDto dto, ClienteEntity cliente, String correo) {
        return UsuarioAccesoEntity.builder()
                .cliente(cliente)
                .username(correo)
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .rol("CLIENTE")
                .activo(true)
                .build();
    }

    private String generarNumeroCuentaUnico() {
        String cuenta;
        int intentos = 0;
        do {
            cuenta = generadorCuentaUtil.generarNumeroCuenta();
            intentos++;
            if (intentos > 10) {
                throw new ReglaNegocioException("No se pudo generar un número de cuenta único");
            }
        } while (cuentaRepository.existsByNumeroCuenta(cuenta));
        return cuenta;
    }

    private ClienteRegistroResponseDto construirRespuestaCompleta(ClienteEntity cliente) {
        DomicilioEntity domicilio = domicilioRepository.findByClienteId(cliente.getId()).orElse(null);
        List<CuentaEntity> cuentas = cuentaRepository.findByClienteId(cliente.getId());
        CuentaEntity cuentaPrincipal = cuentas.isEmpty() ? null : cuentas.get(0);
        UsuarioAccesoEntity usuario = usuarioAccesoRepository.findByClienteId(cliente.getId()).orElse(null);
        return mapearARespuestaDto(cliente, domicilio, cuentas, cuentaPrincipal, usuario);
    }

    private ClienteRegistroResponseDto mapearARespuestaDto(ClienteEntity cliente,
                                                          DomicilioEntity domicilio,
                                                          List<CuentaEntity> cuentas,
                                                          CuentaEntity cuentaPrincipal,
                                                          UsuarioAccesoEntity usuarioAcceso) {
        StringBuilder nombreCompleto = new StringBuilder(cliente.getNombre());
        Optional.ofNullable(cliente.getSegundoNombre())
                .filter(Predicate.not(String::isBlank))
                .ifPresent(sn -> nombreCompleto.append(" ").append(sn));
        nombreCompleto.append(" ").append(cliente.getApellidoPaterno());
        nombreCompleto.append(" ").append(cliente.getApellidoMaterno());

        DomicilioDto domDto = Optional.ofNullable(domicilio)
                .map(d -> DomicilioDto.builder()
                        .idDomicilio(d.getId())
                        .calle(d.getCalle())
                        .numeroExterior(d.getNumeroExterior())
                        .numeroInterior(d.getNumeroInterior())
                        .colonia(d.getColonia())
                        .municipio(d.getMunicipio())
                        .estado(d.getEstado())
                        .codigoPostal(d.getCodigoPostal())
                        .pais(d.getPais())
                        .build())
                .orElse(null);

        CuentaBancariaDto cuentaPrincipalDto = Optional.ofNullable(cuentaPrincipal)
                .map(this::mapearCuentaADto)
                .orElse(null);

        List<CuentaBancariaDto> cuentasDto = Optional.ofNullable(cuentas)
                .map(c -> c.stream().map(this::mapearCuentaADto).collect(Collectors.toList()))
                .orElse(List.of());

        UsuarioAccesoDto usuarioDto = Optional.ofNullable(usuarioAcceso)
                .map(u -> UsuarioAccesoDto.builder()
                        .idUsuario(u.getId())
                        .username(u.getUsername())
                        .activo(u.getActivo())
                        .fechaCreacion(u.getFechaCreacion())
                        .build())
                .orElse(null);

        return ClienteRegistroResponseDto.builder()
                .codigo(0)
                .mensaje("Operación realizada con éxito")
                .idCliente(cliente.getId())
                .nombreCompleto(nombreCompleto.toString())
                .nombre(cliente.getNombre())
                .segundoNombre(cliente.getSegundoNombre())
                .apellidoPaterno(cliente.getApellidoPaterno())
                .apellidoMaterno(cliente.getApellidoMaterno())
                .curp(cliente.getCurp())
                .rfc(cliente.getRfc())
                .sexo(cliente.getSexo())
                .nacionalidad(cliente.getNacionalidad())
                .estadoCivil(cliente.getEstadoCivil())
                .correoElectronico(cliente.getCorreoElectronico())
                .telefonoMovil(cliente.getTelefonoMovil())
                .telefonoAlternativo(cliente.getTelefonoAlternativo())
                .fechaNacimiento(cliente.getFechaNacimiento())
                .ocupacion(cliente.getOcupacion())
                .empresa(cliente.getEmpresa())
                .ingresoMensual(cliente.getIngresoMensual())
                .activo(cliente.getActivo())
                .domicilio(domDto)
                .cuentaBancaria(cuentaPrincipalDto)
                .cuentas(cuentasDto)
                .usuarioAcceso(usuarioDto)
                .fechaRegistro(cliente.getFechaCreacion())
                .fechaActualizacion(cliente.getFechaActualizacion())
                .build();
    }

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
