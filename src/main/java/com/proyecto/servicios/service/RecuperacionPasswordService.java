package com.proyecto.servicios.service;

import com.proyecto.servicios.model.auth.RecuperacionRespuestaDto;
import com.proyecto.servicios.model.auth.RecuperacionRestablecerDto;
import com.proyecto.servicios.model.auth.RecuperacionSolicitudDto;
import com.proyecto.servicios.model.auth.RecuperacionVerificacionDto;

public interface RecuperacionPasswordService {

    RecuperacionRespuestaDto solicitarRecuperacion(RecuperacionSolicitudDto request);

    RecuperacionRespuestaDto verificarCodigo(RecuperacionVerificacionDto request);

    void restablecerPassword(RecuperacionRestablecerDto request);
}
