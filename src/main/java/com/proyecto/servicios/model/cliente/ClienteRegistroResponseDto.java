package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// DTO de respuesta estructurada tras el registro exitoso del cliente y su cuenta bancaria
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta del registro de cliente persona física y su cuenta bancaria asociada")
public class ClienteRegistroResponseDto {

    @Schema(description = "Código de respuesta de la operación (0 = Éxito)", example = "0")
    private Integer codigo;

    @Schema(description = "Mensaje descriptivo del resultado", example = "Cliente registrado y cuenta bancaria creada con exito")
    private String mensaje;

    @Schema(description = "Identificador único asignado al cliente en base de datos", example = "1")
    private Long idCliente;

    @Schema(description = "Nombre completo del cliente", example = "Juan Carlos Perez Lopez")
    private String nombreCompleto;

    @Schema(description = "CURP registrado", example = "PELJ900515HDFRPR09")
    private String curp;

    @Schema(description = "RFC registrado", example = "PELJ9005151A2")
    private String rfc;

    @Schema(description = "Correo electrónico registrado", example = "juan.perez@example.com")
    private String correoElectronico;

    @Schema(description = "Teléfono móvil registrado", example = "5512345678")
    private String telefonoMovil;

    @Schema(description = "Fecha de nacimiento registrada", example = "1990-05-15")
    private LocalDate fechaNacimiento;

    @Schema(description = "Ingreso mensual reportado", example = "35000.00")
    private BigDecimal ingresoMensual;

    @Schema(description = "Información de la cuenta bancaria creada y saldo asignado")
    private CuentaBancariaDto cuentaBancaria;

    @Schema(description = "Fecha y hora del registro")
    private LocalDateTime fechaRegistro;
}
