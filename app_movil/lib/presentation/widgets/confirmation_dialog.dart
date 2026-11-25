import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../core/theme/app_theme.dart';
import 'custom_button.dart';
import 'custom_text_field.dart';

class ConfirmationDialog extends StatefulWidget {
  final String title;
  final String servicioNombre;
  final String numeroCuenta;
  final double monto;
  final String referencia;
  final String? upe;
  final Future<void> Function(String pin) onConfirm;
  final Future<void> Function()? onResendPin;

  const ConfirmationDialog({
    super.key,
    required this.title,
    required this.servicioNombre,
    required this.numeroCuenta,
    required this.monto,
    required this.referencia,
    this.upe,
    required this.onConfirm,
    this.onResendPin,
  });

  @override
  State<ConfirmationDialog> createState() => _ConfirmationDialogState();
}

class _ConfirmationDialogState extends State<ConfirmationDialog> {
  final _pinController = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  bool _isLoading = false;
  bool _isResending = false;
  bool _obscurePin = false;
  String? _errorMessage;
  String? _resendStatusMessage;

  @override
  void dispose() {
    _pinController.dispose();
    super.dispose();
  }

  Future<void> _handleConfirm() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      await widget.onConfirm(_pinController.text.trim().toUpperCase());
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _errorMessage = e.toString().replaceFirst('Exception: ', '');
        });
      }
    }
  }

  Future<void> _handleResend() async {
    if (widget.onResendPin == null) return;
    setState(() {
      _isResending = true;
      _resendStatusMessage = null;
    });
    try {
      await widget.onResendPin!();
      if (mounted) {
        setState(() {
          _isResending = false;
          _resendStatusMessage = '¡PIN reenviado a tu correo!';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isResending = false;
          _resendStatusMessage = 'Error al reenviar PIN';
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      title: Row(
        children: [
          const Icon(Icons.verified_user_outlined, color: AppTheme.primaryColor),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              widget.title,
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
          ),
        ],
      ),
      content: SingleChildScrollView(
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppTheme.primaryColor.withOpacity(0.08),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: AppTheme.primaryColor.withOpacity(0.3)),
                ),
                child: const Row(
                  children: [
                    Icon(Icons.mark_email_read_outlined, color: AppTheme.primaryColor, size: 24),
                    SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        'Hemos enviado un PIN dinámico a tu correo electrónico para autorizar este pago de forma segura.',
                        style: TextStyle(fontSize: 12, color: AppTheme.primaryColor, fontWeight: FontWeight.w500),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              _buildDetailRow('Servicio:', widget.servicioNombre),
              _buildDetailRow('Cuenta Origen:', widget.numeroCuenta),
              _buildDetailRow('Referencia/Teléfono:', widget.referencia),
              if (widget.upe != null) _buildDetailRow('UPE (ID de operación):', widget.upe!),
              const Divider(height: 24),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text('Total a Pagar:', style: TextStyle(fontWeight: FontWeight.w600)),
                  Text(
                    '\$${widget.monto.toStringAsFixed(2)} MXN',
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: AppTheme.primaryColor,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              CustomTextField(
                controller: _pinController,
                label: 'PIN Dinámico de Autorización',
                hint: '10 caracteres (letras y números)',
                prefixIcon: Icons.lock_clock_outlined,
                obscureText: _obscurePin,
                keyboardType: TextInputType.visiblePassword,
                textCapitalization: TextCapitalization.characters,
                inputFormatters: [
                  FilteringTextInputFormatter.allow(RegExp(r'[a-zA-Z0-9]')),
                  LengthLimitingTextInputFormatter(10),
                ],
                suffixIcon: IconButton(
                  icon: Icon(_obscurePin ? Icons.visibility_outlined : Icons.visibility_off_outlined, size: 20),
                  onPressed: () => setState(() => _obscurePin = !_obscurePin),
                ),
                validator: (val) {
                  if (val == null || val.trim().isEmpty) return 'El PIN es obligatorio';
                  if (!RegExp(r'^(?=.*[A-Za-z])(?=.*\d)[A-Za-z0-9]{10}$').hasMatch(val.trim())) {
                    return 'Debe tener 10 caracteres con letras y números';
                  }
                  return null;
                },
              ),
              if (widget.onResendPin != null) ...[
                const SizedBox(height: 6),
                Align(
                  alignment: Alignment.centerRight,
                  child: TextButton.icon(
                    onPressed: _isResending ? null : _handleResend,
                    icon: _isResending
                        ? const SizedBox(width: 12, height: 12, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.refresh, size: 14),
                    label: Text(
                      _resendStatusMessage ?? 'Reenviar PIN al correo',
                      style: TextStyle(
                        fontSize: 12,
                        color: _resendStatusMessage != null ? AppTheme.accentColor : AppTheme.primaryColor,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ),
              ],
              if (_errorMessage != null) ...[
                const SizedBox(height: 12),
                Text(
                  _errorMessage!,
                  style: const TextStyle(color: AppTheme.errorColor, fontSize: 13),
                ),
              ],
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _isLoading ? null : () => Navigator.of(context).pop(false),
          child: const Text('Cancelar', style: TextStyle(color: AppTheme.textSecondary)),
        ),
        SizedBox(
          width: 140,
          child: CustomButton(
            text: 'Autorizar',
            isLoading: _isLoading,
            onPressed: _handleConfirm,
          ),
        ),
      ],
    );
  }

  Widget _buildDetailRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 130,
            child: Text(label, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
            ),
          ),
        ],
      ),
    );
  }
}
