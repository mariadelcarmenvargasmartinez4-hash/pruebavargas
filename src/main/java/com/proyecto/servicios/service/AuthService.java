package com.proyecto.servicios.service;

import com.proyecto.servicios.model.auth.LoginRequestDto;
import com.proyecto.servicios.model.auth.LoginResponseDto;

public interface AuthService {
    LoginResponseDto iniciarSesion(LoginRequestDto request);
}