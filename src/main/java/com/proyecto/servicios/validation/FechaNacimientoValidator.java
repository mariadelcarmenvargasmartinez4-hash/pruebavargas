package com.proyecto.servicios.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

public class FechaNacimientoValidator implements ConstraintValidator<ValidFechaNacimiento, LocalDate> {

    @Override
    public boolean isValid(LocalDate fecha, ConstraintValidatorContext context) {
        if (fecha == null) {
            return true; // Se valida con @NotNull por separado si es obligatorio
        }

        LocalDate hoy = LocalDate.now();

        if (fecha.isAfter(hoy)) {
            return buildViolation(context, "La fecha de nacimiento no puede ser una fecha futura");
        }

        int edad = Period.between(fecha, hoy).getYears();

        if (edad < 18) {
            return buildViolation(context, "El cliente debe ser mayor de edad (18 años o más)");
        }

        if (edad > 100) {
            return buildViolation(context, "La fecha de nacimiento no debe exceder los 100 años");
        }

        return true;
    }

    private boolean buildViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
