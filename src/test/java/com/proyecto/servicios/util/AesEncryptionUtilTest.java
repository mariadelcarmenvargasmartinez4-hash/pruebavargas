package com.proyecto.servicios.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AesEncryptionUtilTest {

    private AesEncryptionUtil aesUtil;
    private static final String CLAVE_PRUEBA = "ClaveSecretaSuperSegura256BitsParaGestoPago!";
    private static final String CLAVE_DIFERENTE = "OtraClaveDiferente256BitsParaPruebaSecurity!";

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
    @DisplayName("Alteración o manipulación (tampering) del criptograma provoca fallo de autenticación AEAD")
    void alteracionCriptogramaFallaDescifrado() {
        String textoOriginal = "{\"numCuenta\":\"1234567890123456\",\"monto\":150.00}";
        String cifradoBase64 = aesUtil.encriptar(textoOriginal, CLAVE_PRUEBA);

        byte[] rawBytes = Base64.getDecoder().decode(cifradoBase64);
        // Modificar el último byte (parte del tag de autenticación GCM)
        rawBytes[rawBytes.length - 1] ^= (byte) 0xFF;
        String cifradoAlterado = Base64.getEncoder().encodeToString(rawBytes);

        assertThrows(IllegalStateException.class, () -> aesUtil.desencriptar(cifradoAlterado, CLAVE_PRUEBA));
    }

    @Test
    @DisplayName("Firma HMAC-SHA256 genera hash determinista y depende estrictamente de la clave secreta")
    void firmaHmacSha256GeneraFirmaValida() {
        String payload = "payload_de_prueba_de_pago";
        String firma1 = aesUtil.firmarPayload(payload, CLAVE_PRUEBA);
        String firma2 = aesUtil.firmarPayload(payload, CLAVE_PRUEBA);
        String firmaClaveDiferente = aesUtil.firmarPayload(payload, CLAVE_DIFERENTE);

        assertNotNull(firma1);
        assertEquals(firma1, firma2);
        assertTrue(firma1.matches("^[0-9A-F]{64}$"));

        // Comprobación de que la firma es un HMAC real dependiente de la clave y no un hash sin clave
        assertNotEquals(firma1, firmaClaveDiferente);
    }

    @Test
    @DisplayName("Verificación con vector conocido de HMAC-SHA256")
    void firmaHmacSha256VectorConocido() {
        // Vector conocido de prueba: payload "The quick brown fox jumps over the lazy dog", key "key"
        // HMAC-SHA256 = f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8
        String payload = "The quick brown fox jumps over the lazy dog";
        String key = "key";
        String firmaGenerada = aesUtil.firmarPayload(payload, key);

        assertEquals("F7BC83F430538424B13298E6AA6FB143EF4D59A14946175997479DBC2D1A3CD8", firmaGenerada);
    }
}
