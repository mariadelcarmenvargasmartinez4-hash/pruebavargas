package com.proyecto.servicios.security;

import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
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
    private static final long COOLDOWN_SEGUNDOS = 60;

    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, TwoFactorPinEntry> activeCodes = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public TwoFactorAuthService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
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

        // Protección contra solicitudes excesivas (anti-flooding / rate-limiting 60s)
        TwoFactorPinEntry entryExistente = activeCodes.get(userKey);
        if (entryExistente != null && entryExistente.getFechaCreacion().plusSeconds(COOLDOWN_SEGUNDOS).isAfter(ahora)) {
            long segundosRestantes = ChronoUnit.SECONDS.between(ahora, entryExistente.getFechaCreacion().plusSeconds(COOLDOWN_SEGUNDOS));
            throw new ReglaNegocioException("Debe esperar " + Math.max(1, segundosRestantes) + " segundos antes de solicitar un nuevo código de verificación 2FA.");
        }

        LocalDateTime expiracion = ahora.plus(EXPIRACION_MINUTOS, ChronoUnit.MINUTES);

        TwoFactorPinEntry entry = new TwoFactorPinEntry(passwordEncoder.encode(pinPlano), ahora, expiracion);
        activeCodes.put(userKey, entry);

        despacharCodigoSeguro(username, pinPlano);
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

    /**
     * Genera y despacha un PIN dinámico de un solo uso (OTP) para autorizar una transacción financiera específica.
     * Se almacena bajo una clave única por cuenta/operación y se envía al correo registrado del titular.
     *
     * @param destinatarioCorreo Correo electrónico del cliente titular
     * @param claveOperacion     Número de cuenta o identificador de operación
     * @param monto              Importe de la transacción
     * @param servicio           Nombre del servicio o concepto a pagar
     * @return El PIN plano de 6 dígitos generado
     */
    public String generarPinTransaccional(String destinatarioCorreo, String claveOperacion, BigDecimal monto, String servicio) {
        if (destinatarioCorreo == null || destinatarioCorreo.isBlank()) {
            throw new ReglaNegocioException("El correo del cliente es requerido para despachar el PIN dinámico de autorización");
        }
        if (claveOperacion == null || claveOperacion.isBlank()) {
            throw new ReglaNegocioException("La clave de operación o número de cuenta es requerida");
        }

        String key = "TX_" + claveOperacion.trim().toLowerCase();
        int pinNumero = 100000 + secureRandom.nextInt(900000); // 6 dígitos numéricos
        String pinPlano = String.valueOf(pinNumero);

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime expiracion = ahora.plus(EXPIRACION_MINUTOS, ChronoUnit.MINUTES);

        TwoFactorPinEntry entry = new TwoFactorPinEntry(passwordEncoder.encode(pinPlano), ahora, expiracion);
        activeCodes.put(key, entry);

        despacharPinTransaccional(destinatarioCorreo, pinPlano, monto, servicio);
        return pinPlano;
    }

    /**
     * Verifica si existe un PIN dinámico activo y no expirado para la operación dada.
     */
    public boolean tienePinTransaccionalActivo(String claveOperacion) {
        if (claveOperacion == null || claveOperacion.isBlank()) return false;
        String key = "TX_" + claveOperacion.trim().toLowerCase();
        TwoFactorPinEntry entry = activeCodes.get(key);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.getExpiracion())) {
            activeCodes.remove(key);
            return false;
        }
        return true;
    }

    /**
     * Valida el PIN dinámico de transacción ingresado y, de ser correcto, lo consume de inmediato (OTP).
     *
     * @param claveOperacion Identificador de la cuenta u operación
     * @param pinIngresado   PIN de 6 dígitos introducido por el usuario
     * @return true si es válido y no expirado
     */
    public boolean validarPinTransaccional(String claveOperacion, String pinIngresado) {
        if (claveOperacion == null || pinIngresado == null) return false;
        String key = "TX_" + claveOperacion.trim().toLowerCase();
        TwoFactorPinEntry entry = activeCodes.get(key);
        if (entry == null) return false;

        if (LocalDateTime.now().isAfter(entry.getExpiracion())) {
            activeCodes.remove(key);
            return false;
        }

        if (entry.getIntentos() >= MAX_INTENTOS) {
            activeCodes.remove(key);
            return false;
        }

        boolean coincide = passwordEncoder.matches(pinIngresado.trim(), entry.getPinHash());
        if (!coincide) {
            entry.incrementarIntentos();
            if (MAX_INTENTOS - entry.getIntentos() <= 0) {
                activeCodes.remove(key);
            }
            return false;
        }

        // Éxito: One-Time Password consumido de inmediato
        activeCodes.remove(key);
        log.info("PIN dinámico transaccional validado y consumido exitosamente para operación '{}'", claveOperacion);
        return true;
    }

    private void despacharPinTransaccional(String destinatario, String codigo, BigDecimal monto, String servicio) {
        String servicioNombre = (servicio != null && !servicio.isBlank()) ? servicio : "Servicio GestoPago";
        String montoStr = (monto != null) ? String.format(Locale.ROOT, "$%.2f MXN", monto) : "";

        if (mailSender == null) {
            log.info("Canal SMTP no configurado: El PIN dinámico de autorización para '{}' es [{}] (Pago de {} por {}). Expira en {} minutos",
                    destinatario, codigo, servicioNombre, montoStr, EXPIRACION_MINUTOS);
            return;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) {
                mensaje.setFrom(mailFrom);
            }
            mensaje.setTo(destinatario);
            mensaje.setSubject("PIN de Autorización de Transacción - Banca Digital");
            mensaje.setText("Estimado cliente,\n\n"
                    + "Se ha solicitado una autorización de pago en su cuenta de Banca Digital:\n\n"
                    + "   • Servicio/Concepto: " + servicioNombre + "\n"
                    + (montoStr.isEmpty() ? "" : "   • Importe: " + montoStr + "\n")
                    + "   • PIN Dinámico de Autorización: " + codigo + "\n\n"
                    + "Este PIN es de un solo uso y expirará en " + EXPIRACION_MINUTOS + " minutos.\n"
                    + "Por motivos de seguridad, nunca comparta este código con nadie.\n\n"
                    + "Si usted no solicitó esta operación, por favor contacte a soporte inmediatamente.\n\n"
                    + "Atentamente,\n"
                    + "Seguridad Bancaria - GestoPago Digital");
            mailSender.send(mensaje);
            log.info("PIN transaccional dinámico enviado exitosamente por correo a '{}'", destinatario);
        } catch (Exception e) {
            log.warn("No fue posible despachar el correo con PIN transaccional a '{}': {}. Fallback a log: [{}]", destinatario, e.getMessage(), codigo);
        }
    }

    public long getExpiracionSegundos() {
        return EXPIRACION_MINUTOS * 60;
    }

    private void despacharCodigoSeguro(String destinatario, String codigo) {
        if (mailSender == null) {
            log.info("Canal SMTP no configurado: El código 2FA para '{}' es [{}] (Modo desarrollo). Expira en {} minutos",
                    destinatario, codigo, EXPIRACION_MINUTOS);
            return;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) {
                mensaje.setFrom(mailFrom);
            }
            mensaje.setTo(destinatario);
            mensaje.setSubject("Código de Verificación 2FA - Banca Digital");
            mensaje.setText("Estimado usuario,\n\n"
                    + "Su código de verificación de doble factor (2FA) para acceder al sistema es:\n\n"
                    + "   " + codigo + "\n\n"
                    + "Este código es de un solo uso y expirará en " + EXPIRACION_MINUTOS + " minutos.\n"
                    + "Por motivos de seguridad, nunca comparta este código con nadie.\n\n"
                    + "Si usted no solicitó este código, por favor ignore este mensaje.\n\n"
                    + "Atentamente,\n"
                    + "Equipo de Seguridad - Banca Digital");
            mailSender.send(mensaje);
            log.info("Código 2FA enviado exitosamente por correo electrónico a '{}'", destinatario);
        } catch (Exception e) {
            log.warn("No fue posible despachar el correo 2FA a '{}': {}. Fallback a log local: [{}]", destinatario, e.getMessage(), codigo);
        }
    }

    @Getter
    @Setter
    private static class TwoFactorPinEntry {
        private final String pinHash;
        private final LocalDateTime fechaCreacion;
        private final LocalDateTime expiracion;
        private int intentos;

        public TwoFactorPinEntry(String pinHash, LocalDateTime fechaCreacion, LocalDateTime expiracion) {
            this.pinHash = pinHash;
            this.fechaCreacion = fechaCreacion;
            this.expiracion = expiracion;
            this.intentos = 0;
        }

        public void incrementarIntentos() {
            this.intentos++;
        }
    }
}
