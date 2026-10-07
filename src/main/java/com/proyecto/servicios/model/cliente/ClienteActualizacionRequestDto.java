package com.proyecto.servicios.model.cliente;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.proyecto.servicios.validation.ValidFechaNacimiento;
import com.proyecto.servicios.validation.ValidTextoNombre;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// DTO para la actualizacion parcial de un cliente (CURP y RFC no son modificables)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para actualización parcial de cliente (CURP y RFC no son modificables)")
public class ClienteActualizacionRequestDto {

    // Datos Personales
    @ValidTextoNombre(tipo = "nombre", obligatorio = false)
    @Schema(description = "Primer nombre", example = "Juan")
    private String nombre;

    @ValidTextoNombre(tipo = "segundoNombre", obligatorio = false)
    @Schema(description = "Segundo nombre (opcional)", example = "Carlos")
    private String segundoNombre;

    @ValidTextoNombre(tipo = "apellidoPaterno", obligatorio = false)
    @Schema(description = "Apellido paterno", example = "Perez")
    private String apellidoPaterno;

    @ValidTextoNombre(tipo = "apellidoMaterno", obligatorio = false)
    @Schema(description = "Apellido materno", example = "Lopez")
    private String apellidoMaterno;

    @ValidFechaNacimiento
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Fecha de nacimiento (AAAA-MM-DD)", example = "1990-05-15")
    private LocalDate fechaNacimiento;

    @Size(max = 50, message = "El sexo no puede exceder 50 caracteres")
    @Schema(description = "Sexo del cliente validado contra catálogo", example = "MASCULINO")
    private String sexo;

    @Size(max = 100, message = "La nacionalidad no puede exceder 100 caracteres")
    @Schema(description = "Nacionalidad validada contra catálogo", example = "Mexicana")
    private String nacionalidad;

    @Size(max = 50, message = "El estado civil no puede exceder 50 caracteres")
    @Schema(description = "Estado civil validado contra catálogo", example = "CASADO")
    private String estadoCivil;

    // Datos de Contacto
    @Email(message = "El formato de correo electrónico es inválido")
    @Size(max = 100, message = "El correo electrónico no puede exceder 100 caracteres")
    @Schema(description = "Correo electrónico principal", example = "juan.perez.nuevo@example.com")
    private String correoElectronico;

    @Pattern(regexp = "^$|^\\d{10}$", message = "El teléfono móvil debe contener exactamente 10 dígitos numéricos")
    @Schema(description = "Teléfono móvil a 10 dígitos numéricos", example = "5599887766")
    private String telefonoMovil;

    @Pattern(regexp = "^$|^\\d{10,15}$", message = "El teléfono alternativo debe ser numérico entre 10 y 15 dígitos")
    @Schema(description = "Teléfono alternativo (opcional)", example = "5511223344")
    private String telefonoAlternativo;

    // Domicilio
    @Size(max = 100, message = "La calle no puede exceder 100 caracteres")
    @Schema(description = "Calle de domicilio", example = "Av. Insurgentes Sur")
    private String calle;

    @Size(max = 20, message = "El número exterior no puede exceder 20 caracteres")
    @Schema(description = "Número exterior", example = "456")
    private String numeroExterior;

    @Size(max = 20, message = "El número interior no puede exceder 20 caracteres")
    @Schema(description = "Número interior (opcional)", example = "Piso 8")
    private String numeroInterior;

    @Size(max = 100, message = "La colonia no puede exceder 100 caracteres")
    @Schema(description = "Colonia", example = "Del Valle")
    private String colonia;

    @Size(max = 100, message = "El municipio no puede exceder 100 caracteres")
    @Schema(description = "Municipio o alcaldía", example = "Benito Juarez")
    private String municipio;

    @Size(max = 50, message = "El estado no puede exceder 50 caracteres")
    @Schema(description = "Estado o entidad federativa", example = "Ciudad de Mexico")
    private String estado;

    @Pattern(regexp = "^$|^\\d{5}$", message = "El código postal debe contener exactamente 5 dígitos numéricos")
    @Schema(description = "Código postal de 5 dígitos numéricos", example = "03100")
    private String codigoPostal;

    @Size(max = 50, message = "El país no puede exceder 50 caracteres")
    @Schema(description = "País de residencia", example = "Mexico")
    private String pais;

    // Información Laboral
    @Size(max = 100, message = "La ocupación no puede exceder 100 caracteres")
    @Schema(description = "Ocupación o profesión", example = "Líder Técnico")
    private String ocupacion;

    @Size(max = 100, message = "La empresa no puede exceder 100 caracteres")
    @Schema(description = "Nombre de la empresa o empleador", example = "Banco Innovador SA")
    private String empresa;

    @DecimalMin(value = "0.01", inclusive = true, message = "El ingreso mensual debe ser mayor a cero")
    @DecimalMax(value = "9999999999.99", message = "El ingreso mensual excede el límite máximo permitido")
    @Digits(integer = 10, fraction = 2, message = "El campo 'ingresoMensual' debe ser un valor numerico decimal valido")
    @Schema(description = "Ingreso mensual comprobable (mayor a cero)", example = "45000.00")
    private BigDecimal ingresoMensual;
}
