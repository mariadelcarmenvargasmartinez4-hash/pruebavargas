import '../entities/cuenta.dart';
import '../entities/producto.dart';
import '../entities/transaccion.dart';

abstract class BancoRepository {
  Future<List<Cuenta>> getCuentas();
  Future<Cuenta> getCuentaPorNumero(String numeroCuenta);
  Future<List<Producto>> getProductos();
  Future<Map<String, dynamic>> procesarPago({
    required String numCuenta,
    required int idServicio,
    required int idProducto,
    required double monto,
    required String upe,
    required String telefono,
    required String pin,
  });
  Future<List<Transaccion>> getHistorial(String numCuenta);
}
