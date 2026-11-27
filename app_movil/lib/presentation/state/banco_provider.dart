import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import '../../core/network/api_client.dart';
import '../../data/models/transaccion_model.dart';
import '../../domain/entities/cuenta.dart';
import '../../domain/entities/producto.dart';
import '../../domain/entities/transaccion.dart';
import '../../domain/repositories/banco_repository.dart';

class BancoProvider extends ChangeNotifier {
  final BancoRepository bancoRepository;

  bool _isLoading = false;
  bool _isLoadingHistorial = false;
  bool _isLoadingProductos = false;
  String? _errorMessage;
  String? _productosError;
  String? _successMessage;

  List<Cuenta> _cuentas = [];
  Cuenta? _cuentaSeleccionada;
  List<Producto> _productos = [];
  List<Transaccion> _historialTransacciones = [];
  Transaccion? _ultimoComprobante;

  BancoProvider({required this.bancoRepository});

  bool get isLoading => _isLoading;
  bool get isLoadingHistorial => _isLoadingHistorial;
  bool get isLoadingProductos => _isLoadingProductos;
  String? get errorMessage => _errorMessage;
  String? get productosError => _productosError;
  String? get successMessage => _successMessage;
  List<Cuenta> get cuentas => _cuentas;
  Cuenta? get cuentaSeleccionada => _cuentaSeleccionada;
  List<Producto> get productos => _productos;
  List<Transaccion> get historialTransacciones => _historialTransacciones;
  Transaccion? get ultimoComprobante => _ultimoComprobante;

  void limpiarUltimoComprobante() {
    _ultimoComprobante = null;
    notifyListeners();
  }

  void seleccionarCuenta(Cuenta cuenta) {
    _cuentaSeleccionada = cuenta;
    notifyListeners();
    cargarHistorial(cuenta.numeroCuenta);
  }

  void limpiarDatos() {
    _cuentas = [];
    _cuentaSeleccionada = null;
    _historialTransacciones = [];
    _errorMessage = null;
    _successMessage = null;
    notifyListeners();
  }

  Future<void> cargarDatosDashboard() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final resCuentas = await bancoRepository.getCuentas();
      _cuentas = resCuentas;
      if (_cuentas.isNotEmpty) {
        if (_cuentaSeleccionada == null || !_cuentas.any((c) => c.numeroCuenta == _cuentaSeleccionada!.numeroCuenta)) {
          _cuentaSeleccionada = _cuentas.first;
        } else {
          _cuentaSeleccionada = _cuentas.firstWhere((c) => c.numeroCuenta == _cuentaSeleccionada!.numeroCuenta);
        }
      } else {
        _cuentaSeleccionada = null;
      }

      if (_productos.isEmpty) {
        await cargarProductos();
      }

      if (_cuentaSeleccionada != null) {
        await cargarHistorial(_cuentaSeleccionada!.numeroCuenta);
      }

      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _isLoading = false;
      _errorMessage = ApiClient.parseError(e);
      notifyListeners();
    }
  }

  Future<void> cargarProductos({bool forzar = false}) async {
    if (_isLoadingProductos) return;
    if (!forzar && _productos.isNotEmpty) return;

    _isLoadingProductos = true;
    _productosError = null;
    notifyListeners();

    try {
      final resProductos = await bancoRepository.getProductos();
      _productos = resProductos;
      if (_productos.isEmpty) {
        _productosError = 'No se recibieron productos del catálogo de GestoPago.';
      }
    } catch (e) {
      _productosError = ApiClient.parseError(e);
    } finally {
      _isLoadingProductos = false;
      notifyListeners();
    }
  }

  Future<void> cargarHistorial(String numCuenta) async {
    _isLoadingHistorial = true;
    notifyListeners();

    try {
      final lista = await bancoRepository.getHistorial(numCuenta);
      _historialTransacciones = lista;
      _isLoadingHistorial = false;
      notifyListeners();
    } catch (e) {
      _isLoadingHistorial = false;
      // No bloqueamos toda la pantalla por fallo de historial
      notifyListeners();
    }
  }

  Future<bool> solicitarPinTransaccional({
    required String numCuenta,
    required double monto,
    String? servicio,
  }) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      await bancoRepository.solicitarPinTransaccional(
        numCuenta: numCuenta,
        monto: monto,
        servicio: servicio,
      );
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _isLoading = false;
      _errorMessage = ApiClient.parseError(e);
      notifyListeners();
      return false;
    }
  }

  Future<bool> pagarServicio({
    required int idServicio,
    required int idProducto,
    required double monto,
    required String upe,
    required String telefono,
    String? referencia,
    required String pin,
  }) async {
    if (_cuentaSeleccionada == null) {
      _errorMessage = 'Debe seleccionar una cuenta origen para pagar.';
      notifyListeners();
      return false;
    }

    _isLoading = true;
    _errorMessage = null;
    _successMessage = null;
    _ultimoComprobante = null;
    notifyListeners();

    try {
      final res = await bancoRepository.procesarPago(
        numCuenta: _cuentaSeleccionada!.numeroCuenta,
        idServicio: idServicio,
        idProducto: idProducto,
        monto: monto,
        upe: upe,
        telefono: telefono,
        referencia: referencia,
        pin: pin,
      );

      if (res['datos'] is Map) {
        _ultimoComprobante = TransaccionModel.fromJson(
          Map<String, dynamic>.from(res['datos'] as Map),
        );
      }

      _successMessage = res['mensaje'] ?? 'Pago procesado exitosamente por GestoPago.';
      await cargarDatosDashboard(); // Refrescar saldos e historial actualizados
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      if (e is DioException && e.response?.data is Map) {
        final respMap = e.response!.data as Map;
        if (respMap['datos'] is Map) {
          final datosMap = Map<String, dynamic>.from(respMap['datos'] as Map);
          if (datosMap.containsKey('estatus') && datosMap.containsKey('upe')) {
            _ultimoComprobante = TransaccionModel.fromJson(datosMap);
            if (_cuentaSeleccionada != null) {
              await cargarHistorial(_cuentaSeleccionada!.numeroCuenta);
            }
          }
        }
      }
      _isLoading = false;
      _errorMessage = ApiClient.parseError(e);
      notifyListeners();
      return false;
    }
  }

  void clearMessages() {
    _errorMessage = null;
    _successMessage = null;
    notifyListeners();
  }
}
