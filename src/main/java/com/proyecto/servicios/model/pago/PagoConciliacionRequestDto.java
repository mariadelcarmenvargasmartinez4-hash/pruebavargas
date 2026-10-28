package com.proyecto.servicios.model.pago;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO para la consulta y conciliación de transacciones de pago de servicios.
 * Recibe el identificador único UPE de 10 dígitos en el cuerpo JSON de la petición.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para consulta y conciliación de transacciones de pago")
public class PagoConciliacionRequestDto implements Serializable {

    @NotBlank(message = "El Identificador Único (UPE) es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El Identificador Único (UPE) debe tener exactamente 10 dígitos numéricos")
    @Schema(description = "Identificador único de 10 dígitos de la transacción", example = "1234567890")
    private String upe;
}
