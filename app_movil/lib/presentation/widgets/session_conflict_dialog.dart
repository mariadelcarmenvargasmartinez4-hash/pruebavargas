import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import 'custom_button.dart';

class SessionConflictDialog extends StatelessWidget {
  final String message;
  final VoidCallback onAccepted;

  const SessionConflictDialog({
    super.key,
    required this.message,
    required this.onAccepted,
  });

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      title: const Row(
        children: [
          Icon(Icons.warning_amber_rounded, color: AppTheme.errorColor, size: 28),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'Sesión Invalida',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
            ),
          ),
        ],
      ),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            message,
            style: const TextStyle(fontSize: 14, height: 1.4),
          ),
          const SizedBox(height: 12),
          const Text(
            'Por políticas de seguridad bancaria, únicamente se permite una sesión activa simultánea por usuario.',
            style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
          ),
        ],
      ),
      actions: [
        CustomButton(
          text: 'Entendido, volver a Iniciar Sesión',
          onPressed: onAccepted,
        ),
      ],
    );
  }
}
