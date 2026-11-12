import '../../domain/entities/transaccion.dart';

class TransaccionModel {
  static Transaccion fromJson(Map<String, dynamic> json) {
    return Transaccion(
      folioTransaccion: json['folioTransaccion']?.toString() ?? '',
      numCuenta: json['numCuenta']?.toString() ?? '',
      monto: (json['monto'] is num) ? (json['monto'] as num).toDouble() : 0.0,
      upe: json['upe']?.toString() ?? '',
      tipoOperacion: json['tipoOperacion']?.toString() ?? 'PAGO_SERVICIO',
      destinoPago: json['destinoPago']?.toString() ?? '-',
      estatus: json['estatus']?.toString() ?? 'APLICADO',
      mensaje: json['mensaje']?.toString() ?? '',
      fechaOperacion: json['fechaOperacion']?.toString() ?? '',
    );
  }
}
