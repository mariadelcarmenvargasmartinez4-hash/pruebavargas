package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// DTO de respuesta estructurada para cliente, domicilio y sus cuentas bancarias asociadas
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta estructurada de cliente persona física, su domicilio y cuentas asociadas")
public class ClienteRegistroResponseDto {

    @Schema(description = "Código de respuesta (0 = Éxito)", example = "0")
    private Integer codigo;

    @Schema(description = "Mensaje descriptivo del resultado", example = "Operación realizada con éxito")
    private String mensaje;

    @Schema(description = "Identificador único del cliente", example = "1")
    private Long idCliente;

    @Schema(description = "Nombre completo del cliente", example = "Juan Carlos Perez Lopez")
    private String nombreCompleto;

    @Schema(description = "Primer nombre", example = "Juan")
    private String nombre;

    @Schema(description = "Segundo nombre", example = "Carlos")
    private String segundoNombre;

    @Schema(description = "Apellido paterno", example = "Perez")
    private String apellidoPaterno;

    @Schema(description = "Apellido materno", example = "Lopez")
    private String apellidoMaterno;

    @Schema(description = "CURP registrado", example = "PELJ900515HDFRPR09")
    private String curp;

    @Schema(description = "RFC registrado", example = "PELJ9005151A2")
    private String rfc;

    @Schema(description = "Sexo", example = "MASCULINO")
    private String sexo;

    @Schema(description = "Nacionalidad", example = "Mexicana")
    private String nacionalidad;

    @Schema(description = "Estado civil", example = "SOLTERO")
    private String estadoCivil;

    @Schema(description = "Correo electrónico registrado", example = "juan.perez@example.com")
    private String correoElectronico;

    @Schema(description = "Teléfono móvil registrado", example = "5512345678")
    private String telefonoMovil;

    @Schema(description = "Teléfono alternativo", example = "5587654321")
    private String telefonoAlternativo;

    @Schema(description = "Fecha de nacimiento registrada", example = "1990-05-15")
    private LocalDate fechaNacimiento;

    @Schema(description = "Ocupación", example = "Ingeniero de Software")
    private String ocupacion;

    @Schema(description = "Empresa", example = "Tech Solutions SA")
    private String empresa;

    @Schema(description = "Ingreso mensual reportado", example = "35000.00")
    private BigDecimal ingresoMensual;

    @Schema(description = "Estatus activo del cliente (baja lógica)", example = "true")
    private Boolean activo;

    @Schema(description = "Domicilio asociado al cliente")
    private DomicilioDto domicilio;

    @Schema(description = "Cuenta bancaria principal creada o consultada")
    private CuentaBancariaDto cuentaBancaria;

    @Schema(description = "Todas las cuentas asociadas al cliente")
    private List<CuentaBancariaDto> cuentas;

    @Schema(description = "Fecha y hora del registro")
    private LocalDateTime fechaRegistro;

    @Schema(description = "Fecha y hora de última actualización")
    private LocalDateTime fechaActualizacion;
}
