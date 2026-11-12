import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/responsive_util.dart';
import '../../domain/entities/transaccion.dart';
import '../state/banco_provider.dart';
import '../widgets/loading_spinner.dart';

class HistorialScreen extends StatefulWidget {
  const HistorialScreen({super.key});

  @override
  State<HistorialScreen> createState() => _HistorialScreenState();
}

class _HistorialScreenState extends State<HistorialScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final bancoProvider = context.read<BancoProvider>();
      if (bancoProvider.cuentaSeleccionada != null) {
        bancoProvider.cargarHistorial(bancoProvider.cuentaSeleccionada!.numeroCuenta);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final bancoProvider = context.watch<BancoProvider>();
    final cuenta = bancoProvider.cuentaSeleccionada;
    final historial = bancoProvider.historialTransacciones;
    final isLoading = bancoProvider.isLoadingHistorial;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Historial y Detalle de Cuenta'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Actualizar movimientos',
            onPressed: (cuenta != null)
                ? () => bancoProvider.cargarHistorial(cuenta.numeroCuenta)
                : null,
          ),
        ],
      ),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () async {
            if (cuenta != null) {
              await bancoProvider.cargarHistorial(cuenta.numeroCuenta);
            }
          },
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
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
                                  cuenta?.tipoCuenta ?? 'CUENTA BANCARIA',
                                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                                ),
                                Chip(
                                  label: Text(cuenta?.estatus ?? 'ACTIVA'),
                                  backgroundColor: const Color(0xFFE8F5E9),
                                  labelStyle: const TextStyle(
                                    color: AppTheme.successColor,
                                    fontWeight: FontWeight.bold,
                                    fontSize: 12,
                                  ),
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

                    // Título de la sección
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text(
                          'Transacciones y Pagos Registrados',
                          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                        if (historial.isNotEmpty)
                          Text(
                            '${historial.length} movimientos',
                            style: TextStyle(color: Colors.grey.shade600, fontSize: 13),
                          ),
                      ],
                    ),
                    const SizedBox(height: 12),

                    // Estado de Carga / Vacío / Lista de Transacciones
                    if (isLoading)
                      const Card(
                        child: Padding(
                          padding: EdgeInsets.all(32),
                          child: LoadingSpinner(mensaje: 'Consultando historial en servidor...'),
                        ),
                      )
                    else if (historial.isEmpty)
                      Card(
                        child: Padding(
                          padding: const EdgeInsets.symmetric(vertical: 40, horizontal: 20),
                          child: Column(
                            children: [
                              Icon(Icons.receipt_long_outlined, size: 54, color: Colors.grey.shade400),
                              const SizedBox(height: 14),
                              const Text(
                                'Sin movimientos registrados',
                                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                              ),
                              const SizedBox(height: 6),
                              Text(
                                'Los pagos de servicios y recargas efectuados sobre esta cuenta se reflejarán automáticamente aquí.',
                                textAlign: TextAlign.center,
                                style: TextStyle(color: Colors.grey.shade600, fontSize: 13),
                              ),
                            ],
                          ),
                        ),
                      )
                    else
                      Card(
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: SingleChildScrollView(
                            scrollDirection: Axis.horizontal,
                            child: DataTable(
                              columnSpacing: 20,
                              headingRowColor: MaterialStateProperty.all(Colors.grey.shade50),
                              columns: const [
                                DataColumn(label: Text('Fecha / Hora', style: TextStyle(fontWeight: FontWeight.bold))),
                                DataColumn(label: Text('Operación', style: TextStyle(fontWeight: FontWeight.bold))),
                                DataColumn(label: Text('Destino / Referencia', style: TextStyle(fontWeight: FontWeight.bold))),
                                DataColumn(label: Text('UPE', style: TextStyle(fontWeight: FontWeight.bold))),
                                DataColumn(label: Text('Monto', style: TextStyle(fontWeight: FontWeight.bold))),
                                DataColumn(label: Text('Estatus', style: TextStyle(fontWeight: FontWeight.bold))),
                                DataColumn(label: Text('Detalle', style: TextStyle(fontWeight: FontWeight.bold))),
                              ],
                              rows: historial.map((tx) => _buildTransaccionRow(context, tx)).toList(),
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
      ),
    );
  }

  DataRow _buildTransaccionRow(BuildContext context, Transaccion tx) {
    final bool esAplicado = tx.estatus.toUpperCase() == 'APLICADO';
    final Color colorEstatus = esAplicado ? AppTheme.successColor : Colors.amber.shade800;
    final String fechaFormateada = tx.fechaOperacion.isNotEmpty
        ? tx.fechaOperacion.replaceFirst('T', ' ').split('.').first
        : '-';

    return DataRow(
      cells: [
        DataCell(Text(fechaFormateada, style: const TextStyle(fontSize: 12))),
        DataCell(
          Row(
            children: [
              Icon(
                tx.tipoOperacion.contains('RECARGA') ? Icons.phone_android : Icons.receipt,
                size: 16,
                color: AppTheme.primaryColor,
              ),
              const SizedBox(width: 6),
              Text(tx.tipoOperacion, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 12)),
            ],
          ),
        ),
        DataCell(Text(tx.destinoPago.isNotEmpty ? tx.destinoPago : '-', style: const TextStyle(fontSize: 12))),
        DataCell(Text(tx.upe, style: const TextStyle(fontFamily: 'monospace', fontSize: 11))),
        DataCell(
          Text(
            '-\$${tx.monto.toStringAsFixed(2)}',
            style: const TextStyle(fontWeight: FontWeight.bold, color: AppTheme.errorColor, fontSize: 13),
          ),
        ),
        DataCell(
          Chip(
            label: Text(tx.estatus),
            backgroundColor: colorEstatus.withOpacity(0.12),
            labelStyle: TextStyle(color: colorEstatus, fontWeight: FontWeight.bold, fontSize: 11),
            padding: EdgeInsets.zero,
          ),
        ),
        DataCell(
          IconButton(
            icon: const Icon(Icons.info_outline, size: 20, color: AppTheme.primaryColor),
            tooltip: 'Ver comprobante',
            onPressed: () => _mostrarComprobanteDialog(context, tx),
          ),
        ),
      ],
    );
  }

  void _mostrarComprobanteDialog(BuildContext context, Transaccion tx) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Row(
          children: const [
            Icon(Icons.verified, color: AppTheme.primaryColor),
            SizedBox(width: 8),
            Text('Comprobante de Pago'),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildInfoItem('Folio Transacción:', tx.folioTransaccion),
            _buildInfoItem('Identificador UPE:', tx.upe),
            _buildInfoItem('Cuenta Débito:', tx.numCuenta),
            _buildInfoItem('Tipo de Pago:', tx.tipoOperacion),
            _buildInfoItem('Destino / Referencia:', tx.destinoPago),
            _buildInfoItem('Importe Aplicado:', '\$${tx.monto.toStringAsFixed(2)} MXN'),
            _buildInfoItem('Fecha y Hora:', tx.fechaOperacion),
            _buildInfoItem('Estatus:', tx.estatus),
            if (tx.mensaje.isNotEmpty) _buildInfoItem('Mensaje:', tx.mensaje),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Cerrar'),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoItem(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 140,
            child: Text(
              label,
              style: const TextStyle(fontWeight: FontWeight.w600, color: Colors.grey, fontSize: 12),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(fontWeight: FontWeight.w500, fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }
}
