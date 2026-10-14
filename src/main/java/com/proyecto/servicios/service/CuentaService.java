package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.CuentaBancariaDto;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.model.cuenta.CuentaActualizacionRequestDto;
import com.proyecto.servicios.model.cuenta.CuentaConsultaFiltroRecord;
import com.proyecto.servicios.model.cuenta.CuentaCreacionRequestDto;
import com.proyecto.servicios.model.cuenta.SaldoCuentaDto;

/**
 * Interfaz limpia y desacoplada para gestión de cuentas bancarias.
 * Sin sobrecargas ni consultas redundantes: unifica la búsqueda mediante paginación y records.
 */
public interface CuentaService {

    // Crea una cuenta bancaria asociada a un cliente activo
    CuentaBancariaDto crearCuenta(CuentaCreacionRequestDto requestDto);

    // Consulta unificada y paginada con filtros opcionales dinámicos mediante Record inmutable
    PaginaResponseRecord<CuentaBancariaDto> buscarCuentasPaginadas(CuentaConsultaFiltroRecord filtro);

    // Consulta cuenta bancaria por número de cuenta
    CuentaBancariaDto obtenerCuentaPorNumero(String numeroCuenta);

    // Consulta saldo disponible de una cuenta
    SaldoCuentaDto obtenerSaldoCuenta(String numeroCuenta);

    // Actualización de cuenta bancaria respetando reglas de baja lógica y cliente activo
    CuentaBancariaDto actualizarCuenta(String numeroCuenta, CuentaActualizacionRequestDto requestDto);
}
