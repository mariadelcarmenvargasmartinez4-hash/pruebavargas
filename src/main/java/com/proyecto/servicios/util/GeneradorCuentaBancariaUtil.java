package com.proyecto.servicios.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

// Utileria para generacion de numeros de cuenta y claves interbancarias CLABE
@Component
public class GeneradorCuentaBancariaUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    // Codigo estandar de institucion bancaria (3 digitos ficticios: 012 = BBVA / 002 = Banamex, etc.)
    private static final String CODIGO_BANCO = "012";
    // Codigo de plaza sucursal (3 digitos)
    private static final String CODIGO_PLAZA = "180";

    // Genera un numero de cuenta bancaria unico de 10 digitos
    public String generarNumeroCuenta() {
        long numero = 1000000000L + (long) (RANDOM.nextDouble() * 8999999999L);
        return String.valueOf(numero);
    }

    // Genera una CLABE interbancaria valida de 18 digitos conforme al estandar mexicano
    public String generarClabe(String numeroCuenta10Digitos) {
        String base17 = CODIGO_BANCO + CODIGO_PLAZA + "0" + numeroCuenta10Digitos;
        int digitoVerificador = calcularDigitoVerificadorClabe(base17);
        return base17 + digitoVerificador;
    }

    // Calcula el digito verificador modulo 10 con ponderaciones 3, 7, 1 conforme a la norma de Banxico
    private int calcularDigitoVerificadorClabe(String base17) {
        int[] factores = {3, 7, 1};
        int suma = 0;
        for (int i = 0; i < base17.length(); i++) {
            int digito = Character.getNumericValue(base17.charAt(i));
            int factor = factores[i % 3];
            suma += (digito * factor) % 10;
        }
        int modulo = suma % 10;
        return (10 - modulo) % 10;
    }
}
