package com.proyecto.servicios.security;

import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

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
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom, "Banca Digital GestoPago");
            }
            helper.setTo(destinatario);
            helper.setSubject("PIN de Autorización de Transacción - Banca Digital");

            String textoPlano = "Estimado cliente,\n\n"
                    + "Se ha solicitado una autorización de pago en su cuenta de Banca Digital:\n\n"
                    + "   • Servicio/Concepto: " + servicioNombre + "\n"
                    + (montoStr.isEmpty() ? "" : "   • Importe: " + montoStr + "\n")
                    + "   • PIN Dinámico de Autorización: " + codigo + "\n\n"
                    + "Este PIN es de un solo uso y expirará en " + EXPIRACION_MINUTOS + " minutos.\n"
                    + "Por motivos de seguridad, nunca comparta este código con nadie.\n\n"
                    + "Si usted no solicitó esta operación, por favor contacte a soporte inmediatamente.\n\n"
                    + "Atentamente,\n"
                    + "Seguridad Bancaria - GestoPago Digital";

            String htmlBody = construirHtmlPinTransaccional(servicioNombre, montoStr, codigo);
            helper.setText(textoPlano, htmlBody);

            mailSender.send(mimeMessage);
            log.info("Correo HTML corporativo con PIN transaccional dinámico enviado exitosamente a '{}'", destinatario);
        } catch (Exception e) {
            log.warn("No fue posible despachar el correo HTML con PIN transaccional a '{}': {}. Fallback a log: [{}]", destinatario, e.getMessage(), codigo);
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
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom, "Banca Digital GestoPago");
            }
            helper.setTo(destinatario);
            helper.setSubject("Código de Verificación 2FA - Banca Digital");

            String textoPlano = "Estimado usuario,\n\n"
                    + "Su código de verificación de doble factor (2FA) para acceder al sistema es:\n\n"
                    + "   " + codigo + "\n\n"
                    + "Este código es de un solo uso y expirará en " + EXPIRACION_MINUTOS + " minutos.\n"
                    + "Por motivos de seguridad, nunca comparta este código con nadie.\n\n"
                    + "Si usted no solicitó este código, por favor ignore este mensaje.\n\n"
                    + "Atentamente,\n"
                    + "Equipo de Seguridad - Banca Digital";

            String htmlBody = construirHtmlLogin2fa(codigo);
            helper.setText(textoPlano, htmlBody);

            mailSender.send(mimeMessage);
            log.info("Correo HTML corporativo con código 2FA de login enviado exitosamente a '{}'", destinatario);
        } catch (Exception e) {
            log.warn("No fue posible despachar el correo HTML 2FA a '{}': {}. Fallback a log local: [{}]", destinatario, e.getMessage(), codigo);
        }
    }

    private String construirHtmlLogin2fa(String codigo) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Código de Verificación 2FA</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #F0F4F8; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #2D3748;">
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color: #F0F4F8; padding: 35px 12px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%" style="max-width: 560px; background-color: #FFFFFF; border-radius: 14px; overflow: hidden; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08); border: 1px solid #E2E8F0;" cellspacing="0" cellpadding="0">
                      <tr>
                        <td style="background: linear-gradient(135deg, #0B192C 0%, #1E3E62 100%); padding: 32px 25px; text-align: center;">
                          <div style="display: inline-block; width: 48px; height: 48px; background: rgba(255, 255, 255, 0.12); border-radius: 12px; line-height: 48px; font-size: 24px;">🛡️</div>
                          <h1 style="color: #FFFFFF; font-size: 21px; font-weight: 700; margin: 12px 0 4px 0; letter-spacing: 0.5px;">BANCA DIGITAL GESTOPAGO</h1>
                          <p style="color: #94A3B8; font-size: 11px; margin: 0; text-transform: uppercase; letter-spacing: 1.5px; font-weight: 600;">Autenticación Segura de Doble Factor</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 35px 35px 25px 35px;">
                          <h2 style="font-size: 17px; color: #0B192C; margin: 0 0 12px 0; font-weight: 600;">Código de Verificación de Acceso</h2>
                          <p style="font-size: 14px; line-height: 1.6; color: #4A5568; margin: 0 0 24px 0;">
                            Se ha detectado una solicitud de inicio de sesión en tu cuenta. Para confirmar tu identidad y acceder a la plataforma, ingresa el siguiente código de seguridad:
                          </p>
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin: 20px 0 25px 0;">
                            <tr>
                              <td align="center">
                                <div style="background-color: #F0F7FF; border: 2px dashed #0056B3; border-radius: 12px; padding: 22px 15px; max-width: 360px; text-align: center;">
                                  <span style="font-size: 11px; text-transform: uppercase; letter-spacing: 1px; color: #0056B3; font-weight: 700; display: block; margin-bottom: 8px;">Código de Verificación 2FA</span>
                                  <span style="font-size: 38px; font-weight: 800; letter-spacing: 10px; color: #0B192C; font-family: 'Courier New', Courier, monospace; display: block;">%s</span>
                                  <span style="font-size: 12px; color: #64748B; margin-top: 8px; display: block; font-weight: 500;">⏱️ Válido durante 5 minutos • Un solo uso</span>
                                </div>
                              </td>
                            </tr>
                          </table>
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color: #FFFBEB; border-left: 4px solid #F59E0B; border-radius: 6px; margin: 20px 0 10px 0;">
                            <tr>
                              <td style="padding: 12px 16px;">
                                <p style="font-size: 12px; line-height: 1.5; color: #92400E; margin: 0;">
                                  <strong>⚠️ Aviso de Seguridad:</strong> Nunca compartas este código con ninguna persona. El personal de soporte nunca te solicitará este PIN por teléfono o mensaje. Si no fuiste tú, te sugerimos cambiar tu contraseña de inmediato.
                                </p>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                      <tr>
                        <td style="background-color: #F8FAFC; padding: 20px 30px; text-align: center; border-top: 1px solid #E2E8F0;">
                          <p style="font-size: 11px; color: #94A3B8; margin: 0 0 6px 0;">
                            Mensaje automático del Sistema de Banca Digital GestoPago.
                          </p>
                          <p style="font-size: 11px; color: #CBD5E1; margin: 0;">
                            © 2026 Banca Digital GestoPago S.A. de C.V. Todos los derechos reservados.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(codigo);
    }

    private String construirHtmlPinTransaccional(String servicio, String monto, String codigo) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>PIN de Autorización de Transacción</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #F0F4F8; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #2D3748;">
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color: #F0F4F8; padding: 35px 12px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%" style="max-width: 560px; background-color: #FFFFFF; border-radius: 14px; overflow: hidden; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08); border: 1px solid #E2E8F0;" cellspacing="0" cellpadding="0">
                      <tr>
                        <td style="background: linear-gradient(135deg, #0B192C 0%, #1E3E62 100%); padding: 32px 25px; text-align: center;">
                          <div style="display: inline-block; width: 48px; height: 48px; background: rgba(255, 255, 255, 0.12); border-radius: 12px; line-height: 48px; font-size: 24px;">💳</div>
                          <h1 style="color: #FFFFFF; font-size: 21px; font-weight: 700; margin: 12px 0 4px 0; letter-spacing: 0.5px;">BANCA DIGITAL GESTOPAGO</h1>
                          <p style="color: #94A3B8; font-size: 11px; margin: 0; text-transform: uppercase; letter-spacing: 1.5px; font-weight: 600;">Autorización Segura de Pago</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 35px 35px 25px 35px;">
                          <h2 style="font-size: 17px; color: #0B192C; margin: 0 0 12px 0; font-weight: 600;">Autorización de Transacción Financiera</h2>
                          <p style="font-size: 14px; line-height: 1.6; color: #4A5568; margin: 0 0 20px 0;">
                            Se ha iniciado una transacción desde tu cuenta de banca digital. Revisa los datos de la operación e introduce el código PIN para confirmar:
                          </p>
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 10px; margin-bottom: 22px; overflow: hidden;">
                            <tr>
                              <td style="padding: 12px 18px; border-bottom: 1px solid #EDF2F7; font-size: 13px; color: #64748B; width: 40%;">Servicio / Destino:</td>
                              <td style="padding: 12px 18px; border-bottom: 1px solid #EDF2F7; font-size: 13px; color: #0B192C; font-weight: 700;">%s</td>
                            </tr>
                            <tr>
                              <td style="padding: 12px 18px; border-bottom: 1px solid #EDF2F7; font-size: 13px; color: #64748B;">Total a Debitar:</td>
                              <td style="padding: 12px 18px; border-bottom: 1px solid #EDF2F7; font-size: 16px; color: #00875A; font-weight: 800;">%s</td>
                            </tr>
                            <tr>
                              <td style="padding: 12px 18px; font-size: 13px; color: #64748B;">Tipo de Validación:</td>
                              <td style="padding: 12px 18px; font-size: 13px; color: #0B192C; font-weight: 600;">PIN Dinámico OTP (One-Time Password)</td>
                            </tr>
                          </table>
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin: 15px 0 25px 0;">
                            <tr>
                              <td align="center">
                                <div style="background-color: #F0FDF4; border: 2px dashed #00875A; border-radius: 12px; padding: 22px 15px; max-width: 360px; text-align: center;">
                                  <span style="font-size: 11px; text-transform: uppercase; letter-spacing: 1px; color: #00875A; font-weight: 700; display: block; margin-bottom: 8px;">PIN de Autorización Bancaria</span>
                                  <span style="font-size: 38px; font-weight: 800; letter-spacing: 10px; color: #0B192C; font-family: 'Courier New', Courier, monospace; display: block;">%s</span>
                                  <span style="font-size: 12px; color: #64748B; margin-top: 8px; display: block; font-weight: 500;">⏱️ Expira en 5 minutos • De un solo uso</span>
                                </div>
                              </td>
                            </tr>
                          </table>
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color: #FEF2F2; border-left: 4px solid #EF4444; border-radius: 6px; margin: 15px 0 10px 0;">
                            <tr>
                              <td style="padding: 12px 16px;">
                                <p style="font-size: 12px; line-height: 1.5; color: #991B1B; margin: 0;">
                                  <strong>🛡️ Protección Antifraude:</strong> Si tú no solicitaste este pago o desconoces el monto/concepto, <strong>NO ingreses este PIN</strong>. Tu dinero está seguro y ningún cargo se aplicará sin esta clave.
                                </p>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                      <tr>
                        <td style="background-color: #F8FAFC; padding: 20px 30px; text-align: center; border-top: 1px solid #E2E8F0;">
                          <p style="font-size: 11px; color: #94A3B8; margin: 0 0 6px 0;">
                            Notificación confidencial emitida por Banca Digital GestoPago.
                          </p>
                          <p style="font-size: 11px; color: #CBD5E1; margin: 0;">
                            © 2026 Banca Digital GestoPago S.A. de C.V. Todos los derechos reservados.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(servicio, monto, codigo);
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
