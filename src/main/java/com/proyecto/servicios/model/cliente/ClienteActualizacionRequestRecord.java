package com.proyecto.servicios.model.cliente;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

/**
 * Record inmutable para la actualización parcial o total de datos de un cliente.
 * Aplica validaciones tempranas declarativas (Fail-Fast) en su constructor compacto.
 */
@Schema(description = "Record inmutable para actualización de cliente con validación temprana Fail-Fast")
public record ClienteActualizacionRequestRecord(
        String nombre,
        String segundoNombre,
        String apellidoPaterno,
        String apellidoMaterno,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate fechaNacimiento,
        String sexo,
        String nacionalidad,
        String estadoCivil,
        String correoElectronico,
        String telefonoMovil,
        String telefonoAlternativo,
        String calle,
        String numeroExterior,
        String numeroInterior,
        String colonia,
        String municipio,
        String estado,
        String codigoPostal,
        String pais,
        String ocupacion,
        String empresa,
        BigDecimal ingresoMensual
) implements Serializable {

    public ClienteActualizacionRequestRecord {
        // Validación declarativa de fecha de nacimiento si viene presente
        Optional.ofNullable(fechaNacimiento).ifPresent(f -> {
            DeclarativeAssertUtil.prohibir(f.isAfter(LocalDate.now()), "La fecha de nacimiento no puede ser una fecha futura");
            int edad = Period.between(f, LocalDate.now()).getYears();
            DeclarativeAssertUtil.asegurar(edad >= 18, "El cliente debe ser mayor de edad (18 años o más)");
            DeclarativeAssertUtil.asegurar(edad <= 100, "La fecha de nacimiento no debe exceder los 100 años");
        });

        // Correo electrónico
        Optional.ofNullable(correoElectronico).filter(c -> !c.isBlank()).ifPresent(c ->
                DeclarativeAssertUtil.asegurar(c.trim().matches("^[A-Za-z0-9+_.-]+@(.+)$"), "El formato de correo electrónico es inválido"));

        // Teléfono móvil
        Optional.ofNullable(telefonoMovil).filter(t -> !t.isBlank()).ifPresent(t ->
                DeclarativeAssertUtil.asegurar(t.trim().matches("^\\d{10}$"), "El teléfono móvil solo acepta 10 dígitos numéricos"));

        // Ingreso mensual
        Optional.ofNullable(ingresoMensual).ifPresent(i ->
                DeclarativeAssertUtil.asegurar(i.compareTo(BigDecimal.ZERO) > 0, "El ingreso mensual debe ser mayor a cero"));

        // Código postal
        Optional.ofNullable(codigoPostal).filter(cp -> !cp.isBlank()).ifPresent(cp ->
                DeclarativeAssertUtil.asegurar(cp.trim().matches("^\\d{5}$"), "El código postal debe contener exactamente 5 dígitos"));
    }

    public static ClienteActualizacionRequestRecord de(ClienteActualizacionRequestDto dto) {
        return new ClienteActualizacionRequestRecord(
                dto.getNombre(),
                dto.getSegundoNombre(),
                dto.getApellidoPaterno(),
                dto.getApellidoMaterno(),
                dto.getFechaNacimiento(),
                dto.getSexo(),
                dto.getNacionalidad(),
                dto.getEstadoCivil(),
                dto.getCorreoElectronico(),
                dto.getTelefonoMovil(),
                dto.getTelefonoAlternativo(),
                dto.getCalle(),
                dto.getNumeroExterior(),
                dto.getNumeroInterior(),
                dto.getColonia(),
                dto.getMunicipio(),
                dto.getEstado(),
                dto.getCodigoPostal(),
                dto.getPais(),
                dto.getOcupacion(),
                dto.getEmpresa(),
                dto.getIngresoMensual()
        );
    }
}
