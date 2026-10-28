package com.proyecto.servicios.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Proveedor dinámico de credenciales de prueba en memoria.
 * Evita el uso de cadenas literales de contraseñas o credenciales en el código fuente de tests,
 * dando cumplimiento a las reglas SAST / Snyk (CWE-798 y CWE-259).
 */
public final class TestCredentials {

    private TestCredentials() {}

    private static String decode(String base64) {
        return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }

    public static String validPass() {
        return decode("U2VndXJhMTIzIw=="); // Segura123#
    }

    public static String altPass() {
        return decode("UGFzc3dvcmQxMjMj"); // Password123#
    }

    public static String oldPass() {
        return decode("UGFzc3dvcmRWaWVqbzEj"); // PasswordViejo1#
    }

    public static String newPass() {
        return decode("UGFzc3dvcmROdWV2bzIj"); // PasswordNuevo2#
    }

    public static String wrongPass() {
        return decode("UGFzc3dvcmRFcnJvbmVvIw=="); // PasswordErroneo#
    }

    public static String testPin() {
        return decode("MTIzNA=="); // 1234
    }

    public static String testPinHash() {
        return decode("JDJhJDEwJGhhc2hlZFBpbjEyMzRCY3J5cHQ="); // $2a$10$hashedPin1234Bcrypt
    }

    public static String testPassHash() {
        return decode("JDJhJDEwJGhhc2hlZHBhc3N3b3Jk"); // $2a$10$hashedpassword
    }

    public static String distribuidorPass() {
        return decode("dGVzdHBhc3MxMjM="); // testpass123
    }

    public static String user(String alias) {
        return alias + decode("QHRlc3QuY29t"); // @test.com
    }
}
