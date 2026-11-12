class Cuenta {
  final int id;
  final String numeroCuenta;
  final String clabe;
  final String tipoCuenta;
  final double saldo;
  final String moneda;
  final String estatus;

  const Cuenta({
    required this.id,
    required this.numeroCuenta,
    required this.clabe,
    required this.tipoCuenta,
    required this.saldo,
    required this.moneda,
    required this.estatus,
  });
}
