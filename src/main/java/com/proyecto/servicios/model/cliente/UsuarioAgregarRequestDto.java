package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para crear un usuario de acceso para un cliente existente")
public class UsuarioAgregarRequestDto {

    @NotNull(message = "El identificador del cliente es obligatorio")
    private Long clienteId;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, max = 50, message = "La contrasena debe tener entre 8 y 50 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,50}$", message = "La contrasena debe contener mayuscula, minuscula, numero y caracter especial")
    private String password;
}