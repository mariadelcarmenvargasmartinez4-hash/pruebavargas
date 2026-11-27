class Transaccion {
  final int? id;
  final String folioTransaccion;
  final String numCuenta;
  final int? idServicio;
  final int? idProducto;
  final double monto;
  final String moneda;
  final String upe;
  final String tipoOperacion;
  final String destinoPago;
  final String estatus;
  final String mensaje;
  final String? nombreProducto;
  final String? nombreServicio;
  final String? numAutorizacion;
  final String? idTxProveedor;
  final String atencionClientes;
  final String nombreGateway;
  final bool? confirmadoSinCargo;
  final String fechaOperacion;

  Transaccion({
    this.id,
    required this.folioTransaccion,
    required this.numCuenta,
    this.idServicio,
    this.idProducto,
    required this.monto,
    this.moneda = 'MXN',
    required this.upe,
    required this.tipoOperacion,
    required this.destinoPago,
    required this.estatus,
    required this.mensaje,
    this.nombreProducto,
    this.nombreServicio,
    this.numAutorizacion,
    this.idTxProveedor,
    this.atencionClientes = '800-437-8672',
    this.nombreGateway = 'GestoPago Gateway 2.0',
    this.confirmadoSinCargo,
    required this.fechaOperacion,
  });
}
