package com.proyecto.servicios.model.cliente;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// DTO de peticion para el registro de cliente persona fisica y apertura de cuenta
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para el registro de cliente persona física y asignación de cuenta bancaria")
public class ClienteRegistroRequestDto {

    // Datos Personales
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
    @Schema(description = "Primer nombre", example = "Juan")
    private String nombre;

    @Size(max = 50, message = "El segundo nombre no puede exceder 50 caracteres")
    @Schema(description = "Segundo nombre (opcional)", example = "Carlos")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(max = 50, message = "El apellido paterno no puede exceder 50 caracteres")
    @Schema(description = "Apellido paterno", example = "Perez")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(max = 50, message = "El apellido materno no puede exceder 50 caracteres")
    @Schema(description = "Apellido materno", example = "Lopez")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Fecha de nacimiento (AAAA-MM-DD)", example = "1990-05-15")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El CURP es obligatorio")
    @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$", message = "El formato de CURP es invalido")
    @Size(min = 18, max = 18, message = "El CURP debe tener exactamente 18 caracteres")
    @Schema(description = "CURP oficial de 18 caracteres", example = "PELJ900515HDFRPR09")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = "^[A-ZÑ&]{4}\\d{6}[A-Z0-9]{3}$", message = "El formato de RFC para persona fisica es invalido")
    @Size(min = 13, max = 13, message = "El RFC debe tener exactamente 13 caracteres")
    @Schema(description = "RFC de persona física con homoclave (13 caracteres)", example = "PELJ9005151A2")
    private String rfc;

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "^(MASCULINO|FEMENINO|OTRO)$", message = "El sexo debe ser MASCULINO, FEMENINO u OTRO")
    @Schema(description = "Sexo del cliente", example = "MASCULINO")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    @Size(max = 50, message = "La nacionalidad no puede exceder 50 caracteres")
    @Schema(description = "Nacionalidad", example = "Mexicana")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    @Pattern(regexp = "^(SOLTERO|CASADO|DIVORCIADO|VIUDO|UNION_LIBRE)$", message = "El estado civil no es valido")
    @Schema(description = "Estado civil", example = "SOLTERO")
    private String estadoCivil;

    // Datos de Contacto
    @NotBlank(message = "El correo electronico es obligatorio")
    @Email(message = "El formato de correo electronico es invalido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    @Schema(description = "Correo electrónico principal", example = "juan.perez@example.com")
    private String correoElectronico;

    @NotBlank(message = "El telefono movil es obligatorio")
    @Pattern(regexp = "^\\d{10,15}$", message = "El telefono movil debe contener entre 10 y 15 digitos")
    @Schema(description = "Teléfono móvil a 10 dígitos", example = "5512345678")
    private String telefonoMovil;

    @Pattern(regexp = "^$|^\\d{10,15}$", message = "El telefono alternativo debe contener entre 10 y 15 digitos")
    @Schema(description = "Teléfono alternativo (opcional)", example = "5587654321")
    private String telefonoAlternativo;

    // Domicilio
    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 100, message = "La calle no puede exceder 100 caracteres")
    @Schema(description = "Calle de domicilio", example = "Av. Reforma")
    private String calle;

    @NotBlank(message = "El numero exterior es obligatorio")
    @Size(max = 20, message = "El numero exterior no puede exceder 20 caracteres")
    @Schema(description = "Número exterior", example = "123")
    private String numeroExterior;

    @Size(max = 20, message = "El numero interior no puede exceder 20 caracteres")
    @Schema(description = "Número interior (opcional)", example = "Piso 4, Depto B")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(max = 100, message = "La colonia no puede exceder 100 caracteres")
    @Schema(description = "Colonia", example = "Juarez")
    private String colonia;

    @NotBlank(message = "El municipio o alcaldia es obligatorio")
    @Size(max = 100, message = "El municipio no puede exceder 100 caracteres")
    @Schema(description = "Municipio o alcaldía", example = "Cuauhtemoc")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    @Size(max = 50, message = "El estado no puede exceder 50 caracteres")
    @Schema(description = "Estado o entidad federativa", example = "Ciudad de Mexico")
    private String estado;

    @NotBlank(message = "El codigo postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "El codigo postal debe contener exactamente 5 digitos")
    @Schema(description = "Código postal de 5 dígitos", example = "06600")
    private String codigoPostal;

    @NotBlank(message = "El pais es obligatorio")
    @Size(max = 50, message = "El pais no puede exceder 50 caracteres")
    @Schema(description = "País de residencia", example = "Mexico")
    private String pais;

    // Informacion Laboral
    @NotBlank(message = "La ocupacion es obligatoria")
    @Size(max = 100, message = "La ocupacion no puede exceder 100 caracteres")
    @Schema(description = "Ocupación o profesión", example = "Ingeniero de Software")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(max = 100, message = "La empresa no puede exceder 100 caracteres")
    @Schema(description = "Nombre de la empresa o empleador", example = "Tech Solutions SA")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.00", message = "El ingreso mensual no puede ser negativo")
    @Schema(description = "Ingreso mensual comprobable", example = "35000.00")
    private BigDecimal ingresoMensual;

    // Saldo Inicial de la cuenta bancaria
    @NotNull(message = "El saldo inicial de apertura es obligatorio")
    @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
    @Schema(description = "Saldo inicial asignado a la nueva cuenta bancaria", example = "1000.00")
    private BigDecimal saldoInicial;

    @Schema(description = "Tipo de cuenta (opcional, default DEBITO)", example = "DEBITO")
    private String tipoCuenta;
}
