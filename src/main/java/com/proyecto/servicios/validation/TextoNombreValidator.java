package com.proyecto.servicios.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Validador declarativo de nombres y apellidos sin estructuras condicionales 'if'.
 */
public class TextoNombreValidator implements ConstraintValidator<ValidTextoNombre, String> {

    private String tipo;
    private boolean obligatorio;

    private static final Set<String> PALABRAS_PROHIBIDAS = Set.of(
            "EXECUTE", "EXEC", "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "ALTER",
            "CREATE", "TRUNCATE", "SCRIPT", "JAVASCRIPT", "NULL", "UNDEFINED", "TRUE",
            "FALSE", "SI", "NO", "EVAL", "WHERE", "FROM", "TABLE", "JOIN", "UNION",
            "GRANT", "REVOKE", "FETCH", "FUNCTION", "PROCEDURE", "DECLARE"
    );

    private record ReglaValidacion(Predicate<String> condicionFallo, String mensaje) {}

    @Override
    public void initialize(ValidTextoNombre constraintAnnotation) {
        this.tipo = constraintAnnotation.tipo();
        this.obligatorio = constraintAnnotation.obligatorio();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return Optional.ofNullable(value)
                .filter(Predicate.not(String::isEmpty))
                .map(v -> ejecutarReglasValidacion(v, context))
                .orElseGet(() -> !obligatorio || buildViolation(context, "El " + tipo + " es obligatorio y no debe estar vacio"));
    }

    private boolean ejecutarReglasValidacion(String value, ConstraintValidatorContext context) {
        List<ReglaValidacion> reglas = List.of(
                new ReglaValidacion(v -> v.startsWith(" ") || Character.isWhitespace(v.charAt(0)),
                        "El " + tipo + " no puede iniciar con espacios"),
                new ReglaValidacion(v -> v.endsWith(" ") || Character.isWhitespace(v.charAt(v.length() - 1)),
                        "El " + tipo + " no puede terminar con espacios"),
                new ReglaValidacion(v -> v.contains("  "),
                        "No se permiten multiples espacios internos consecutivos"),
                new ReglaValidacion(v -> v.trim().length() < 2,
                        "El " + tipo + " debe tener al menos 2 caracteres reales"),
                new ReglaValidacion(v -> v.length() > 50,
                        "El " + tipo + " no puede exceder 50 caracteres"),
                new ReglaValidacion(v -> !v.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+$"),
                        "El " + tipo + " solo puede aceptar letras"),
                new ReglaValidacion(v -> Arrays.stream(v.trim().split("\\s+"))
                        .anyMatch(p -> PALABRAS_PROHIBIDAS.contains(p.toUpperCase())),
                        "El " + tipo + " no puede ser una palabra reservada o de programacion")
        );

        return reglas.stream()
                .filter(r -> r.condicionFallo().test(value))
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
