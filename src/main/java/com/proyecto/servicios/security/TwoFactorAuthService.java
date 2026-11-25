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
import java.util.concurrent.CompletableFuture;

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
    private static final String LETRAS_MAYUSCULAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITOS = "0123456789";
    private static final String ALFANUMERICO = LETRAS_MAYUSCULAS + DIGITOS;

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
        return generarPinTemporal(username, true);
    }

    /**
     * @param aplicarCooldown true para el reenvío manual (anti-flooding de 60s); false en el login,
     *                        donde cada autenticación válida debe generar y enviar un código nuevo de inmediato.
     */
    public String generarPinTemporal(String username, boolean aplicarCooldown) {
        if (username == null || username.isBlank()) {
            throw new ReglaNegocioException("El usuario es requerido para generar el PIN temporal 2FA");
        }

        String userKey = username.trim().toLowerCase();
        int pinNumero = 100000 + secureRandom.nextInt(900000); // 6 dígitos numéricos garantizados
        String pinPlano = String.valueOf(pinNumero);

        LocalDateTime ahora = LocalDateTime.now();

        // Protección contra solicitudes excesivas (anti-flooding / rate-limiting 60s) solo en reenvíos manuales
        TwoFactorPinEntry entryExistente = activeCodes.get(userKey);
        if (aplicarCooldown && entryExistente != null && entryExistente.getFechaCreacion().plusSeconds(COOLDOWN_SEGUNDOS).isAfter(ahora)) {
            long segundosRestantes = ChronoUnit.SECONDS.between(ahora, entryExistente.getFechaCreacion().plusSeconds(COOLDOWN_SEGUNDOS));
            throw new ReglaNegocioException("Debe esperar " + Math.max(1, segundosRestantes) + " segundos antes de solicitar un nuevo código de verificación 2FA.");
        }

        LocalDateTime expiracion = ahora.plus(EXPIRACION_MINUTOS, ChronoUnit.MINUTES);

        TwoFactorPinEntry entry = new TwoFactorPinEntry(passwordEncoder.encode(pinPlano), ahora, expiracion);
        // Si ya había un código vigente (doble clic / segundo login), ambos códigos siguen siendo válidos
        if (entryExistente != null && ahora.isBefore(entryExistente.getExpiracion())) {
            entry.heredarCodigos(entryExistente);
        }
        activeCodes.put(userKey, entry);

        // Envío asíncrono: la respuesta del login no espera al servidor SMTP
        CompletableFuture.runAsync(() -> despacharCodigoSeguro(username, pinPlano));
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

        boolean coincide = entry.coincide(pinIngresado.trim(), passwordEncoder);
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

    private String generarCodigoAlfanumerico10() {
        char[] chars = new char[10];
        chars[0] = LETRAS_MAYUSCULAS.charAt(secureRandom.nextInt(LETRAS_MAYUSCULAS.length()));
        chars[1] = DIGITOS.charAt(secureRandom.nextInt(DIGITOS.length()));
        for (int i = 2; i < 10; i++) {
            chars[i] = ALFANUMERICO.charAt(secureRandom.nextInt(ALFANUMERICO.length()));
        }
        for (int i = chars.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }

    /**
     * Genera y despacha un código dinámico de un solo uso (OTP) de 10 caracteres alfanuméricos
     * (combinando letras y números) para autorizar una transacción financiera específica.
     *
     * @param destinatarioCorreo Correo electrónico del cliente titular
     * @param claveOperacion     Número de cuenta o identificador de operación
     * @param monto              Importe de la transacción
     * @param servicio           Nombre del servicio o concepto a pagar
     * @return El código plano de 10 caracteres (letras y números) generado
     */
    public String generarPinTransaccional(String destinatarioCorreo, String claveOperacion, BigDecimal monto, String servicio) {
        if (destinatarioCorreo == null || destinatarioCorreo.isBlank()) {
            throw new ReglaNegocioException("El correo del cliente es requerido para despachar el PIN dinámico de autorización");
        }
        if (claveOperacion == null || claveOperacion.isBlank()) {
            throw new ReglaNegocioException("La clave de operación o número de cuenta es requerida");
        }

        String key = "TX_" + claveOperacion.trim().toLowerCase();
        String pinPlano = generarCodigoAlfanumerico10();

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime expiracion = ahora.plus(EXPIRACION_MINUTOS, ChronoUnit.MINUTES);

        TwoFactorPinEntry entryExistente = activeCodes.get(key);
        TwoFactorPinEntry entry = new TwoFactorPinEntry(passwordEncoder.encode(pinPlano), ahora, expiracion);
        if (entryExistente != null && ahora.isBefore(entryExistente.getExpiracion())) {
            entry.heredarCodigos(entryExistente);
        }
        activeCodes.put(key, entry);

        CompletableFuture.runAsync(() -> despacharPinTransaccional(destinatarioCorreo, pinPlano, monto, servicio));
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
     * Valida el código dinámico de transacción ingresado (10 caracteres con letras y números).
     *
     * @param claveOperacion Identificador de la cuenta u operación
     * @param pinIngresado   Código de 10 caracteres introducido por el usuario
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

        String pinNormalizado = pinIngresado.trim().toUpperCase(Locale.ROOT);
        boolean coincide = entry.coincide(pinNormalizado, passwordEncoder);
        if (!coincide) {
            entry.incrementarIntentos();
            if (MAX_INTENTOS - entry.getIntentos() <= 0) {
                activeCodes.remove(key);
            }
            return false;
        }

        log.info("Código dinámico transaccional validado exitosamente para operación '{}'", claveOperacion);
        return true;
    }

    /**
     * Consume de forma definitiva el código OTP transaccional una vez aplicada la operación.
     */
    public void consumirPinTransaccional(String claveOperacion) {
        if (claveOperacion == null || claveOperacion.isBlank()) return;
        String key = "TX_" + claveOperacion.trim().toLowerCase();
        activeCodes.remove(key);
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

            String fromEffective = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom : "samantanavarro08@gmail.com";
            helper.setFrom(fromEffective, "Banca Digital GestoPago");
            mimeMessage.setHeader("X-Priority", "1");
            mimeMessage.setHeader("Importance", "High");
            helper.setTo(destinatario.trim());
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
            log.info("Correo HTML corporativo con PIN transaccional dinámico enviado exitosamente a '{}' [PIN: {}]", destinatario, codigo);
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

            String fromEffective = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom : "samantanavarro08@gmail.com";
            helper.setFrom(fromEffective, "Banca Digital GestoPago");
            mimeMessage.setHeader("X-Priority", "1");
            mimeMessage.setHeader("Importance", "High");
            helper.setTo(destinatario.trim());
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
            log.info("Correo HTML corporativo con código 2FA de login enviado exitosamente a '{}' [PIN: {}]", destinatario, codigo);
        } catch (Exception e) {
            log.warn("No fue posible despachar el correo HTML 2FA a '{}': {}. Fallback a log local: [{}]", destinatario, e.getMessage(), codigo);
        }
    }

    private static String escapar(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
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
                                  <span style="font-size: 38px; font-weight: 800; letter-spacing: 10px; color: #0B192C; font-family: 'Courier New', Courier, monospace; display: block;">@@CODIGO@@</span>
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
            """.replace("@@CODIGO@@", codigo);
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
                              <td style="padding: 12px 18px; border-bottom: 1px solid #EDF2F7; font-size: 13px; color: #0B192C; font-weight: 700;">@@SERVICIO@@</td>
                            </tr>
                            <tr>
                              <td style="padding: 12px 18px; border-bottom: 1px solid #EDF2F7; font-size: 13px; color: #64748B;">Total a Debitar:</td>
                              <td style="padding: 12px 18px; border-bottom: 1px solid #EDF2F7; font-size: 16px; color: #00875A; font-weight: 800;">@@MONTO@@</td>
                            </tr>
                            <tr>
                              <td style="padding: 12px 18px; font-size: 13px; color: #64748B;">Tipo de Validación:</td>
                              <td style="padding: 12px 18px; font-size: 13px; color: #0B192C; font-weight: 600;">PIN Dinámico OTP (One-Time Password)</td>
                            </tr>
                          </table>
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin: 15px 0 25px 0;">
                            <tr>
                              <td align="center">
                                <div style="background-color: #F0FDF4; border: 2px dashed #00875A; border-radius: 12px; padding: 22px 15px; max-width: 440px; text-align: center;">
                                  <span style="font-size: 11px; text-transform: uppercase; letter-spacing: 1px; color: #00875A; font-weight: 700; display: block; margin-bottom: 8px;">Código de Autorización Bancaria (10 caracteres)</span>
                                  <span style="font-size: 28px; font-weight: 800; letter-spacing: 5px; color: #0B192C; font-family: 'Courier New', Courier, monospace; display: block;">@@CODIGO@@</span>
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
            """.replace("@@SERVICIO@@", escapar(servicio)).replace("@@MONTO@@", escapar(monto)).replace("@@CODIGO@@", codigo);
    }

    @Getter
    @Setter
    private static class TwoFactorPinEntry {
        private final String pinHash;
        private final java.util.List<String> hashesPrevios = new java.util.concurrent.CopyOnWriteArrayList<>();
        private final LocalDateTime fechaCreacion;
        private final LocalDateTime expiracion;
        private int intentos;

        public TwoFactorPinEntry(String pinHash, LocalDateTime fechaCreacion, LocalDateTime expiracion) {
            this.pinHash = pinHash;
            this.fechaCreacion = fechaCreacion;
            this.expiracion = expiracion;
            this.intentos = 0;
        }

        // Conserva los códigos anteriores aún vigentes (p. ej. si el usuario inicia sesión dos veces seguidas)
        public void heredarCodigos(TwoFactorPinEntry anterior) {
            this.hashesPrevios.addAll(anterior.hashesPrevios);
            this.hashesPrevios.add(anterior.pinHash);
        }

        public boolean coincide(String pin, PasswordEncoder encoder) {
            if (encoder.matches(pin, pinHash)) return true;
            for (String h : hashesPrevios) {
                if (encoder.matches(pin, h)) return true;
            }
            return false;
        }

        public void incrementarIntentos() {
            this.intentos++;
        }
    }
}
