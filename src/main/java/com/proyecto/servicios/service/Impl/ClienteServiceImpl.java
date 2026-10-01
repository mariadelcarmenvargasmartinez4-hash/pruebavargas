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
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.DomicilioRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.util.GeneradorCuentaBancariaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

// Implementacion de la logica de negocio para clientes con persistencia en PostgreSQL
@Slf4j
@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final DomicilioRepository domicilioRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioAccesoRepository usuarioAccesoRepository;
    private final GeneradorCuentaBancariaUtil generadorCuentaUtil;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Value("${banco.cuenta.saldo-inicial-default:500.00}")
    private BigDecimal saldoInicialPorDefecto;

    // Inyeccion de dependencias por constructor
    public ClienteServiceImpl(ClienteRepository clienteRepository,
                              DomicilioRepository domicilioRepository,
                              CuentaRepository cuentaRepository,
                              UsuarioAccesoRepository usuarioAccesoRepository,
                              GeneradorCuentaBancariaUtil generadorCuentaUtil,
                              org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.domicilioRepository = domicilioRepository;
        this.cuentaRepository = cuentaRepository;
        this.usuarioAccesoRepository = usuarioAccesoRepository;
        this.generadorCuentaUtil = generadorCuentaUtil;
        this.passwordEncoder = passwordEncoder;
    }

    // Registra un nuevo cliente persona fisica, su domicilio y crea automaticamente su cuenta bancaria
    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto registrarCliente(ClienteRegistroRequestDto dto) {
        log.info("Iniciando registro de cliente persona fisica con CURP: {}", dto.getCurp());

        // 1. Regla de negocio: El cliente debe ser mayor de edad (18 anos o mas)
        validarMayoriaDeEdad(dto.getFechaNacimiento());

        // 2. Regla de negocio: No pueden existir dos clientes con la misma CURP
        String curp = dto.getCurp().trim().toUpperCase();
        if (clienteRepository.existsByCurp(curp)) {
            log.warn("El CURP '{}' ya se encuentra registrado", curp);
            throw new CurpDuplicadaException("El CURP '" + curp + "' ya esta registrado");
        }

        // 3. Regla de negocio: No pueden existir dos clientes con el mismo RFC
        String rfc = dto.getRfc().trim().toUpperCase();
        if (clienteRepository.existsByRfc(rfc)) {
            log.warn("El RFC '{}' ya se encuentra registrado", rfc);
            throw new RfcDuplicadoException("El RFC '" + rfc + "' ya esta registrado");
        }

        // 4. Regla de negocio: No pueden existir dos clientes con el mismo correo electronico
        String correo = dto.getCorreoElectronico().trim().toLowerCase();
        if (clienteRepository.existsByCorreoElectronico(correo)) {
            log.warn("El correo '{}' ya se encuentra registrado", correo);
            throw new ClienteYaRegistradoException("El correo electronico ya esta registrado");
        }

        // 5. Regla de negocio: El ingreso mensual debe ser mayor a cero
        if (dto.getIngresoMensual() == null || dto.getIngresoMensual().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El ingreso mensual debe ser mayor a cero");
        }

        // 6. Regla de negocio: El saldo inicial no puede ser negativo
        if (dto.getSaldoInicial() != null && dto.getSaldoInicial().compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El saldo inicial no puede ser negativo");
        }

        BigDecimal saldoInicial = saldoInicialPorDefecto != null
            ? saldoInicialPorDefecto
            : new BigDecimal("500.00");

        // 7. Persistencia de la entidad ClienteEntity (Tabla clientes)
        ClienteEntity cliente = ClienteEntity.builder()
                .nombre(dto.getNombre().trim())
                .segundoNombre(dto.getSegundoNombre() != null ? dto.getSegundoNombre().trim() : null)
                .apellidoPaterno(dto.getApellidoPaterno().trim())
                .apellidoMaterno(dto.getApellidoMaterno().trim())
                .fechaNacimiento(dto.getFechaNacimiento())
                .curp(curp)
                .rfc(rfc)
                .sexo(dto.getSexo().trim().toUpperCase())
                .nacionalidad(dto.getNacionalidad().trim())
                .estadoCivil(dto.getEstadoCivil().trim().toUpperCase())
                .correoElectronico(correo)
                .telefonoMovil(dto.getTelefonoMovil().trim())
                .telefonoAlternativo(dto.getTelefonoAlternativo() != null && !dto.getTelefonoAlternativo().isBlank() ? dto.getTelefonoAlternativo().trim() : null)
                .ocupacion(dto.getOcupacion().trim())
                .empresa(dto.getEmpresa().trim())
                .ingresoMensual(dto.getIngresoMensual())
                .activo(true)
                .build();

        ClienteEntity clienteGuardado = clienteRepository.save(cliente);

        // 8. Persistencia de la entidad DomicilioEntity (Tabla domicilios)
        DomicilioEntity domicilio = DomicilioEntity.builder()
                .cliente(clienteGuardado)
                .calle(dto.getCalle().trim())
                .numeroExterior(dto.getNumeroExterior().trim())
                .numeroInterior(dto.getNumeroInterior() != null && !dto.getNumeroInterior().isBlank() ? dto.getNumeroInterior().trim() : null)
                .colonia(dto.getColonia().trim())
                .municipio(dto.getMunicipio().trim())
                .estado(dto.getEstado().trim())
                .codigoPostal(dto.getCodigoPostal().trim())
                .pais(dto.getPais().trim())
                .build();

        DomicilioEntity domicilioGuardado = domicilioRepository.save(domicilio);
        clienteGuardado.setDomicilio(domicilioGuardado);

        // 9. Creacion automatica de cuenta bancaria unica (Tabla cuentas)
        String numeroCuenta = generarNumeroCuentaUnico();
        String clabe = generadorCuentaUtil.generarClabe(numeroCuenta);
        String tipoCuenta = (dto.getTipoCuenta() != null && !dto.getTipoCuenta().isBlank()) ? dto.getTipoCuenta().toUpperCase() : "DEBITO";

        CuentaEntity cuenta = CuentaEntity.builder()
                .cliente(clienteGuardado)
                .numeroCuenta(numeroCuenta)
                .clabe(clabe)
                .tipoCuenta(tipoCuenta)
                .saldo(saldoInicial)
                .moneda("MXN")
                .estatus("ACTIVA")
                .build();

        CuentaEntity cuentaGuardada = cuentaRepository.save(cuenta);

        // 10. Creacion automatica de Usuario de Acceso (correo como username, password cifrado con BCrypt)
        String passwordCifrada = passwordEncoder.encode(dto.getPassword());

        UsuarioAccesoEntity usuarioAcceso = UsuarioAccesoEntity.builder()
                .cliente(clienteGuardado)
                .username(correo)
                .passwordHash(passwordCifrada)
                .rol("CLIENTE")
                .activo(true)
                .build();

        UsuarioAccesoEntity usuarioGuardado = usuarioAccesoRepository.save(usuarioAcceso);
        clienteGuardado.setUsuarioAcceso(usuarioGuardado);

        log.info("Usuario de acceso creado exitosamente para el cliente ID: {}, username: {}", clienteGuardado.getId(), usuarioGuardado.getUsername());
        log.info("Cliente registrado exitosamente. ID: {}, cuenta bancaria: {}, saldo inicial: {}", clienteGuardado.getId(), cuentaGuardada.getNumeroCuenta(), cuentaGuardada.getSaldo());

        return mapearARespuestaDto(clienteGuardado, domicilioGuardado, List.of(cuentaGuardada), cuentaGuardada, usuarioGuardado);
    }

    // Consulta todos los clientes registrados
    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<ClienteRegistroResponseDto> listarClientes() {
        return clienteRepository.findAll().stream()
                .map(this::construirRespuestaCompleta)
                .collect(Collectors.toList());
    }

    // Consulta solo clientes activos
    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<ClienteRegistroResponseDto> listarClientesActivos() {
        return clienteRepository.findByActivoTrue().stream()
                .map(this::construirRespuestaCompleta)
                .collect(Collectors.toList());
    }

    // Consulta cliente por ID
    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto obtenerClientePorId(Long id) {
        ClienteEntity cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID " + id));

        return construirRespuestaCompleta(cliente);
    }

    // Consulta cliente por CURP
    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto obtenerClientePorCurp(String curp) {
        String curpLimpio = curp.trim().toUpperCase();
        ClienteEntity cliente = clienteRepository.findByCurp(curpLimpio)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con CURP " + curpLimpio));

        return construirRespuestaCompleta(cliente);
    }

    // Consulta cliente por RFC
    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto obtenerClientePorRfc(String rfc) {
        String rfcLimpio = rfc.trim().toUpperCase();
        ClienteEntity cliente = clienteRepository.findByRfc(rfcLimpio)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con RFC " + rfcLimpio));

        return construirRespuestaCompleta(cliente);
    }

    // Consulta cliente por correo electronico
    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto obtenerClientePorCorreo(String correo) {
        String correoLimpio = correo.trim().toLowerCase();
        ClienteEntity cliente = clienteRepository.findByCorreoElectronico(correoLimpio)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con correo " + correoLimpio));

        return construirRespuestaCompleta(cliente);
    }

    // Consulta cliente por numero de cuenta
    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto obtenerClientePorNumeroCuenta(String numeroCuenta) {
        String cuentaLimpia = numeroCuenta.trim();
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuenta(cuentaLimpia)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta bancaria no encontrada con numero " + cuentaLimpia));

        ClienteEntity cliente = cuenta.getCliente();
        return construirRespuestaCompleta(cliente);
    }

    // Busquedas por filtros parciales
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

    // Consulta clientes registrados en rango de fechas
    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<ClienteRegistroResponseDto> buscarClientesPorRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return clienteRepository.findByFechaCreacionBetween(fechaInicio, fechaFin).stream()
                .map(this::construirRespuestaCompleta)
                .collect(Collectors.toList());
    }

    // Actualizacion parcial de datos de cliente (CURP, RFC y cuenta son inmutables)
    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto actualizarCliente(Long id, ClienteActualizacionRequestDto dto) {
        log.info("Iniciando actualizacion parcial de cliente con ID: {}", id);

        ClienteEntity cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID " + id));

        // Validacion de fecha de nacimiento si se envio
        if (dto.getFechaNacimiento() != null) {
            validarMayoriaDeEdad(dto.getFechaNacimiento());
            cliente.setFechaNacimiento(dto.getFechaNacimiento());
        }

        // Validacion de unicidad de correo si ha cambiado
        if (dto.getCorreoElectronico() != null && !dto.getCorreoElectronico().isBlank()) {
            String nuevoCorreo = dto.getCorreoElectronico().trim().toLowerCase();
            if (!nuevoCorreo.equalsIgnoreCase(cliente.getCorreoElectronico()) && clienteRepository.existsByCorreoElectronico(nuevoCorreo)) {
                throw new ClienteYaRegistradoException("El correo electronico ya esta registrado por otro cliente");
            }
            cliente.setCorreoElectronico(nuevoCorreo);
        }

        // Actualizacion de datos personales
        if (dto.getNombre() != null && !dto.getNombre().isBlank()) cliente.setNombre(dto.getNombre().trim());
        if (dto.getSegundoNombre() != null) cliente.setSegundoNombre(dto.getSegundoNombre().trim());
        if (dto.getApellidoPaterno() != null && !dto.getApellidoPaterno().isBlank()) cliente.setApellidoPaterno(dto.getApellidoPaterno().trim());
        if (dto.getApellidoMaterno() != null && !dto.getApellidoMaterno().isBlank()) cliente.setApellidoMaterno(dto.getApellidoMaterno().trim());
        if (dto.getSexo() != null && !dto.getSexo().isBlank()) cliente.setSexo(dto.getSexo().trim().toUpperCase());
        if (dto.getNacionalidad() != null && !dto.getNacionalidad().isBlank()) cliente.setNacionalidad(dto.getNacionalidad().trim());
        if (dto.getEstadoCivil() != null && !dto.getEstadoCivil().isBlank()) cliente.setEstadoCivil(dto.getEstadoCivil().trim().toUpperCase());

        // Actualizacion de datos de contacto
        if (dto.getTelefonoMovil() != null && !dto.getTelefonoMovil().isBlank()) cliente.setTelefonoMovil(dto.getTelefonoMovil().trim());
        if (dto.getTelefonoAlternativo() != null) cliente.setTelefonoAlternativo(dto.getTelefonoAlternativo().trim());

        // Actualizacion laboral
        if (dto.getOcupacion() != null && !dto.getOcupacion().isBlank()) cliente.setOcupacion(dto.getOcupacion().trim());
        if (dto.getEmpresa() != null && !dto.getEmpresa().isBlank()) cliente.setEmpresa(dto.getEmpresa().trim());
        if (dto.getIngresoMensual() != null) {
            if (dto.getIngresoMensual().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ReglaNegocioException("El ingreso mensual debe ser mayor a cero");
            }
            cliente.setIngresoMensual(dto.getIngresoMensual());
        }

        ClienteEntity clienteActualizado = clienteRepository.save(cliente);

        // Actualizacion del Domicilio si viene algun campo
        DomicilioEntity domicilio = domicilioRepository.findByClienteId(clienteActualizado.getId())
                .orElseGet(() -> DomicilioEntity.builder().cliente(clienteActualizado).build());

        boolean actualizoDomicilio = false;
        if (dto.getCalle() != null && !dto.getCalle().isBlank()) { domicilio.setCalle(dto.getCalle().trim()); actualizoDomicilio = true; }
        if (dto.getNumeroExterior() != null && !dto.getNumeroExterior().isBlank()) { domicilio.setNumeroExterior(dto.getNumeroExterior().trim()); actualizoDomicilio = true; }
        if (dto.getNumeroInterior() != null) { domicilio.setNumeroInterior(dto.getNumeroInterior().trim()); actualizoDomicilio = true; }
        if (dto.getColonia() != null && !dto.getColonia().isBlank()) { domicilio.setColonia(dto.getColonia().trim()); actualizoDomicilio = true; }
        if (dto.getMunicipio() != null && !dto.getMunicipio().isBlank()) { domicilio.setMunicipio(dto.getMunicipio().trim()); actualizoDomicilio = true; }
        if (dto.getEstado() != null && !dto.getEstado().isBlank()) { domicilio.setEstado(dto.getEstado().trim()); actualizoDomicilio = true; }
        if (dto.getCodigoPostal() != null && !dto.getCodigoPostal().isBlank()) { domicilio.setCodigoPostal(dto.getCodigoPostal().trim()); actualizoDomicilio = true; }
        if (dto.getPais() != null && !dto.getPais().isBlank()) { domicilio.setPais(dto.getPais().trim()); actualizoDomicilio = true; }

        if (actualizoDomicilio && domicilio.getCalle() != null) {
            domicilioRepository.save(domicilio);
        }

        log.info("Cliente con ID {} actualizado exitosamente", id);
        return construirRespuestaCompleta(clienteActualizado);
    }

    // 5. Baja Logica: Desactiva cliente y sus cuentas bancarias sin eliminarlos de la BD
    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public void desactivarCliente(Long id) {
        log.info("Ejecutando baja logica para cliente con ID: {}", id);

        ClienteEntity cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID " + id));

        cliente.setActivo(false);
        clienteRepository.save(cliente);

        // Regla de negocio: Solo los clientes activos podrán tener cuentas activas.
        // Al desactivar el cliente, se desactivan o cancelan sus cuentas y su usuario de acceso.
        List<CuentaEntity> cuentas = cuentaRepository.findByClienteId(cliente.getId());
        for (CuentaEntity cuenta : cuentas) {
            cuenta.setEstatus("INACTIVA");
            cuentaRepository.save(cuenta);
        }

        usuarioAccesoRepository.findByClienteId(cliente.getId()).ifPresent(usuario -> {
            usuario.setActivo(false);
            usuarioAccesoRepository.save(usuario);
        });

        log.info("Cliente con ID {}, sus cuentas y usuario de acceso han sido desactivados exitosamente (baja logica)", id);
    }

    // Reactiva un cliente, sus cuentas y su usuario
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

    // Construye la respuesta completa DTO con cliente, domicilio, cuentas y usuario de acceso
    private ClienteRegistroResponseDto construirRespuestaCompleta(ClienteEntity cliente) {
        DomicilioEntity domicilio = domicilioRepository.findByClienteId(cliente.getId()).orElse(null);
        List<CuentaEntity> cuentas = cuentaRepository.findByClienteId(cliente.getId());
        CuentaEntity cuentaPrincipal = cuentas.isEmpty() ? null : cuentas.get(0);
        UsuarioAccesoEntity usuario = usuarioAccesoRepository.findByClienteId(cliente.getId()).orElse(null);
        return mapearARespuestaDto(cliente, domicilio, cuentas, cuentaPrincipal, usuario);
    }

    // Valida mayoria de edad (18 anos o mas)
    private void validarMayoriaDeEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new ReglaNegocioException("La fecha de nacimiento es obligatoria");
        }
        if (fechaNacimiento.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de nacimiento no puede ser una fecha futura");
        }
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < 18) {
            log.warn("Cliente menor de edad. Calculada: {} anos", edad);
            throw new ReglaNegocioException("El cliente debe ser mayor de edad (18 anos o mas)");
        }
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

    // Mapeador de entidades a DTO
    private ClienteRegistroResponseDto mapearARespuestaDto(ClienteEntity cliente,
                                                          DomicilioEntity domicilio,
                                                          List<CuentaEntity> cuentas,
                                                          CuentaEntity cuentaPrincipal,
                                                          UsuarioAccesoEntity usuarioAcceso) {
        StringBuilder nombreCompleto = new StringBuilder(cliente.getNombre());
        if (cliente.getSegundoNombre() != null && !cliente.getSegundoNombre().isBlank()) {
            nombreCompleto.append(" ").append(cliente.getSegundoNombre());
        }
        nombreCompleto.append(" ").append(cliente.getApellidoPaterno());
        nombreCompleto.append(" ").append(cliente.getApellidoMaterno());

        DomicilioDto domDto = null;
        if (domicilio != null) {
            domDto = DomicilioDto.builder()
                    .idDomicilio(domicilio.getId())
                    .calle(domicilio.getCalle())
                    .numeroExterior(domicilio.getNumeroExterior())
                    .numeroInterior(domicilio.getNumeroInterior())
                    .colonia(domicilio.getColonia())
                    .municipio(domicilio.getMunicipio())
                    .estado(domicilio.getEstado())
                    .codigoPostal(domicilio.getCodigoPostal())
                    .pais(domicilio.getPais())
                    .build();
        }

        CuentaBancariaDto cuentaPrincipalDto = null;
        if (cuentaPrincipal != null) {
            cuentaPrincipalDto = mapearCuentaADto(cuentaPrincipal);
        }

        List<CuentaBancariaDto> cuentasDto = (cuentas != null)
                ? cuentas.stream().map(this::mapearCuentaADto).collect(Collectors.toList())
                : List.of();

        UsuarioAccesoDto usuarioDto = null;
        if (usuarioAcceso != null) {
            usuarioDto = UsuarioAccesoDto.builder()
                    .idUsuario(usuarioAcceso.getId())
                    .username(usuarioAcceso.getUsername())
                    .activo(usuarioAcceso.getActivo())
                    .fechaCreacion(usuarioAcceso.getFechaCreacion())
                    .build();
        }

        return ClienteRegistroResponseDto.builder()
                .codigo(0)
                .mensaje("Operacion realizada con exito")
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
