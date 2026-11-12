import 'package:flutter/material.dart';
import '../../core/network/api_client.dart';
import '../../domain/entities/cuenta.dart';
import '../../domain/entities/producto.dart';
import '../../domain/repositories/banco_repository.dart';

class BancoProvider extends ChangeNotifier {
  final BancoRepository bancoRepository;

  bool _isLoading = false;
  String? _errorMessage;
  String? _successMessage;

  List<Cuenta> _cuentas = [];
  Cuenta? _cuentaSeleccionada;
  List<Producto> _productos = [];

  BancoProvider({required this.bancoRepository});

  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  String? get successMessage => _successMessage;
  List<Cuenta> get cuentas => _cuentas;
  Cuenta? get cuentaSeleccionada => _cuentaSeleccionada;
  List<Producto> get productos => _productos;

  void seleccionarCuenta(Cuenta cuenta) {
    _cuentaSeleccionada = cuenta;
    notifyListeners();
  }

  Future<void> cargarDatosDashboard() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final resCuentas = await bancoRepository.getCuentas();
      _cuentas = resCuentas;
      if (_cuentas.isNotEmpty && _cuentaSeleccionada == null) {
        _cuentaSeleccionada = _cuentas.first;
      }

      final resProductos = await bancoRepository.getProductos();
      _productos = resProductos;

      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _isLoading = false;
      _errorMessage = ApiClient.parseError(e);
      notifyListeners();
    }
  }

  Future<bool> pagarServicio({
    required int idServicio,
    required int idProducto,
    required double monto,
    required String upe,
    required String telefono,
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
    notifyListeners();

    try {
      final res = await bancoRepository.procesarPago(
        numCuenta: _cuentaSeleccionada!.numeroCuenta,
        idServicio: idServicio,
        idProducto: idProducto,
        monto: monto,
        upe: upe,
        telefono: telefono,
        pin: pin,
      );

      _successMessage = res['mensaje'] ?? 'Pago procesado exitosamente por GestoPago.';
      await cargarDatosDashboard(); // Refrescar saldos actualizados
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

  void clearMessages() {
    _errorMessage = null;
    _successMessage = null;
    notifyListeners();
  }
}
