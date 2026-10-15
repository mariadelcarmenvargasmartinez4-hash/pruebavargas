package com.proyecto.servicio.empresa.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Validador de campos bajo el paquete 'com.proyecto.servicio.empresa.controller'
 * para compatibilidad con la estructura del grupo del proyecto.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ValidadorCampos extends com.proyecto.servicios.controller.ValidadorCampos {
}
