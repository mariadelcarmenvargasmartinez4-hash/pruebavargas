import '../../domain/entities/cuenta.dart';
import '../../domain/entities/producto.dart';
import '../../domain/entities/transaccion.dart';
import '../../domain/repositories/banco_repository.dart';
import '../datasources/banco_remote_datasource.dart';
import '../models/cuenta_model.dart';
import '../models/producto_model.dart';
import '../models/transaccion_model.dart';

class BancoRepositoryImpl implements BancoRepository {
  final BancoRemoteDataSource remoteDataSource;

  BancoRepositoryImpl({required this.remoteDataSource});

  @override
  Future<List<Cuenta>> getCuentas() async {
    final list = await remoteDataSource.getCuentas();
    return list.map((item) => CuentaModel.fromJson(item as Map<String, dynamic>)).toList();
  }

  @override
  Future<Cuenta> getCuentaPorNumero(String numeroCuenta) async {
    final data = await remoteDataSource.getCuentaPorNumero(numeroCuenta);
    return CuentaModel.fromJson(data);
  }

  @override
  Future<List<Producto>> getProductos() async {
    final list = await remoteDataSource.getProductos();
    return list.map((item) => ProductoModel.fromJson(item as Map<String, dynamic>)).toList();
  }

  @override
  Future<Map<String, dynamic>> procesarPago({
    required String numCuenta,
    required int idServicio,
    required int idProducto,
    required double monto,
    required String upe,
    required String telefono,
    required String pin,
  }) async {
    final payload = {
      'numCuenta': numCuenta,
      'idServicio': idServicio,
      'idProducto': idProducto,
      'monto': monto,
      'upe': upe,
      'telefono': telefono,
      'pin': pin,
      'bandera': true,
    };
    return await remoteDataSource.procesarPago(payload);
  }

  @override
  Future<List<Transaccion>> getHistorial(String numCuenta) async {
    final list = await remoteDataSource.getHistorial(numCuenta);
    return list.map((item) => TransaccionModel.fromJson(item as Map<String, dynamic>)).toList();
  }
}
