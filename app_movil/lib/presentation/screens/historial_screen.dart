import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/responsive_util.dart';
import '../state/banco_provider.dart';

class HistorialScreen extends StatelessWidget {
  const HistorialScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final bancoProvider = context.watch<BancoProvider>();
    final cuenta = bancoProvider.cuentaSeleccionada;

    return Scaffold(
      appBar: AppBar(title: const Text('Historial y Detalle de Cuenta')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: ResponsiveUtil.pagePadding(context),
          child: Center(
            child: ConstrainedBox(
              constraints: BoxConstraints(maxWidth: ResponsiveUtil.contentWidth(context)),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Resumen de la Cuenta Seleccionada
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(20),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                cuenta?.tipoCuenta ?? 'CUENTA',
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                              ),
                              Chip(
                                label: Text(cuenta?.estatus ?? 'ACTIVA'),
                                backgroundColor: const Color(0xFFE8F5E9),
                                labelStyle: const TextStyle(color: AppTheme.successColor, fontWeight: FontWeight.bold, fontSize: 12),
                                padding: EdgeInsets.zero,
                              ),
                            ],
                          ),
                          const SizedBox(height: 10),
                          Text(
                            '\$${cuenta?.saldo.toStringAsFixed(2) ?? "0.00"} ${cuenta?.moneda ?? "MXN"}',
                            style: const TextStyle(
                              fontSize: 28,
                              fontWeight: FontWeight.bold,
                              color: AppTheme.primaryColor,
                            ),
                          ),
                          const SizedBox(height: 14),
                          const Divider(),
                          _buildInfoItem('Número de Cuenta:', cuenta?.numeroCuenta ?? '-'),
                          _buildInfoItem('CLABE Interbancaria:', cuenta?.clabe ?? '-'),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),

                  // Tabla Responsiva de Movimientos con Scroll Horizontal y Vertical
                  const Text(
                    'Últimos Movimientos Registrados',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 12),

                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: SingleChildScrollView(
                        scrollDirection: Axis.horizontal,
                        child: DataTable(
                          columnSpacing: 24,
                          headingRowColor: MaterialStateProperty.all(Colors.grey.shade50),
                          columns: const [
                            DataColumn(label: Text('Fecha', style: TextStyle(fontWeight: FontWeight.bold))),
                            DataColumn(label: Text('Concepto / Servicio', style: TextStyle(fontWeight: FontWeight.bold))),
                            DataColumn(label: Text('Tipo', style: TextStyle(fontWeight: FontWeight.bold))),
                            DataColumn(label: Text('Importe', style: TextStyle(fontWeight: FontWeight.bold))),
                            DataColumn(label: Text('Estatus', style: TextStyle(fontWeight: FontWeight.bold))),
                          ],
                          rows: [
                            _buildRow('Hoy 11:30 AM', 'Pago de Servicio CFE GestoPago', 'CARGO', '-\$150.00', 'APLICADO', AppTheme.errorColor),
                            _buildRow('Ayer 04:15 PM', 'Recarga Telefónica Telcel', 'CARGO', '-\$50.00', 'APLICADO', AppTheme.errorColor),
                            _buildRow('10 Nov 2026', 'Apertura de Cuenta Bancaria', 'ABONO', '+\$2,000.00', 'EXITOSO', AppTheme.successColor),
                          ],
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildInfoItem(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
          Text(value, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
        ],
      ),
    );
  }

  DataRow _buildRow(String fecha, String concepto, String tipo, String importe, String estatus, Color importeColor) {
    return DataRow(
      cells: [
        DataCell(Text(fecha, style: const TextStyle(fontSize: 12))),
        DataCell(Text(concepto, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500))),
        DataCell(Text(tipo, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary))),
        DataCell(Text(importe, style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: importeColor))),
        DataCell(
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
            decoration: BoxDecoration(
              color: AppTheme.successColor.withOpacity(0.1),
              borderRadius: BorderRadius.circular(6),
            ),
            child: Text(estatus, style: const TextStyle(fontSize: 11, color: AppTheme.successColor, fontWeight: FontWeight.bold)),
          ),
        ),
      ],
    );
  }
}
