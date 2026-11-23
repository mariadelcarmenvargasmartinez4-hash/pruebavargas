package com.proyecto.servicios.security;

import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TwoFactorAuthServiceTest {

    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    private TwoFactorAuthService twoFactorAuthService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        twoFactorAuthService = new TwoFactorAuthService(passwordEncoder);
        twoFactorAuthService.setMailSender(mailSender);
        twoFactorAuthService.setMailFrom("no-reply@bancadigital.com");
    }

    @Test
    @DisplayName("Generar PIN temporal despacha correo con formato profesional y emite código de 6 dígitos")
    void generarPinTemporal_Exitoso() {
        String correo = "test.user@example.com";
        String pin = twoFactorAuthService.generarPinTemporal(correo);

        assertNotNull(pin);
        assertEquals(6, pin.length());
        assertTrue(pin.matches("\\d{6}"));

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage mensaje = messageCaptor.getValue();
        assertEquals(correo, mensaje.getTo()[0]);
        assertEquals("no-reply@bancadigital.com", mensaje.getFrom());
        assertEquals("Código de Verificación 2FA - Banca Digital", mensaje.getSubject());
        assertTrue(mensaje.getText().contains(pin));
        assertTrue(mensaje.getText().contains("5 minutos"));
    }

    @Test
    @DisplayName("Generar PIN temporal con username nulo o vacío lanza ReglaNegocioException")
    void generarPinTemporal_UsernameInvalido_LanzaExcepcion() {
        assertThrows(ReglaNegocioException.class, () -> twoFactorAuthService.generarPinTemporal(null));
        assertThrows(ReglaNegocioException.class, () -> twoFactorAuthService.generarPinTemporal("   "));
    }

    @Test
    @DisplayName("Solicitar segundo PIN dentro del periodo de cooldown de 60 segundos lanza ReglaNegocioException")
    void generarPinTemporal_CooldownActivo_LanzaExcepcion() {
        String correo = "cooldown.user@example.com";
        twoFactorAuthService.generarPinTemporal(correo);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> twoFactorAuthService.generarPinTemporal(correo));
        assertTrue(ex.getMessage().contains("segundos antes de solicitar un nuevo código"));
    }

    @Test
    @DisplayName("Validar PIN correcto consume el código y retorna true")
    void validarPinTemporal_Correcto_RetornaTrueYConsumeCodigo() {
        String correo = "valido.user@example.com";
        String pin = twoFactorAuthService.generarPinTemporal(correo);

        boolean esValido = twoFactorAuthService.validarPinTemporal(correo, pin);
        assertTrue(esValido);

        // Al consumirse el OTP, un segundo intento debe fallar indicando que no hay PIN activo
        assertThrows(CredencialesInvalidasException.class,
                () -> twoFactorAuthService.validarPinTemporal(correo, pin));
    }

    @Test
    @DisplayName("Validar PIN con argumentos nulos lanza CredencialesInvalidasException")
    void validarPinTemporal_ArgumentosNulos_LanzaExcepcion() {
        assertThrows(CredencialesInvalidasException.class,
                () -> twoFactorAuthService.validarPinTemporal(null, "123456"));
        assertThrows(CredencialesInvalidasException.class,
                () -> twoFactorAuthService.validarPinTemporal("user@test.com", null));
    }

    @Test
    @DisplayName("Validar PIN cuando no existe código activo lanza CredencialesInvalidasException")
    void validarPinTemporal_SinCodigoActivo_LanzaExcepcion() {
        assertThrows(CredencialesInvalidasException.class,
                () -> twoFactorAuthService.validarPinTemporal("inexistente@test.com", "123456"));
    }

    @Test
    @DisplayName("Validar PIN incorrecto descuenta intentos y bloquea al tercer intento fallido")
    void validarPinTemporal_IntentosFallidosYBloqueo() {
        String correo = "intentos.user@example.com";
        String pinReal = twoFactorAuthService.generarPinTemporal(correo);
        String pinErroneo = pinReal.equals("111111") ? "222222" : "111111";

        // Intento 1 fallido (restan 2)
        CredencialesInvalidasException ex1 = assertThrows(CredencialesInvalidasException.class,
                () -> twoFactorAuthService.validarPinTemporal(correo, pinErroneo));
        assertTrue(ex1.getMessage().contains("Intentos restantes: 2"));

        // Intento 2 fallido (resta 1)
        CredencialesInvalidasException ex2 = assertThrows(CredencialesInvalidasException.class,
                () -> twoFactorAuthService.validarPinTemporal(correo, pinErroneo));
        assertTrue(ex2.getMessage().contains("Intentos restantes: 1"));

        // Intento 3 fallido (bloqueado)
        CredencialesInvalidasException ex3 = assertThrows(CredencialesInvalidasException.class,
                () -> twoFactorAuthService.validarPinTemporal(correo, pinErroneo));
        assertTrue(ex3.getMessage().contains("bloqueado"));

        // Posterior intento no encuentra PIN activo
        assertThrows(CredencialesInvalidasException.class,
                () -> twoFactorAuthService.validarPinTemporal(correo, pinReal));
    }

    @Test
    @DisplayName("Si el despachador de correo falla, no interrumpe el flujo y registra advertencia")
    void despacharCorreo_ConFalloSmtp_NoInterrumpeFlujo() {
        doThrow(new MailSendException("Error SMTP")).when(mailSender).send(any(SimpleMailMessage.class));

        String correo = "smtp.fallo@example.com";
        String pin = twoFactorAuthService.generarPinTemporal(correo);

        assertNotNull(pin);
        // El código sigue siendo válido en memoria a pesar del fallo de red
        assertTrue(twoFactorAuthService.validarPinTemporal(correo, pin));
    }

    @Test
    @DisplayName("Si mailSender es null, opera en modo degradado sin fallar")
    void despacharCorreo_SinMailSender_NoFalla() {
        twoFactorAuthService.setMailSender(null);

        String correo = "sin.mail@example.com";
        String pin = twoFactorAuthService.generarPinTemporal(correo);

        assertNotNull(pin);
        assertTrue(twoFactorAuthService.validarPinTemporal(correo, pin));
    }

    @Test
    @DisplayName("Tiempo de expiración en segundos es exactamente 300 (5 minutos)")
    void getExpiracionSegundos_Retorna300() {
        assertEquals(300L, twoFactorAuthService.getExpiracionSegundos());
    }
}
