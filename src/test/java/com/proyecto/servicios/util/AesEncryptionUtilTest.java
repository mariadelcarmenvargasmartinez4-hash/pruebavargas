package com.proyecto.servicios.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AesEncryptionUtilTest {

    private AesEncryptionUtil aesUtil;
    private static final String CLAVE_PRUEBA = "ClaveSecretaSuperSegura256BitsParaGestoPago!";

    @BeforeEach
    void setUp() {
        aesUtil = new AesEncryptionUtil();
    }

    @Test
    @DisplayName("Cifrado y descifrado AES-GCM conserva la integridad del payload")
    void cifradoYDescifradoAesGcmExitoso() {
        String textoOriginal = "{\"numCuenta\":\"1234567890123456\",\"monto\":150.00,\"upe\":\"1234567890\"}";

        String cifradoBase64 = aesUtil.encriptar(textoOriginal, CLAVE_PRUEBA);

        assertNotNull(cifradoBase64);
        assertNotEquals(textoOriginal, cifradoBase64);

        String textoDescifrado = aesUtil.desencriptar(cifradoBase64, CLAVE_PRUEBA);

        assertEquals(textoOriginal, textoDescifrado);
    }

    @Test
    @DisplayName("Firma HMAC-SHA256 genera hash determinista no nulo")
    void firmaHmacSha256GeneraFirmaValida() {
        String payload = "payload_de_prueba_de_pago";
        String firma1 = aesUtil.firmarPayload(payload, CLAVE_PRUEBA);
        String firma2 = aesUtil.firmarPayload(payload, CLAVE_PRUEBA);

        assertNotNull(firma1);
        assertEquals(firma1, firma2);
        assertTrue(firma1.matches("^[0-9A-F]{64}$"));
    }
}
