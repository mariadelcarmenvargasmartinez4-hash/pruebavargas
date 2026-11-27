import '../../domain/entities/transaccion.dart';

class TransaccionModel {
  static Transaccion fromJson(Map<String, dynamic> json) {
    int? parseInt(dynamic v) {
      if (v is int) return v;
      if (v is num) return v.toInt();
      if (v != null) return int.tryParse(v.toString());
      return null;
    }

    String? parseOptionalString(dynamic v) {
      if (v == null) return null;
      final s = v.toString().trim();
      return s.isEmpty ? null : s;
    }

    return Transaccion(
      id: parseInt(json['id']),
      folioTransaccion: json['folioTransaccion']?.toString() ?? '',
      numCuenta: json['numCuenta']?.toString() ?? '',
      idServicio: parseInt(json['idServicio']),
      idProducto: parseInt(json['idProducto']),
      monto: (json['monto'] is num)
          ? (json['monto'] as num).toDouble()
          : (double.tryParse(json['monto']?.toString() ?? '') ?? 0.0),
      moneda: parseOptionalString(json['moneda']) ?? 'MXN',
      upe: json['upe']?.toString() ?? '',
      tipoOperacion: json['tipoOperacion']?.toString() ?? 'PAGO_SERVICIO',
      destinoPago: json['destinoPago']?.toString() ?? '-',
      estatus: json['estatus']?.toString() ?? 'APLICADO',
      mensaje: json['mensaje']?.toString() ?? '',
      nombreProducto: parseOptionalString(json['nombreProducto']),
      nombreServicio: parseOptionalString(json['nombreServicio']),
      numAutorizacion: parseOptionalString(json['numAutorizacion']),
      idTxProveedor: parseOptionalString(json['idTxProveedor']),
      atencionClientes: parseOptionalString(json['atencionClientes']) ?? '800-437-8672',
      nombreGateway: parseOptionalString(json['nombreGateway']) ?? 'GestoPago Gateway 2.0',
      confirmadoSinCargo: json['confirmadoSinCargo'] is bool ? json['confirmadoSinCargo'] as bool : null,
      fechaOperacion: json['fechaOperacion']?.toString() ?? '',
    );
  }
}
