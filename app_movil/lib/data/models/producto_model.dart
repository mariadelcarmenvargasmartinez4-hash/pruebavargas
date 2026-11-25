import '../../domain/entities/producto.dart';

class ProductoModel {
  static Producto fromJson(Map<String, dynamic> json) {
    double parsedMonto = 0.0;
    if (json['monto'] is num) {
      parsedMonto = (json['monto'] as num).toDouble();
    } else if (json['precio'] != null) {
      parsedMonto = double.tryParse(json['precio'].toString()) ?? 0.0;
    }

    final nombreProducto = (json['producto'] != null && json['producto'].toString().trim().isNotEmpty)
        ? json['producto'].toString()
        : (json['descripcion'] ?? json['servicio'] ?? '');

    return Producto(
      idProducto: json['idProducto'] ?? 0,
      idServicio: json['idServicio'] ?? 1,
      descripcion: nombreProducto,
      monto: parsedMonto,
      servicio: json['servicio'] ?? '',
    );
  }
}
