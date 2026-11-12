import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/responsive_util.dart';
import '../state/auth_provider.dart';
import '../widgets/custom_button.dart';
import '../widgets/custom_text_field.dart';
import 'dashboard_screen.dart';
import 'login_screen.dart';

class TwoFactorScreen extends StatefulWidget {
  const TwoFactorScreen({super.key});

  @override
  State<TwoFactorScreen> createState() => _TwoFactorScreenState();
}

class _TwoFactorScreenState extends State<TwoFactorScreen> {
  final _pinController = TextEditingController();
  final _formKey = GlobalKey<FormState>();

  int _segundosRestantes = 300; // 5 minutos
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _iniciarConteo();
  }

  void _iniciarConteo() {
    _timer?.cancel();
    _segundosRestantes = 300;
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_segundosRestantes > 0) {
        setState(() => _segundosRestantes--);
      } else {
        timer.cancel();
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    _pinController.dispose();
    super.dispose();
  }

  String get _tiempoFormateado {
    final minutos = (_segundosRestantes ~/ 60).toString().padLeft(2, '0');
    final segundos = (_segundosRestantes % 60).toString().padLeft(2, '0');
    return '$minutos:$segundos';
  }

  Future<void> _verificarCodigo() async {
    if (!_formKey.currentState!.validate()) return;

    final authProvider = context.read<AuthProvider>();
    final success = await authProvider.verificar2Fa(_pinController.text.trim());

    if (!mounted) return;

    if (success) {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => const DashboardScreen()),
      );
    } else {
      if (authProvider.errorMessage != null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(authProvider.errorMessage!),
            backgroundColor: AppTheme.errorColor,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    }
  }

  Future<void> _reenviarCodigo() async {
    final authProvider = context.read<AuthProvider>();
    final success = await authProvider.solicitar2Fa();
    if (success) {
      _iniciarConteo();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Nuevo PIN temporal 2FA generado y enviado.'),
          backgroundColor: AppTheme.successColor,
          behavior: SnackBarBehavior.floating,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Autenticación Multifactor'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            Navigator.of(context).pushReplacement(
              MaterialPageRoute(builder: (_) => const LoginScreen()),
            );
          },
        ),
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: ResponsiveUtil.pagePadding(context),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 440),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 32),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        const Icon(
                          Icons.phonelink_lock_rounded,
                          size: 56,
                          color: AppTheme.primaryColor,
                        ),
                        const SizedBox(height: 16),
                        const Text(
                          'Verificación 2FA',
                          textAlign: TextAlign.center,
                          style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Ingrese el PIN temporal de 6 dígitos enviado para ${authProvider.pendingUsername}',
                          textAlign: TextAlign.center,
                          style: const TextStyle(fontSize: 13, color: AppTheme.textSecondary),
                        ),
                        const SizedBox(height: 24),

                        // Campo PIN de 6 dígitos
                        CustomTextField(
                          controller: _pinController,
                          label: 'PIN Temporal (OTP)',
                          hint: 'Ejemplo: 481516',
                          prefixIcon: Icons.pin_outlined,
                          keyboardType: TextInputType.number,
                          validator: (val) {
                            if (val == null || val.trim().isEmpty) return 'El PIN es requerido';
                            if (!RegExp(r'^\d{6}$').hasMatch(val.trim())) return 'Debe tener exactamente 6 dígitos numéricos';
                            return null;
                          },
                        ),
                        const SizedBox(height: 16),

                        // Cronómetro de Expiración
                        Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const Icon(Icons.timer_outlined, size: 18, color: AppTheme.textSecondary),
                            const SizedBox(width: 6),
                            Text(
                              'El código expira en: $_tiempoFormateado',
                              style: TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                                color: _segundosRestantes < 60 ? AppTheme.errorColor : AppTheme.textSecondary,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 24),

                        CustomButton(
                          text: 'Verificar y Entrar',
                          isLoading: authProvider.status == AuthStatus.loading,
                          onPressed: _verificarCodigo,
                        ),
                        const SizedBox(height: 12),

                        TextButton(
                          onPressed: _segundosRestantes == 0 ? _reenviarCodigo : null,
                          child: const Text('Reenviar nuevo código 2FA'),
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
}
