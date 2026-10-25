package com.proyecto.servicios.model.pago;

import com.proyecto.servicios.util.DeclarativeAssertUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Record inmutable para la solicitud de pago de servicios con validación temprana Fail-Fast.
 */
@Schema(description = "Record inmutable para validación de pago de servicio o recarga")
public record PagoServicioRequestRecord(
        String numCuenta,
        Long idServicio,
        Long idProducto,
        Long idConcepto,
        Integer idDistribuidor,
        BigDecimal monto,
        String upe,
        Boolean bandera,
        String telefono,
        String referencia,
        String pin
) implements Serializable {

    public PagoServicioRequestRecord {
        DeclarativeAssertUtil.asegurar(numCuenta != null && !numCuenta.isBlank(),
                "El identificador de la cuenta (numCuenta) es obligatorio");
        DeclarativeAssertUtil.asegurar(idServicio != null && idServicio > 0,
                "El identificador del servicio (idServicio) es obligatorio y debe ser mayor a cero");
        DeclarativeAssertUtil.asegurar(idProducto != null && idProducto > 0,
                "El identificador del producto (idProducto) es obligatorio y debe ser mayor a cero");
        DeclarativeAssertUtil.asegurar(monto != null && monto.compareTo(BigDecimal.ZERO) > 0,
                "El precio o monto a pagar debe ser mayor a cero");
        DeclarativeAssertUtil.asegurar(upe != null && upe.matches("^\\d{10}$"),
                "El Identificador Único (UPE) es obligatorio y debe tener exactamente 10 dígitos numéricos");
        DeclarativeAssertUtil.asegurar(bandera != null,
                "La bandera de tipo de pago (true = recarga/teléfono, false = servicio con referencia) es obligatoria");

        // Regla condicional de negocio:
        // Bandera = true -> Teléfono obligatorio de 10 dígitos
        // Bandera = false -> Referencia obligatoria
        DeclarativeAssertUtil.asegurar(
                !Boolean.TRUE.equals(bandera) || (telefono != null && telefono.trim().matches("^\\d{10}$")),
                "Para recargas o pago telefónico (bandera = true), el teléfono es obligatorio y debe tener 10 dígitos");

        DeclarativeAssertUtil.asegurar(
                !Boolean.FALSE.equals(bandera) || (referencia != null && !referencia.trim().isBlank()),
                "Para pago de servicios con referencia (bandera = false), el campo referencia es obligatorio");

        DeclarativeAssertUtil.asegurar(pin != null && pin.trim().matches("^\\d{4,8}$"),
                "El PIN o pass seguro de autenticación es obligatorio y debe contener entre 4 y 8 dígitos numéricos");

        numCuenta = numCuenta.trim();
        upe = upe.trim();
        telefono = Optional.ofNullable(telefono).map(String::trim).filter(t -> !t.isBlank()).orElse(null);
        referencia = Optional.ofNullable(referencia).map(String::trim).filter(r -> !r.isBlank()).orElse(null);
        pin = pin.trim();
    }

    public static PagoServicioRequestRecord de(PagoServicioRequestDto dto) {
        BigDecimal montoFinal = Optional.ofNullable(dto.getMonto()).orElse(dto.getPrecio());
        String pinFinal = Optional.ofNullable(dto.getPin()).orElse(dto.getPassSeguro());
        Boolean banderaFinal = Optional.ofNullable(dto.getBandera()).orElse(dto.getEsRecargaTelefonica());
        Long conceptoFinal = dto.getIdConcepto();


        return new PagoServicioRequestRecord(
                dto.getNumCuenta(),
                dto.getIdServicio(),
                dto.getIdProducto(),
                conceptoFinal,
                dto.getIdDistribuidor(),
                montoFinal,
                dto.getUpe(),
                banderaFinal,
                dto.getTelefono(),
                dto.getReferencia(),
                pinFinal
        );
    }
}
