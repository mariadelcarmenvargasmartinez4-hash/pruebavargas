class Producto {
  final int idProducto;
  final int idServicio;
  final String descripcion;
  final double monto;
  final String servicio;

  const Producto({
    required this.idProducto,
    this.idServicio = 1,
    required this.descripcion,
    required this.monto,
    required this.servicio,
  });
}
