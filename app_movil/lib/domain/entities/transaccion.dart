class Transaccion {
  final String folioTransaccion;
  final String numCuenta;
  final double monto;
  final String upe;
  final String tipoOperacion;
  final String destinoPago;
  final String estatus;
  final String mensaje;
  final String fechaOperacion;

  Transaccion({
    required this.folioTransaccion,
    required this.numCuenta,
    required this.monto,
    required this.upe,
    required this.tipoOperacion,
    required this.destinoPago,
    required this.estatus,
    required this.mensaje,
    required this.fechaOperacion,
  });
}
