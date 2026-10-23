package com.proyecto.servicios.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilidad de criptografía segura bajo estándares Senior y OWASP:
 * - Algoritmo AES/GCM/NoPadding (cifrado autenticado AEAD)
 * - Generación segura de Vector de Inicialización (IV) de 12 bytes mediante SecureRandom
 * - Firma digital de integridad HMAC-SHA256
 */
@Slf4j
@Component
public class AesEncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH_BYTE = 12;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Cifra un texto plano utilizando AES-GCM con IV aleatorio prepended.
     *
     * @param textoPlano Cadena a cifrar
     * @param claveSecreta Clave de cifrado en texto
     * @return Cadena cifrada en Base64 que contiene [IV (12 bytes) + CipherText + Tag]
     */
    public String encriptar(String textoPlano, String claveSecreta) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            secureRandom.nextBytes(iv);

            SecretKey secretKey = derivarClaveAes(claveSecreta);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Error durante el cifrado AES-GCM: {}", e.getMessage());
            throw new IllegalStateException("Fallo en la operación de cifrado de la transacción", e);
        }
    }

    /**
     * Descifra un texto cifrado en Base64 utilizando AES-GCM.
     *
     * @param textoCifradoBase64 Cadena en Base64
     * @param claveSecreta Clave secreta
     * @return Texto plano descifrado
     */
    public String desencriptar(String textoCifradoBase64, String claveSecreta) {
        try {
            byte[] combined = Base64.getDecoder().decode(textoCifradoBase64);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            int cipherTextSize = combined.length - iv.length;
            byte[] cipherText = new byte[cipherTextSize];
            System.arraycopy(combined, iv.length, cipherText, 0, cipherTextSize);

            SecretKey secretKey = derivarClaveAes(claveSecreta);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decrypted = cipher.doFinal(cipherText);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error durante el descifrado AES-GCM: {}", e.getMessage());
            throw new IllegalStateException("Fallo en la operación de descifrado de la transacción", e);
        }
    }

    /**
     * Genera una firma HMAC-SHA256 para garantizar la integridad y no repudio del pago.
     *
     * @param payload Datos a firmar
     * @param claveSecreta Clave para el HMAC
     * @return Firma en formato hexadecimal en mayúsculas
     */
    public String firmarPayload(String payload, String claveSecreta) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(claveSecreta.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(2 * rawHmac.length);
            for (byte b : rawHmac) {
                hex.append(String.format("%02X", b));
            }
            return hex.toString();
        } catch (Exception e) {
            log.error("Error al generar firma HMAC: {}", e.getMessage());
            throw new IllegalStateException("Fallo en la generación de firma de transacción", e);
        }
    }

    private SecretKey derivarClaveAes(String clave) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(clave.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
}
