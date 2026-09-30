package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.CuentaBancariaEntity;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.cliente.ClienteRegistroResponseDto;
import com.proyecto.servicios.model.cliente.CuentaBancariaDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaBancariaRepository;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.util.GeneradorCuentaBancariaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

// Implementacion de la logica de negocio para el registro de clientes y apertura de cuenta bancaria
@Slf4j
@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaBancariaRepository cuentaBancariaRepository;
    private final GeneradorCuentaBancariaUtil generadorCuentaUtil;

    // Inyeccion de dependencias por constructor
    public ClienteServiceImpl(ClienteRepository clienteRepository,
                              CuentaBancariaRepository cuentaBancariaRepository,
                              GeneradorCuentaBancariaUtil generadorCuentaUtil) {
        this.clienteRepository = clienteRepository;
        this.cuentaBancariaRepository = cuentaBancariaRepository;
        this.generadorCuentaUtil = generadorCuentaUtil;
    }

    // Registra un cliente persona fisica y genera su cuenta bancaria con saldo inicial
    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto registrarCliente(ClienteRegistroRequestDto dto) {
        log.info("Iniciando registro de cliente persona fisica con CURP: {}", dto.getCurp());

        // 1. Validacion de mayoria de edad (minimo 18 anos)
        validarMayoriaDeEdad(dto.getFechaNacimiento());

        // 2. Validacion de unicidad de CURP
        if (clienteRepository.existsByCurp(dto.getCurp())) {
            log.warn("El CURP '{}' ya se encuentra registrado en el sistema", dto.getCurp());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El CURP '" + dto.getCurp() + "' ya esta registrado");
        }

        // 3. Validacion de unicidad de RFC
        if (clienteRepository.existsByRfc(dto.getRfc())) {
            log.warn("El RFC '{}' ya se encuentra registrado en el sistema", dto.getRfc());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El RFC '" + dto.getRfc() + "' ya esta registrado");
        }

        // 4. Validacion de unicidad de correo electronico
        if (clienteRepository.existsByCorreoElectronico(dto.getCorreoElectronico())) {
            log.warn("El correo '{}' ya se encuentra registrado en el sistema", dto.getCorreoElectronico());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electronico ya esta registrado");
        }

        // 5. Validacion de saldo inicial
        if (dto.getSaldoInicial() == null || dto.getSaldoInicial().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El saldo inicial no puede ser negativo");
        }

        // 6. Construccion de la entidad ClienteEntity
        ClienteEntity cliente = ClienteEntity.builder()
                .nombre(dto.getNombre().trim())
                .segundoNombre(dto.getSegundoNombre() != null ? dto.getSegundoNombre().trim() : null)
                .apellidoPaterno(dto.getApellidoPaterno().trim())
                .apellidoMaterno(dto.getApellidoMaterno().trim())
                .fechaNacimiento(dto.getFechaNacimiento())
                .curp(dto.getCurp().trim().toUpperCase())
                .rfc(dto.getRfc().trim().toUpperCase())
                .sexo(dto.getSexo().trim().toUpperCase())
                .nacionalidad(dto.getNacionalidad().trim())
                .estadoCivil(dto.getEstadoCivil().trim().toUpperCase())
                .correoElectronico(dto.getCorreoElectronico().trim().toLowerCase())
                .telefonoMovil(dto.getTelefonoMovil().trim())
                .telefonoAlternativo(dto.getTelefonoAlternativo() != null && !dto.getTelefonoAlternativo().isBlank() ? dto.getTelefonoAlternativo().trim() : null)
                .calle(dto.getCalle().trim())
                .numeroExterior(dto.getNumeroExterior().trim())
                .numeroInterior(dto.getNumeroInterior() != null && !dto.getNumeroInterior().isBlank() ? dto.getNumeroInterior().trim() : null)
                .colonia(dto.getColonia().trim())
                .municipio(dto.getMunicipio().trim())
                .estado(dto.getEstado().trim())
                .codigoPostal(dto.getCodigoPostal().trim())
                .pais(dto.getPais().trim())
                .ocupacion(dto.getOcupacion().trim())
                .empresa(dto.getEmpresa().trim())
                .ingresoMensual(dto.getIngresoMensual())
                .activo(true)
                .build();

        ClienteEntity clienteGuardado = clienteRepository.save(cliente);

        // 7. Generacion de cuenta bancaria asociada
        String numeroCuenta = generarNumeroCuentaUnico();
        String clabe = generadorCuentaUtil.generarClabe(numeroCuenta);
        String tipoCuenta = (dto.getTipoCuenta() != null && !dto.getTipoCuenta().isBlank()) ? dto.getTipoCuenta().toUpperCase() : "DEBITO";

        CuentaBancariaEntity cuenta = CuentaBancariaEntity.builder()
                .cliente(clienteGuardado)
                .numeroCuenta(numeroCuenta)
                .clabe(clabe)
                .tipoCuenta(tipoCuenta)
                .saldo(dto.getSaldoInicial())
                .moneda("MXN")
                .estatus("ACTIVA")
                .build();

        CuentaBancariaEntity cuentaGuardada = cuentaBancariaRepository.save(cuenta);

        log.info("Cliente registrado exitosamente con ID: {}, cuenta bancaria asignada: {}", clienteGuardado.getId(), cuentaGuardada.getNumeroCuenta());

        // 8. Construccion y retorno del DTO de respuesta
        return mapearARespuestaDto(clienteGuardado, cuentaGuardada);
    }

    // Consulta todos los clientes registrados
    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public List<ClienteRegistroResponseDto> listarClientes() {
        return clienteRepository.findAll().stream()
                .map(c -> {
                    List<CuentaBancariaEntity> cuentas = cuentaBancariaRepository.findByClienteId(c.getId());
                    CuentaBancariaEntity cuenta = cuentas.isEmpty() ? null : cuentas.get(0);
                    return mapearARespuestaDto(c, cuenta);
                })
                .collect(Collectors.toList());
    }

    // Consulta un cliente por ID
    @Override
    @Transactional(readOnly = true, transactionManager = "sfTransactionManager")
    public ClienteRegistroResponseDto obtenerClientePorId(Long id) {
        ClienteEntity cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado con id " + id));

        List<CuentaBancariaEntity> cuentas = cuentaBancariaRepository.findByClienteId(cliente.getId());
        CuentaBancariaEntity cuenta = cuentas.isEmpty() ? null : cuentas.get(0);
        return mapearARespuestaDto(cliente, cuenta);
    }

    // Valida que el cliente sea mayor de edad (minimo 18 anos)
    private void validarMayoriaDeEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de nacimiento es obligatoria");
        }
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < 18) {
            log.warn("Intento de registro de menor de edad. Edad calculada: {} anos", edad);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cliente debe ser mayor de edad (minimo 18 anos)");
        }
    }

    // Genera un numero de cuenta bancaria que no exista previamente en la base de datos
    private String generarNumeroCuentaUnico() {
        String cuenta;
        int intentos = 0;
        do {
            cuenta = generadorCuentaUtil.generarNumeroCuenta();
            intentos++;
            if (intentos > 10) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar un numero de cuenta unico");
            }
        } while (cuentaBancariaRepository.existsByNumeroCuenta(cuenta));
        return cuenta;
    }

    // Mapea entidades ClienteEntity y CuentaBancariaEntity a ClienteRegistroResponseDto
    private ClienteRegistroResponseDto mapearARespuestaDto(ClienteEntity cliente, CuentaBancariaEntity cuenta) {
        StringBuilder nombreCompleto = new StringBuilder(cliente.getNombre());
        if (cliente.getSegundoNombre() != null && !cliente.getSegundoNombre().isBlank()) {
            nombreCompleto.append(" ").append(cliente.getSegundoNombre());
        }
        nombreCompleto.append(" ").append(cliente.getApellidoPaterno());
        nombreCompleto.append(" ").append(cliente.getApellidoMaterno());

        CuentaBancariaDto cuentaDto = null;
        if (cuenta != null) {
            cuentaDto = CuentaBancariaDto.builder()
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

        return ClienteRegistroResponseDto.builder()
                .codigo(0)
                .mensaje("Cliente registrado y cuenta bancaria creada con exito")
                .idCliente(cliente.getId())
                .nombreCompleto(nombreCompleto.toString())
                .curp(cliente.getCurp())
                .rfc(cliente.getRfc())
                .correoElectronico(cliente.getCorreoElectronico())
                .telefonoMovil(cliente.getTelefonoMovil())
                .fechaNacimiento(cliente.getFechaNacimiento())
                .ingresoMensual(cliente.getIngresoMensual())
                .cuentaBancaria(cuentaDto)
                .fechaRegistro(cliente.getFechaCreacion())
                .build();
    }
}
