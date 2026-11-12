package com.proyecto.servicios.service;

import com.proyecto.servicios.model.auth.LoginRequestDto;
import com.proyecto.servicios.model.auth.LoginResponseDto;
import com.proyecto.servicios.model.auth.SesionUsuarioDto;
import com.proyecto.servicios.model.auth.TwoFactorRespuestaDto;
import com.proyecto.servicios.model.auth.TwoFactorSolicitudDto;
import com.proyecto.servicios.model.auth.TwoFactorVerificacionDto;

public interface AuthService {

    // Inicio de sesión primario con validación de credenciales y registro de sesión única
    LoginResponseDto iniciarSesion(LoginRequestDto request);

    // Generación de PIN temporal para Autenticación Multifactor (2FA)
    TwoFactorRespuestaDto solicitarTwoFactor(TwoFactorSolicitudDto request);

    // Verificación de PIN temporal 2FA y emisión de token JWT
    LoginResponseDto verificarTwoFactor(TwoFactorVerificacionDto request);

    // Cierre de sesión e invalidación del token en el servidor
    void cerrarSesion(String username);

    // Consulta de la sesión única activa del usuario
    SesionUsuarioDto obtenerSesionActiva(String username);
}
