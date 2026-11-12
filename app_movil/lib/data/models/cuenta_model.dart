import '../../domain/entities/cuenta.dart';

class CuentaModel {
  static Cuenta fromJson(Map<String, dynamic> json) {
    return Cuenta(
      id: json['id'] ?? 0,
      numeroCuenta: json['numeroCuenta'] ?? '',
      clabe: json['clabe'] ?? '',
      tipoCuenta: json['tipoCuenta'] ?? 'DEBITO',
      saldo: (json['saldo'] is num) ? (json['saldo'] as num).toDouble() : 0.0,
      moneda: json['moneda'] ?? 'MXN',
      estatus: json['estatus'] ?? 'ACTIVA',
    );
  }
}
