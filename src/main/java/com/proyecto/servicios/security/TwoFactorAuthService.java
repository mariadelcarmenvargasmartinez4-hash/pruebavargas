package com.proyecto.servicios.security;

import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de Autenticación Multifactor (2FA) y gestión de PINs temporales de un solo uso (OTP).
 * Genera claves numéricas de 6 dígitos con expiración de 5 minutos, almacenamiento cifrado mediante BCrypt
 * y mitigación de fuerza bruta con un límite estricto de 3 intentos.
 */
@Slf4j
@Service
public class TwoFactorAuthService {

    private static final int MAX_INTENTOS = 3;
    private static final long EXPIRACION_MINUTOS = 5;

    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, TwoFactorPinEntry> activeCodes = new ConcurrentHashMap<>();

    public TwoFactorAuthService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Genera un nuevo PIN temporal de 6 dígitos numéricos para el usuario especificado.
     * El PIN se almacena cifrado con BCrypt y expira en 5 minutos.
     *
     * @param username Correo o nombre de usuario
     * @return El PIN en texto plano para ser despachado al usuario (SMS/Email/Respuesta de prueba)
     */
    public String generarPinTemporal(String username) {
        if (username == null || username.isBlank()) {
            throw new ReglaNegocioException("El usuario es requerido para generar el PIN temporal 2FA");
        }

        String userKey = username.trim().toLowerCase();
        int pinNumero = 100000 + secureRandom.nextInt(900000); // 6 dígitos numéricos garantizados
        String pinPlano = String.valueOf(pinNumero);

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime expiracion = ahora.plus(EXPIRACION_MINUTOS, ChronoUnit.MINUTES);

        TwoFactorPinEntry entry = new TwoFactorPinEntry(passwordEncoder.encode(pinPlano), expiracion);
        activeCodes.put(userKey, entry);

        log.info("Código 2FA generado para el usuario '{}'. Expira en {} minutos", username, EXPIRACION_MINUTOS);
        return pinPlano;
    }

    /**
     * Valida el PIN temporal ingresado por el usuario.
     *
     * @param username     Usuario
     * @param pinIngresado PIN de 6 dígitos proporcionado
     * @return true si el PIN es correcto y válido
     */
    public boolean validarPinTemporal(String username, String pinIngresado) {
        if (username == null || pinIngresado == null) {
            throw new CredencialesInvalidasException("Usuario y PIN son obligatorios");
        }

        String userKey = username.trim().toLowerCase();
        TwoFactorPinEntry entry = activeCodes.get(userKey);

        if (entry == null) {
            throw new CredencialesInvalidasException("No hay ningún PIN temporal activo o ya ha expirado. Solicite uno nuevo.");
        }

        if (LocalDateTime.now().isAfter(entry.getExpiracion())) {
            activeCodes.remove(userKey);
            throw new CredencialesInvalidasException("El PIN temporal ha expirado. Solicite un nuevo código.");
        }

        if (entry.getIntentos() >= MAX_INTENTOS) {
            activeCodes.remove(userKey);
            throw new CredencialesInvalidasException("Ha superado el número máximo de intentos permitidos (3). El código fue invalidado.");
        }

        boolean coincide = passwordEncoder.matches(pinIngresado.trim(), entry.getPinHash());
        if (!coincide) {
            entry.incrementarIntentos();
            int restantes = MAX_INTENTOS - entry.getIntentos();
            if (restantes <= 0) {
                activeCodes.remove(userKey);
                throw new CredencialesInvalidasException("PIN temporal incorrecto. Ha superado los intentos y el código fue bloqueado.");
            }
            throw new CredencialesInvalidasException("PIN temporal incorrecto. Intentos restantes: " + restantes);
        }

        // Si fue exitoso, se consume el código (One-Time Password)
        activeCodes.remove(userKey);
        log.info("PIN temporal 2FA validado exitosamente para el usuario '{}'", username);
        return true;
    }

    public long getExpiracionSegundos() {
        return EXPIRACION_MINUTOS * 60;
    }

    @Getter
    @Setter
    private static class TwoFactorPinEntry {
        private final String pinHash;
        private final LocalDateTime expiracion;
        private int intentos;

        public TwoFactorPinEntry(String pinHash, LocalDateTime expiracion) {
            this.pinHash = pinHash;
            this.expiracion = expiracion;
            this.intentos = 0;
        }

        public void incrementarIntentos() {
            this.intentos++;
        }
    }
}
