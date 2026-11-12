import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/responsive_util.dart';
import '../../domain/entities/producto.dart';
import '../state/banco_provider.dart';
import '../widgets/confirmation_dialog.dart';
import '../widgets/custom_button.dart';
import '../widgets/custom_text_field.dart';

class PagosScreen extends StatefulWidget {
  const PagosScreen({super.key});

  @override
  State<PagosScreen> createState() => _PagosScreenState();
}

class _PagosScreenState extends State<PagosScreen> {
  final _referenciaController = TextEditingController();
  final _telefonoController = TextEditingController(text: '5512345678');
  final _montoController = TextEditingController(text: '150.00');
  final _formKey = GlobalKey<FormState>();

  Producto? _productoSeleccionado;
  Map<String, dynamic>? _comprobante;

  @override
  void dispose() {
    _referenciaController.dispose();
    _telefonoController.dispose();
    _montoController.dispose();
    super.dispose();
  }

  void _abrirModalConfirmacion() {
    if (!_formKey.currentState!.validate()) return;

    final bancoProvider = context.read<BancoProvider>();
    final cuenta = bancoProvider.cuentaSeleccionada;

    if (cuenta == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Por favor seleccione una cuenta bancaria origen.'),
          backgroundColor: AppTheme.errorColor,
        ),
      );
      return;
    }

    final monto = double.tryParse(_montoController.text.trim()) ?? 0.0;
    final servicioNombre = _productoSeleccionado?.descripcion ?? 'Pago de Servicio GestoPago';

    showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => ConfirmationDialog(
        title: 'Confirmar Transacción Bancaria',
        servicioNombre: servicioNombre,
        numeroCuenta: cuenta.numeroCuenta,
        monto: monto,
        referencia: _referenciaController.text.trim().isNotEmpty
            ? _referenciaController.text.trim()
            : _telefonoController.text.trim(),
        onConfirm: (pin) async {
          final exito = await bancoProvider.pagarServicio(
            idServicio: 1,
            idProducto: _productoSeleccionado?.idProducto ?? 101,
            monto: monto,
            upe: _referenciaController.text.trim().isNotEmpty
                ? _referenciaController.text.trim()
                : '1234567890',
            telefono: _telefonoController.text.trim(),
            pin: pin,
          );

          if (!exito) {
            throw Exception(bancoProvider.errorMessage ?? 'Error al procesar el pago.');
          }

          setState(() {
            _comprobante = {
              'servicio': servicioNombre,
              'cuenta': cuenta.numeroCuenta,
              'monto': monto,
              'fecha': DateTime.now().toString().substring(0, 19),
              'autorizacion': 'GP-${DateTime.now().millisecondsSinceEpoch.toString().substring(5)}',
            };
          });
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final bancoProvider = context.watch<BancoProvider>();
    final cuenta = bancoProvider.cuentaSeleccionada;

    return Scaffold(
      appBar: AppBar(title: const Text('Pago de Servicios y Compras')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: ResponsiveUtil.pagePadding(context),
          child: Center(
            child: ConstrainedBox(
              constraints: BoxConstraints(maxWidth: ResponsiveUtil.contentWidth(context)),
              child: _comprobante != null
                  ? _buildComprobanteExito()
                  : Card(
                      child: Padding(
                        padding: const EdgeInsets.all(24),
                        child: Form(
                          key: _formKey,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              const Text(
                                'Formulario de Pago GestoPago',
                                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                              ),
                              const SizedBox(height: 6),
                              const Text(
                                'Seleccione el servicio o producto e ingrese los datos de referencia',
                                style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                              ),
                              const Divider(height: 28),

                              // Cuenta Origen
                              Container(
                                padding: const EdgeInsets.all(12),
                                decoration: BoxDecoration(
                                  color: Colors.grey.shade50,
                                  borderRadius: BorderRadius.circular(10),
                                  border: Border.all(color: Colors.grey.shade200),
                                ),
                                child: Row(
                                  children: [
                                    const Icon(Icons.account_balance_wallet_outlined, color: AppTheme.primaryColor),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          const Text('Cuenta a debitar:', style: TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
                                          Text(
                                            cuenta != null
                                                ? '${cuenta.tipoCuenta} - ${cuenta.numeroCuenta} (Saldo: \$${cuenta.saldo.toStringAsFixed(2)})'
                                                : 'No hay cuenta seleccionada',
                                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(height: 18),

                              // Selector de Catálogo de Productos/Servicios
                              DropdownButtonFormField<Producto>(
                                decoration: const InputDecoration(
                                  labelText: 'Servicio / Producto',
                                  prefixIcon: Icon(Icons.category_outlined),
                                ),
                                value: _productoSeleccionado,
                                hint: const Text('Seleccione un servicio del catálogo'),
                                items: bancoProvider.productos.map((prod) {
                                  return DropdownMenuItem<Producto>(
                                    value: prod,
                                    child: Text('${prod.descripcion} (\$${prod.monto.toStringAsFixed(2)})'),
                                  );
                                }).toList(),
                                onChanged: (val) {
                                  setState(() {
                                    _productoSeleccionado = val;
                                    if (val != null && val.monto > 0) {
                                      _montoController.text = val.monto.toStringAsFixed(2);
                                    }
                                  });
                                },
                              ),
                              const SizedBox(height: 18),

                              // Referencia / UPE
                              CustomTextField(
                                controller: _referenciaController,
                                label: 'Referencia / Contrato / UPE',
                                hint: 'Número de servicio o contrato',
                                prefixIcon: Icons.tag,
                                keyboardType: TextInputType.text,
                              ),
                              const SizedBox(height: 18),

                              // Teléfono
                              CustomTextField(
                                controller: _telefonoController,
                                label: 'Teléfono Móvil (10 dígitos)',
                                prefixIcon: Icons.phone_android_outlined,
                                keyboardType: TextInputType.phone,
                                validator: (val) {
                                  if (val == null || val.trim().isEmpty) return 'El teléfono es requerido';
                                  if (!RegExp(r'^\d{10}$').hasMatch(val.trim())) return 'Debe contener exactamente 10 dígitos';
                                  return null;
                                },
                              ),
                              const SizedBox(height: 18),

                              // Monto a pagar
                              CustomTextField(
                                controller: _montoController,
                                label: 'Monto a Pagar (MXN)',
                                prefixIcon: Icons.attach_money,
                                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                                validator: (val) {
                                  if (val == null || val.trim().isEmpty) return 'El monto es obligatorio';
                                  final num = double.tryParse(val.trim());
                                  if (num == null || num <= 0) return 'El monto debe ser mayor a 0';
                                  return null;
                                },
                              ),
                              const SizedBox(height: 28),

                              CustomButton(
                                text: 'Continuar con el Pago',
                                icon: Icons.arrow_forward_rounded,
                                isLoading: bancoProvider.isLoading,
                                onPressed: _abrirModalConfirmacion,
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildComprobanteExito() {
    return Card(
      elevation: 3,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const CircleAvatar(
              radius: 36,
              backgroundColor: Color(0xFFE8F5E9),
              child: Icon(Icons.check_circle_rounded, color: AppTheme.successColor, size: 48),
            ),
            const SizedBox(height: 16),
            const Text(
              '¡Pago Exitoso!',
              style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: AppTheme.successColor),
            ),
            const SizedBox(height: 4),
            const Text('Transacción autorizada y aplicada en GestoPago', style: TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
            const Divider(height: 32),
            _buildReciboRow('Servicio:', _comprobante!['servicio']),
            _buildReciboRow('Cuenta Debitada:', _comprobante!['cuenta']),
            _buildReciboRow('Importe:', '\$${(_comprobante!['monto'] as double).toStringAsFixed(2)} MXN'),
            _buildReciboRow('Folio Autorización:', _comprobante!['autorizacion']),
            _buildReciboRow('Fecha / Hora:', _comprobante!['fecha']),
            const SizedBox(height: 28),
            CustomButton(
              text: 'Realizar Otro Pago',
              icon: Icons.refresh,
              onPressed: () {
                setState(() {
                  _comprobante = null;
                  _referenciaController.clear();
                });
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildReciboRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
          Text(value, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
        ],
      ),
    );
  }
}
