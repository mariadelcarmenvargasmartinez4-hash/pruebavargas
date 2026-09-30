package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para representar el domicilio asociado a un cliente
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Domicilio del cliente")
public class DomicilioDto {

    @Schema(description = "Identificador del domicilio", example = "1")
    private Long idDomicilio;

    @Schema(description = "Calle", example = "Av. Reforma")
    private String calle;

    @Schema(description = "Número exterior", example = "123")
    private String numeroExterior;

    @Schema(description = "Número interior", example = "Piso 4, Depto B")
    private String numeroInterior;

    @Schema(description = "Colonia", example = "Juarez")
    private String colonia;

    @Schema(description = "Municipio o alcaldía", example = "Cuauhtemoc")
    private String municipio;

    @Schema(description = "Estado", example = "Ciudad de Mexico")
    private String estado;

    @Schema(description = "Código postal", example = "06600")
    private String codigoPostal;

    @Schema(description = "País", example = "Mexico")
    private String pais;
}
