package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.CuentaBancariaDto;
import com.proyecto.servicios.model.cuenta.CuentaActualizacionRequestDto;
import com.proyecto.servicios.model.cuenta.CuentaCreacionRequestDto;
import com.proyecto.servicios.model.cuenta.SaldoCuentaDto;

import java.util.List;

// Interfaz de servicio para creacion, consulta y actualizacion de cuentas bancarias
public interface CuentaService {

    // Crea una cuenta bancaria asociada a un cliente activo
    CuentaBancariaDto crearCuenta(CuentaCreacionRequestDto requestDto);

    // Consulta cuenta bancaria por numero de cuenta
    CuentaBancariaDto obtenerCuentaPorNumero(String numeroCuenta);

    // Consulta cuentas bancarias asociadas a un cliente
    List<CuentaBancariaDto> obtenerCuentasPorClienteId(Long clienteId);

    // Consulta cuentas bancarias por estatus
    List<CuentaBancariaDto> obtenerCuentasPorEstatus(String estatus);

    // Consulta cuentas bancarias activas
    List<CuentaBancariaDto> obtenerCuentasActivas();

    // Consulta saldo disponible de una cuenta
    SaldoCuentaDto obtenerSaldoCuenta(String numeroCuenta);

    // Actualizacion parcial de una cuenta (saldo, estatus, tipo) sin modificar numero de cuenta
    CuentaBancariaDto actualizarCuenta(String numeroCuenta, CuentaActualizacionRequestDto requestDto);
}
