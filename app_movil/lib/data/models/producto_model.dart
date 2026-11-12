import '../../domain/entities/producto.dart';

class ProductoModel {
  static Producto fromJson(Map<String, dynamic> json) {
    return Producto(
      idProducto: json['idProducto'] ?? 0,
      descripcion: json['descripcion'] ?? '',
      monto: (json['monto'] is num) ? (json['monto'] as num).toDouble() : 0.0,
      servicio: json['servicio'] ?? '',
    );
  }
}
