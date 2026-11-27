package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.entity.cliente.RecuperacionPasswordEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.exception.PasswordInvalidaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.auth.RecuperacionRespuestaDto;
import com.proyecto.servicios.model.auth.RecuperacionRestablecerDto;
import com.proyecto.servicios.model.auth.RecuperacionSolicitudDto;
import com.proyecto.servicios.model.auth.RecuperacionVerificacionDto;
import com.proyecto.servicios.repositorys.cliente.RecuperacionPasswordRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import com.proyecto.servicios.security.UserSessionService;
import com.proyecto.servicios.service.RecuperacionPasswordService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class RecuperacionPasswordServiceImpl implements RecuperacionPasswordService {

    private static final long EXPIRACION_CODIGO_MINUTOS = 10;
    private static final long EXPIRACION_RESET_MINUTOS = 10;
    private static final long COOLDOWN_SOLICITUD_SEGUNDOS = 45;
    private static final int MAX_INTENTOS = 3;
    private static final String REGEX_PASSWORD_SEGURA = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).{8,50}$";

    private final UsuarioAccesoRepository usuarioRepository;
    private final RecuperacionPasswordRepository recuperacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionService userSessionService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public RecuperacionPasswordServiceImpl(UsuarioAccesoRepository usuarioRepository,
                                           RecuperacionPasswordRepository recuperacionRepository,
                                           PasswordEncoder passwordEncoder,
                                           UserSessionService userSessionService) {
        this.usuarioRepository = usuarioRepository;
        this.recuperacionRepository = recuperacionRepository;
        this.passwordEncoder = passwordEncoder;
        this.userSessionService = userSessionService;
    }

    @Override
    @Transactional
    public RecuperacionRespuestaDto solicitarRecuperacion(RecuperacionSolicitudDto request) {
        String correoLimpio = request.getCorreo().trim().toLowerCase(Locale.ROOT);
        LocalDateTime ahora = LocalDateTime.now();

        Optional<UsuarioAccesoEntity> usuarioOpt = usuarioRepository.findByUsername(correoLimpio);

        // Mitigación contra enumeración de cuentas: solo se procesa internamente si existe y está activo,
        // pero hacia el exterior siempre se responde de manera uniforme.
        if (usuarioOpt.isPresent() && Boolean.TRUE.equals(usuarioOpt.get().getActivo())) {
            UsuarioAccesoEntity usuario = usuarioOpt.get();

            Optional<RecuperacionPasswordEntity> recienteOpt =
                    recuperacionRepository.findFirstByUsuarioIdAndUsadoFalseOrderByFechaCreacionDesc(usuario.getId());

            boolean enCooldown = recienteOpt.isPresent()
                    && recienteOpt.get().getFechaCreacion() != null
                    && recienteOpt.get().getFechaCreacion().plusSeconds(COOLDOWN_SOLICITUD_SEGUNDOS).isAfter(ahora)
                    && recienteOpt.get().getFechaExpiracion().isAfter(ahora);

            if (!enCooldown) {
                // Invalidar cualquier código previo activo de este usuario
                List<RecuperacionPasswordEntity> previos =
                        recuperacionRepository.findByUsuarioIdAndUsadoFalse(usuario.getId());
                for (RecuperacionPasswordEntity previo : previos) {
                    previo.setUsado(true);
                    previo.setResetTokenHash(null);
                }
                if (!previos.isEmpty()) {
                    recuperacionRepository.saveAll(previos);
                }

                String codigoPlano = String.valueOf(100000 + secureRandom.nextInt(900000));
                RecuperacionPasswordEntity nueva = RecuperacionPasswordEntity.builder()
                        .usuario(usuario)
                        .codigoHash(passwordEncoder.encode(codigoPlano))
                        .intentosFallidos(0)
                        .maxIntentos(MAX_INTENTOS)
                        .verificado(false)
                        .usado(false)
                        .fechaCreacion(ahora)
                        .fechaExpiracion(ahora.plusMinutes(EXPIRACION_CODIGO_MINUTOS))
                        .build();

                recuperacionRepository.save(nueva);
                CompletableFuture.runAsync(() -> enviarCorreoRecuperacion(usuario.getUsername(), codigoPlano));
            }
        }

        return RecuperacionRespuestaDto.builder()
                .expiracionSegundos(EXPIRACION_CODIGO_MINUTOS * 60)
                .verificado(false)
                .resetToken(null)
                .build();
    }

    @Override
    @Transactional(noRollbackFor = {ReglaNegocioException.class, PasswordInvalidaException.class})
    public RecuperacionRespuestaDto verificarCodigo(RecuperacionVerificacionDto request) {
        String correoLimpio = request.getCorreo().trim().toLowerCase(Locale.ROOT);
        String codigoIngresado = request.getCodigo().trim();
        LocalDateTime ahora = LocalDateTime.now();

        UsuarioAccesoEntity usuario = usuarioRepository.findByUsername(correoLimpio)
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new ReglaNegocioException("El código de verificación es incorrecto o ha expirado."));

        RecuperacionPasswordEntity rec = recuperacionRepository
                .findFirstByUsuarioIdAndUsadoFalseOrderByFechaCreacionDesc(usuario.getId())
                .orElseThrow(() -> new ReglaNegocioException("El código de verificación es incorrecto o ha expirado."));

        if (ahora.isAfter(rec.getFechaExpiracion())) {
            rec.setUsado(true);
            recuperacionRepository.save(rec);
            throw new ReglaNegocioException("El código de recuperación ha expirado. Solicite un nuevo código.");
        }

        if (rec.getIntentosFallidos() >= rec.getMaxIntentos()) {
            rec.setUsado(true);
            recuperacionRepository.save(rec);
            throw new ReglaNegocioException("Se superó el límite de intentos permitidos. Solicite un nuevo código.");
        }

        if (!passwordEncoder.matches(codigoIngresado, rec.getCodigoHash())) {
            int nuevosIntentos = rec.getIntentosFallidos() + 1;
            rec.setIntentosFallidos(nuevosIntentos);
            int restantes = rec.getMaxIntentos() - nuevosIntentos;
            if (restantes <= 0) {
                rec.setUsado(true);
                recuperacionRepository.save(rec);
                throw new ReglaNegocioException("Código incorrecto. Se agotaron los intentos permitidos y el código fue invalidado.");
            }
            recuperacionRepository.save(rec);
            throw new ReglaNegocioException("Código incorrecto. Intentos restantes: " + restantes);
        }

        byte[] tokenBytes = new byte[24];
        secureRandom.nextBytes(tokenBytes);
        String resetTokenPlano = HexFormat.of().formatHex(tokenBytes);

        rec.setVerificado(true);
        rec.setResetTokenHash(passwordEncoder.encode(resetTokenPlano));
        rec.setFechaExpiracionReset(ahora.plusMinutes(EXPIRACION_RESET_MINUTOS));
        recuperacionRepository.save(rec);

        return RecuperacionRespuestaDto.builder()
                .expiracionSegundos(EXPIRACION_RESET_MINUTOS * 60)
                .verificado(true)
                .resetToken(resetTokenPlano)
                .build();
    }

    @Override
    @Transactional(noRollbackFor = {ReglaNegocioException.class, PasswordInvalidaException.class})
    public void restablecerPassword(RecuperacionRestablecerDto request) {
        String nuevaPassword = request.getNuevaPassword();
        String confirmarPassword = request.getConfirmarPassword();

        if (nuevaPassword == null || !nuevaPassword.equals(confirmarPassword)) {
            throw new PasswordInvalidaException("Las contraseñas no coinciden");
        }

        if (!nuevaPassword.matches(REGEX_PASSWORD_SEGURA)) {
            throw new PasswordInvalidaException(
                    "La contraseña debe tener entre 8 y 50 caracteres, al menos 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial sin espacios");
        }

        String correoLimpio = request.getCorreo().trim().toLowerCase(Locale.ROOT);
        LocalDateTime ahora = LocalDateTime.now();

        UsuarioAccesoEntity usuario = usuarioRepository.findByUsername(correoLimpio)
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new ReglaNegocioException("No existe una solicitud de recuperación válida. Solicite un nuevo código."));

        RecuperacionPasswordEntity rec = recuperacionRepository
                .findFirstByUsuarioIdAndUsadoFalseOrderByFechaCreacionDesc(usuario.getId())
                .orElseThrow(() -> new ReglaNegocioException("No existe una solicitud de recuperación válida. Solicite un nuevo código."));

        if (!Boolean.TRUE.equals(rec.getVerificado())
                || rec.getResetTokenHash() == null
                || rec.getFechaExpiracionReset() == null) {
            throw new ReglaNegocioException("Debe validar primero el código de verificación enviado a su correo.");
        }

        if (ahora.isAfter(rec.getFechaExpiracionReset())) {
            rec.setUsado(true);
            rec.setResetTokenHash(null);
            recuperacionRepository.save(rec);
            throw new ReglaNegocioException("El tiempo para cambiar la contraseña ha expirado. Inicie nuevamente la recuperación.");
        }

        if (!passwordEncoder.matches(request.getResetToken().trim(), rec.getResetTokenHash())) {
            rec.setUsado(true);
            rec.setResetTokenHash(null);
            recuperacionRepository.save(rec);
            throw new ReglaNegocioException("Comprobante de verificación inválido. Solicite un nuevo código.");
        }

        // Actualizar hash BCrypt en usuarios_acceso e invalidar código/token de recuperación definitivamente
        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);

        rec.setUsado(true);
        rec.setResetTokenHash(null);
        recuperacionRepository.save(rec);

        // Cerrar sesiones previas por seguridad
        userSessionService.invalidarSesion(usuario.getUsername());
        log.info("Contraseña restablecida exitosamente mediante recuperación por correo para '{}'", usuario.getUsername());
    }

    private void enviarCorreoRecuperacion(String destinatario, String codigo) {
        if (mailSender == null) {
            log.warn("Canal SMTP no configurado (spring.mail.* ausente): no se pudo despachar correo de recuperación a '{}'", destinatario);
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
            helper.setSubject("Recuperación de Contraseña - Banca Digital GestoPago");

            String textoPlano = "Estimado usuario,\n\n"
                    + "Se recibió una solicitud para restablecer la contraseña de su cuenta en Banca Digital GestoPago.\n\n"
                    + "Su código temporal de verificación es:\n\n"
                    + "   " + codigo + "\n\n"
                    + "Este código es válido durante " + EXPIRACION_CODIGO_MINUTOS + " minutos y puede utilizarse una sola vez.\n"
                    + "Si usted no solicitó recuperar su contraseña, ignore este mensaje; su cuenta permanece segura.\n\n"
                    + "Atentamente,\n"
                    + "Equipo de Seguridad - Banca Digital GestoPago";

            String htmlBody = construirHtmlRecuperacion(codigo);
            helper.setText(textoPlano, htmlBody);

            mailSender.send(mimeMessage);
            log.info("Correo HTML de recuperación de contraseña enviado exitosamente a '{}'", destinatario);
        } catch (Exception e) {
            log.warn("Error al enviar correo de recuperación a '{}': {}", destinatario, e.getMessage());
        }
    }

    private String construirHtmlRecuperacion(String codigo) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Recuperación de Contraseña</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #F0F4F8; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #2D3748;">
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color: #F0F4F8; padding: 35px 12px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%" style="max-width: 560px; background-color: #FFFFFF; border-radius: 14px; overflow: hidden; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08); border: 1px solid #E2E8F0;" cellspacing="0" cellpadding="0">
                      <tr>
                        <td style="background: linear-gradient(135deg, #0B192C 0%, #1E3E62 100%); padding: 32px 25px; text-align: center;">
                          <div style="display: inline-block; width: 48px; height: 48px; background: rgba(255, 255, 255, 0.12); border-radius: 12px; line-height: 48px; font-size: 24px;">🔐</div>
                          <h1 style="color: #FFFFFF; font-size: 21px; font-weight: 700; margin: 12px 0 4px 0; letter-spacing: 0.5px;">BANCA DIGITAL GESTOPAGO</h1>
                          <p style="color: #94A3B8; font-size: 11px; margin: 0; text-transform: uppercase; letter-spacing: 1.5px; font-weight: 600;">Recuperación Segura de Contraseña</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 35px 35px 25px 35px;">
                          <h2 style="font-size: 17px; color: #0B192C; margin: 0 0 12px 0; font-weight: 600;">Código Temporal de Recuperación</h2>
                          <p style="font-size: 14px; line-height: 1.6; color: #4A5568; margin: 0 0 24px 0;">
                            Recibimos una solicitud para restablecer la contraseña de tu cuenta. Ingresa el siguiente código en la aplicación para continuar:
                          </p>
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin: 20px 0 25px 0;">
                            <tr>
                              <td align="center">
                                <div style="background-color: #F0F7FF; border: 2px dashed #0056B3; border-radius: 12px; padding: 22px 15px; max-width: 360px; text-align: center;">
                                  <span style="font-size: 11px; text-transform: uppercase; letter-spacing: 1px; color: #0056B3; font-weight: 700; display: block; margin-bottom: 8px;">Código de Recuperación</span>
                                  <span style="font-size: 38px; font-weight: 800; letter-spacing: 10px; color: #0B192C; font-family: 'Courier New', Courier, monospace; display: block;">@@CODIGO@@</span>
                                  <span style="font-size: 12px; color: #64748B; margin-top: 8px; display: block; font-weight: 500;">⏱️ Válido durante 10 minutos • Un solo uso</span>
                                </div>
                              </td>
                            </tr>
                          </table>
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color: #FFFBEB; border-left: 4px solid #F59E0B; border-radius: 6px; margin: 20px 0 10px 0;">
                            <tr>
                              <td style="padding: 12px 16px;">
                                <p style="font-size: 12px; line-height: 1.5; color: #92400E; margin: 0;">
                                  <strong>⚠️ Aviso de Seguridad:</strong> Nunca compartas este código con nadie. Si no solicitaste cambiar tu contraseña, puedes ignorar este correo de forma segura.
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
}
