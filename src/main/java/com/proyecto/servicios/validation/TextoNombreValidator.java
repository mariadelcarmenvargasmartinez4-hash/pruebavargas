package com.proyecto.servicios.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TextoNombreValidator implements ConstraintValidator<ValidTextoNombre, String> {

    private String tipo;
    private boolean obligatorio;

    @Override
    public void initialize(ValidTextoNombre constraintAnnotation) {
        this.tipo = constraintAnnotation.tipo();
        this.obligatorio = constraintAnnotation.obligatorio();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            if (!obligatorio) {
                return true;
            }
            return buildViolation(context, "El " + tipo + " es obligatorio y no debe estar vacio");
        }

        // 1. Validar que no inicie ni termine con espacios en blanco
        if (value.startsWith(" ") || value.endsWith(" ")) {
            return buildViolation(context, "El " + tipo + " no debe iniciar ni terminar con espacios en blanco");
        }

        // 2. Validar que no contenga multiples espacios internos consecutivos
        if (value.contains("  ")) {
            return buildViolation(context, "No se permiten multiples espacios internos consecutivos");
        }

        // 3. Validar longitud minima de 2 caracteres reales
        if (value.trim().length() < 2) {
            return buildViolation(context, "El " + tipo + " debe tener al menos 2 caracteres reales");
        }

        // 4. Validar longitud maxima de 50 caracteres
        if (value.length() > 50) {
            return buildViolation(context, "El " + tipo + " no puede exceder 50 caracteres");
        }

        // 5. Validar que solo contenga letras y espacios
        if (!value.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+$")) {
            return buildViolation(context, "El " + tipo + " solo puede contener letras y espacios");
        }

        return true;
    }

    private boolean buildViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
