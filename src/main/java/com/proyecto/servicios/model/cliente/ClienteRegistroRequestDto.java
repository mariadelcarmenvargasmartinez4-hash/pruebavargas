package com.proyecto.servicios.model.cliente;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.proyecto.servicios.validation.ValidFechaNacimiento;
import com.proyecto.servicios.validation.ValidTextoNombre;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// DTO de petición para el registro de cliente persona física, su domicilio y asignación de cuenta bancaria
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para el registro de cliente persona física, su domicilio y asignación de cuenta bancaria")
public class ClienteRegistroRequestDto {

    // Datos Personales con validación granular de nombres y apellidos
    @ValidTextoNombre(tipo = "nombre", obligatorio = true)
    @Schema(description = "Primer nombre", example = "Juan")
    private String nombre;

    @ValidTextoNombre(tipo = "segundoNombre", obligatorio = false)
    @Schema(description = "Segundo nombre (opcional)", example = "Carlos")
    private String segundoNombre;

    @ValidTextoNombre(tipo = "apellidoPaterno", obligatorio = true)
    @Schema(description = "Apellido paterno", example = "Perez")
    private String apellidoPaterno;

    @ValidTextoNombre(tipo = "apellidoMaterno", obligatorio = true)
    @Schema(description = "Apellido materno", example = "Lopez")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @ValidFechaNacimiento
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Fecha de nacimiento (AAAA-MM-DD)", example = "1990-05-15")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El CURP es obligatorio y no debe estar vacío")
    @Pattern(regexp = "^(?i)[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$", message = "El CURP no cumple con el formato oficial de 18 caracteres (ejemplo: AAAA000000HAAAAA00)")
    @Size(min = 18, max = 18, message = "El CURP debe contener exactamente 18 caracteres")
    @Schema(description = "CURP oficial de 18 caracteres alfanuméricos", example = "PELJ900515HDFRPR09")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio y no debe estar vacío")
    @Pattern(regexp = "^(?i)[A-ZÑ&]{4}\\d{6}[A-Z0-9]{2,3}$", message = "El RFC no cumple con el formato oficial de 12 o 13 caracteres (ejemplo: AAAA000000AAA)")
    @Size(min = 12, max = 13, message = "El RFC debe tener exactamente 12 o 13 caracteres")
    @Schema(description = "RFC de persona física (12 o 13 caracteres)", example = "PELJ9005151A2")
    private String rfc;

    @NotBlank(message = "El sexo es obligatorio y no debe estar vacío")
    @Size(max = 50, message = "El sexo no puede exceder 50 caracteres")
    @Schema(description = "Sexo del cliente validado contra catálogo", example = "MASCULINO")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria y no debe estar vacía")
    @Size(max = 100, message = "La nacionalidad no puede exceder 100 caracteres")
    @Schema(description = "Nacionalidad validada contra catálogo", example = "MEXICANA")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio y no debe estar vacío")
    @Size(max = 50, message = "El estado civil no puede exceder 50 caracteres")
    @Schema(description = "Estado civil validado contra catálogo", example = "SOLTERO")
    private String estadoCivil;

    // Datos de Contacto
    @NotBlank(message = "El correo electrónico es obligatorio y no debe estar vacío")
    @Email(message = "El formato de correo electrónico es inválido")
    @Size(max = 100, message = "El correo electrónico no puede exceder 100 caracteres")
    @Schema(description = "Correo electrónico principal", example = "juan.perez@example.com")
    private String correoElectronico;

    @NotBlank(message = "El teléfono móvil es obligatorio y no debe estar vacío")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono móvil solo acepta 10 dígitos numéricos (sin guiones ni espacios)")
    @Schema(description = "Teléfono móvil a 10 dígitos numéricos", example = "5512345678")
    private String telefonoMovil;

    @Pattern(regexp = "^$|^\\d{10,15}$", message = "El teléfono alternativo solo acepta dígitos numéricos (entre 10 y 15 dígitos)")
    @Schema(description = "Teléfono alternativo (opcional)", example = "5587654321")
    private String telefonoAlternativo;

    // Domicilio (Tabla domicilios)
    @NotBlank(message = "La calle es obligatoria y no debe estar vacía")
    @Size(max = 100, message = "La calle no puede exceder 100 caracteres")
    @Schema(description = "Calle de domicilio", example = "Av. Reforma")
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio y no debe estar vacío")
    @Size(max = 20, message = "El número exterior no puede exceder 20 caracteres")
    @Schema(description = "Número exterior", example = "123")
    private String numeroExterior;

    @Size(max = 20, message = "El número interior no puede exceder 20 caracteres")
    @Schema(description = "Número interior (opcional)", example = "Piso 4, Depto B")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria y no debe estar vacía")
    @Size(max = 100, message = "La colonia no puede exceder 100 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ .'-]{2,100}$", message = "La colonia solo puede contener letras, números y espacios válidos")
    @Schema(description = "Colonia", example = "Juarez")
    private String colonia;

    @NotBlank(message = "El municipio o alcaldía es obligatorio y no debe estar vacío")
    @Size(max = 100, message = "El municipio no puede exceder 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ .'-]{2,100}$", message = "El municipio o alcaldía solo puede contener letras y espacios válidos")
    @Schema(description = "Municipio o alcaldía", example = "Cuauhtemoc")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio y no debe estar vacío")
    @Size(max = 50, message = "El estado no puede exceder 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ .'-]{2,50}$", message = "El estado solo puede contener letras y espacios válidos")
    @Schema(description = "Estado o entidad federativa", example = "Ciudad de Mexico")
    private String estado;

    @NotBlank(message = "El código postal es obligatorio y no debe estar vacío")
    @Pattern(regexp = "^\\d{5}$", message = "El código postal debe contener exactamente 5 dígitos numéricos")
    @Schema(description = "Código postal de 5 dígitos numéricos", example = "06600")
    private String codigoPostal;

    @NotBlank(message = "El país es obligatorio y no debe estar vacío")
    @Size(max = 50, message = "El país no puede exceder 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ .'-]{2,50}$", message = "El país solo puede contener letras y espacios válidos")
    @Schema(description = "País de residencia", example = "Mexico")
    private String pais;

    // Información Laboral
    @NotBlank(message = "La ocupación es obligatoria y no debe estar vacía")
    @Size(max = 100, message = "La ocupación no puede exceder 100 caracteres")
    @Schema(description = "Ocupación o profesión", example = "Ingeniero de Software")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria y no debe estar vacía")
    @Size(max = 100, message = "La empresa no puede exceder 100 caracteres")
    @Schema(description = "Nombre de la empresa o empleador", example = "Tech Solutions SA")
    private String empresa;

    // Control estricto de límites numéricos decimales (evita desbordamientos en base de datos)
    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", inclusive = true, message = "El ingreso mensual debe ser mayor a cero")
    @DecimalMax(value = "9999999999.99", message = "El ingreso mensual excede el límite máximo permitido")
    @Digits(integer = 10, fraction = 2, message = "El campo 'ingresoMensual' debe ser un valor numerico decimal valido")
    @Schema(description = "Ingreso mensual comprobable (mayor a cero)", example = "35000.00")
    private BigDecimal ingresoMensual;

    // Saldo Inicial de la cuenta bancaria
    @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
    @DecimalMax(value = "999999999999.99", message = "El saldo inicial excede el límite máximo permitido")
    @Digits(integer = 12, fraction = 2, message = "El campo 'saldoInicial' debe ser un valor numerico decimal valido")
    @Schema(description = "Campo opcional; el sistema define el saldo inicial de la cuenta", example = "1000.00")
    private BigDecimal saldoInicial;

    @Pattern(regexp = "^$|^(?i)(DEBITO|CREDITO|AHORRO|NOMINA|CHEQUES)$", message = "El tipo de cuenta debe ser DEBITO, CREDITO, AHORRO o NOMINA")
    @Schema(description = "Tipo de cuenta (opcional, default DEBITO)", example = "DEBITO")
    private String tipoCuenta;

    // Contraseña para creación del usuario de acceso
    @NotBlank(message = "La contraseña para el usuario de acceso es obligatoria y no debe estar vacía")
    @Size(min = 8, max = 50, message = "La contraseña debe tener entre 8 y 50 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).{8,50}$", message = "La contraseña no cumple con los criterios de seguridad: debe contener al menos 8 caracteres, incluyendo una letra mayúscula, una letra minúscula, un número y un carácter especial sin espacios")
    @Schema(description = "Contraseña de acceso para el usuario del cliente", example = "Segura123#")
    private String password;
}
