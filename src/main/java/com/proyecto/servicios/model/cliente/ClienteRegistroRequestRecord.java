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
 * Record inmutable para la captura y validación temprana (Fail-Fast) del registro de clientes.
 * Valida todas las reglas de negocio e integridad en su constructor compacto de forma declarativa,
 * garantizando que ninguna petición inválida alcance las interfaces o servicios.
 */
@Schema(description = "Record inmutable para registro de clientes con validación temprana Fail-Fast")
public record ClienteRegistroRequestRecord(
        @Schema(description = "Primer nombre", example = "Juan")
        String nombre,

        @Schema(description = "Segundo nombre (opcional)", example = "Carlos")
        String segundoNombre,

        @Schema(description = "Apellido paterno", example = "Perez")
        String apellidoPaterno,

        @Schema(description = "Apellido materno", example = "Lopez")
        String apellidoMaterno,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Fecha de nacimiento (AAAA-MM-DD)", example = "1990-05-15")
        LocalDate fechaNacimiento,

        @Schema(description = "CURP oficial de 18 caracteres alfanuméricos", example = "PELJ900515HDFRPR09")
        String curp,

        @Schema(description = "RFC de persona física (12 o 13 caracteres)", example = "PELJ9005151A2")
        String rfc,

        @Schema(description = "Sexo del cliente validado contra catálogo", example = "MASCULINO")
        String sexo,

        @Schema(description = "Nacionalidad validada contra catálogo", example = "MEXICANA")
        String nacionalidad,

        @Schema(description = "Estado civil validado contra catálogo", example = "SOLTERO")
        String estadoCivil,

        @Schema(description = "Correo electrónico principal", example = "juan.perez@example.com")
        String correoElectronico,

        @Schema(description = "Teléfono móvil a 10 dígitos numéricos", example = "5512345678")
        String telefonoMovil,

        @Schema(description = "Teléfono alternativo (opcional)", example = "5587654321")
        String telefonoAlternativo,

        @Schema(description = "Calle de domicilio", example = "Av. Reforma")
        String calle,

        @Schema(description = "Número exterior", example = "123")
        String numeroExterior,

        @Schema(description = "Número interior (opcional)", example = "4B")
        String numeroInterior,

        @Schema(description = "Colonia", example = "Juarez")
        String colonia,

        @Schema(description = "Municipio o alcaldía", example = "Cuauhtemoc")
        String municipio,

        @Schema(description = "Estado o entidad federativa", example = "Ciudad de Mexico")
        String estado,

        @Schema(description = "Código postal de 5 dígitos", example = "06600")
        String codigoPostal,

        @Schema(description = "País", example = "Mexico")
        String pais,

        @Schema(description = "Ocupación o profesión", example = "Ingeniero de Software")
        String ocupacion,

        @Schema(description = "Empresa o lugar de trabajo", example = "Tech Solutions SA")
        String empresa,

        @Schema(description = "Ingreso mensual neto comprobable", example = "35000.00")
        BigDecimal ingresoMensual,

        @Schema(description = "Saldo inicial de apertura", example = "1500.00")
        BigDecimal saldoInicial,

        @Schema(description = "Tipo de cuenta bancaria", example = "DEBITO")
        String tipoCuenta,

        @Schema(description = "Contraseña para usuario de acceso", example = "Passw0rd#2026")
        String password
) implements Serializable {

    /**
     * Constructor compacto: Ejecuta validación declarativa temprana sin estructuras 'if'.
     */
    public ClienteRegistroRequestRecord {
        // Nombres y Apellidos
        DeclarativeAssertUtil.asegurar(nombre != null && !nombre.isBlank(), "El nombre es obligatorio");
        DeclarativeAssertUtil.asegurar(apellidoPaterno != null && !apellidoPaterno.isBlank(), "El apellido paterno es obligatorio");
        DeclarativeAssertUtil.asegurar(apellidoMaterno != null && !apellidoMaterno.isBlank(), "El apellido materno es obligatorio");

        // Fecha de nacimiento y mayoría de edad
        DeclarativeAssertUtil.asegurar(fechaNacimiento != null, "La fecha de nacimiento es obligatoria");
        DeclarativeAssertUtil.prohibir(Optional.ofNullable(fechaNacimiento).map(f -> f.isAfter(LocalDate.now())).orElse(false),
                "La fecha de nacimiento no puede ser una fecha futura");

        Optional.ofNullable(fechaNacimiento).ifPresent(f -> {
            int edad = Period.between(f, LocalDate.now()).getYears();
            DeclarativeAssertUtil.asegurar(edad >= 18, "El cliente debe ser mayor de edad (18 años o más)");
            DeclarativeAssertUtil.asegurar(edad <= 100, "La fecha de nacimiento no debe exceder los 100 años");
        });

        // CURP y RFC formatos oficiales
        DeclarativeAssertUtil.asegurar(curp != null && curp.trim().matches("^(?i)[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$"),
                "El CURP no cumple con el formato oficial de 18 caracteres (ejemplo: AAAA000000HAAAAA00)");
        DeclarativeAssertUtil.asegurar(rfc != null && rfc.trim().matches("^(?i)[A-ZÑ&]{4}\\d{6}[A-Z0-9]{2,3}$"),
                "El RFC no cumple con el formato oficial de 12 o 13 caracteres (ejemplo: AAAA000000AAA)");

        // Catálogos
        DeclarativeAssertUtil.asegurar(sexo != null && !sexo.isBlank(), "El sexo es obligatorio y no debe estar vacío");
        DeclarativeAssertUtil.asegurar(nacionalidad != null && !nacionalidad.isBlank(), "La nacionalidad es obligatoria y no debe estar vacía");
        DeclarativeAssertUtil.asegurar(estadoCivil != null && !estadoCivil.isBlank(), "El estado civil es obligatorio y no debe estar vacío");

        // Contacto
        DeclarativeAssertUtil.asegurar(correoElectronico != null && correoElectronico.trim().matches("^[A-Za-z0-9+_.-]+@(.+)$"),
                "El formato de correo electrónico es inválido");
        DeclarativeAssertUtil.asegurar(telefonoMovil != null && telefonoMovil.trim().matches("^\\d{10}$"),
                "El teléfono móvil solo acepta 10 dígitos numéricos (sin guiones ni espacios)");

        // Domicilio
        DeclarativeAssertUtil.asegurar(calle != null && !calle.isBlank(), "La calle es obligatoria y no debe estar vacía");
        DeclarativeAssertUtil.asegurar(numeroExterior != null && !numeroExterior.isBlank(), "El número exterior es obligatorio");
        DeclarativeAssertUtil.asegurar(colonia != null && !colonia.isBlank(), "La colonia es obligatoria");
        DeclarativeAssertUtil.asegurar(municipio != null && !municipio.isBlank(), "El municipio es obligatorio");
        DeclarativeAssertUtil.asegurar(estado != null && !estado.isBlank(), "El estado es obligatorio");
        DeclarativeAssertUtil.asegurar(codigoPostal != null && codigoPostal.trim().matches("^\\d{5}$"),
                "El código postal debe contener exactamente 5 dígitos numéricos");
        DeclarativeAssertUtil.asegurar(pais != null && !pais.isBlank(), "El país es obligatorio");

        // Información Laboral
        DeclarativeAssertUtil.asegurar(ocupacion != null && !ocupacion.isBlank(), "La ocupación es obligatoria");
        DeclarativeAssertUtil.asegurar(empresa != null && !empresa.isBlank(), "La empresa es obligatoria");
        DeclarativeAssertUtil.asegurar(ingresoMensual != null && ingresoMensual.compareTo(BigDecimal.ZERO) > 0,
                "El ingreso mensual debe ser mayor a cero");

        // Saldo Inicial
        Optional.ofNullable(saldoInicial).ifPresent(s ->
                DeclarativeAssertUtil.asegurar(s.compareTo(BigDecimal.ZERO) >= 0, "El saldo inicial no puede ser negativo"));

        // Contraseña
        Optional.ofNullable(password).ifPresent(p ->
                DeclarativeAssertUtil.asegurar(p.length() >= 8, "La contraseña debe tener al menos 8 caracteres"));

        // Sanitización declarativa inmutable
        nombre = nombre.trim();
        segundoNombre = Optional.ofNullable(segundoNombre).map(String::trim).orElse(null);
        apellidoPaterno = apellidoPaterno.trim();
        apellidoMaterno = apellidoMaterno.trim();
        curp = curp.trim().toUpperCase();
        rfc = rfc.trim().toUpperCase();
        sexo = sexo.trim().toUpperCase();
        nacionalidad = nacionalidad.trim().toUpperCase();
        estadoCivil = estadoCivil.trim().toUpperCase();
        correoElectronico = correoElectronico.trim().toLowerCase();
        telefonoMovil = telefonoMovil.trim();
        telefonoAlternativo = Optional.ofNullable(telefonoAlternativo).map(String::trim).orElse(null);
        calle = calle.trim();
        numeroExterior = numeroExterior.trim();
        numeroInterior = Optional.ofNullable(numeroInterior).map(String::trim).orElse(null);
        colonia = colonia.trim();
        municipio = municipio.trim();
        estado = estado.trim();
        codigoPostal = codigoPostal.trim();
        pais = pais.trim();
        ocupacion = ocupacion.trim();
        empresa = empresa.trim();
        tipoCuenta = Optional.ofNullable(tipoCuenta).map(String::trim).map(String::toUpperCase).orElse("DEBITO");
    }

    public static ClienteRegistroRequestRecord de(ClienteRegistroRequestDto dto) {
        return new ClienteRegistroRequestRecord(
                dto.getNombre(),
                dto.getSegundoNombre(),
                dto.getApellidoPaterno(),
                dto.getApellidoMaterno(),
                dto.getFechaNacimiento(),
                dto.getCurp(),
                dto.getRfc(),
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
                dto.getIngresoMensual(),
                dto.getSaldoInicial(),
                dto.getTipoCuenta(),
                dto.getPassword()
        );
    }
}
