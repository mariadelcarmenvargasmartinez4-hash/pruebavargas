package com.proyecto.servicios.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Validador declarativo de fecha de nacimiento y mayoría de edad sin estructuras 'if'.
 */
public class FechaNacimientoValidator implements ConstraintValidator<ValidFechaNacimiento, LocalDate> {

    private record ReglaFecha(Predicate<LocalDate> condicionFallo, String mensaje) {}

    @Override
    public boolean isValid(LocalDate fecha, ConstraintValidatorContext context) {
        return Optional.ofNullable(fecha)
                .map(f -> validarFecha(f, context))
                .orElse(true);
    }

    private boolean validarFecha(LocalDate fecha, ConstraintValidatorContext context) {
        LocalDate hoy = LocalDate.now();

        List<ReglaFecha> reglas = List.of(
                new ReglaFecha(f -> f.isAfter(hoy), "La fecha de nacimiento no puede ser una fecha futura"),
                new ReglaFecha(f -> Period.between(f, hoy).getYears() < 18, "El cliente debe ser mayor de edad (18 años o más)"),
                new ReglaFecha(f -> Period.between(f, hoy).getYears() > 100, "La fecha de nacimiento no debe exceder los 100 años")
        );

        return reglas.stream()
                .filter(r -> r.condicionFallo().test(fecha))
                .findFirst()
                .map(r -> buildViolation(context, r.mensaje()))
                .orElse(true);
    }

    private boolean buildViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
