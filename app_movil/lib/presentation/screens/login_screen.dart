import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/constants/api_constants.dart';
import '../../core/network/api_client.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/responsive_util.dart';
import '../state/auth_provider.dart';
import '../widgets/custom_button.dart';
import '../widgets/custom_text_field.dart';
import 'dashboard_screen.dart';
import 'register_screen.dart';
import 'two_factor_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _correoController = TextEditingController(text: 'admin@gestopago.com');
  final _passwordController = TextEditingController(text: 'Segura123#');
  final _formKey = GlobalKey<FormState>();
  bool _obscurePassword = true;

  @override
  void dispose() {
    _correoController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submitLogin() async {
    if (!_formKey.currentState!.validate()) return;

    final authProvider = context.read<AuthProvider>();
    final success = await authProvider.login(
      _correoController.text.trim(),
      _passwordController.text,
    );

    if (!mounted) return;

    if (!success && authProvider.errorMessage != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(authProvider.errorMessage!),
          backgroundColor: AppTheme.errorColor,
          behavior: SnackBarBehavior.floating,
        ),
      );
    }
  }

  void _mostrarConfigServidor(BuildContext context) {
    final customController = TextEditingController(text: ApiConstants.activeBaseUrl);
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Conexión con el Servidor Backend', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Servidor activo actualmente:', style: TextStyle(fontSize: 12, color: Colors.grey)),
            Text(ApiConstants.activeBaseUrl, style: const TextStyle(fontWeight: FontWeight.bold, color: AppTheme.primaryColor)),
            const Divider(height: 24),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.wifi, color: AppTheme.primaryColor),
              title: const Text('Wi-Fi Local de tu PC'),
              subtitle: const Text('http://192.168.100.10:8088'),
              onTap: () {
                context.read<ApiClient>().setBaseUrl(ApiConstants.baseUrlWifi);
                setState(() {});
                Navigator.of(ctx).pop();
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Conectado a Wi-Fi Local (192.168.100.10:8088)')),
                );
              },
            ),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.cloud_outlined, color: AppTheme.primaryColor),
              title: const Text('Nube Render'),
              subtitle: const Text(ApiConstants.baseUrlRender),
              onTap: () {
                context.read<ApiClient>().setBaseUrl(ApiConstants.baseUrlRender);
                setState(() {});
                Navigator.of(ctx).pop();
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Conectado a Nube Render')),
                );
              },
            ),
            const SizedBox(height: 8),
            TextField(
              controller: customController,
              decoration: const InputDecoration(
                labelText: 'O escribir IP personalizada:',
                hintText: 'http://192.168.X.X:8088',
                isDense: true,
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            onPressed: () {
              final newUrl = customController.text.trim();
              if (newUrl.isNotEmpty) {
                context.read<ApiClient>().setBaseUrl(newUrl);
                setState(() {});
                Navigator.of(ctx).pop();
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('Conectado a: $newUrl')),
                );
              }
            },
            child: const Text('Guardar'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();
    final isDesktop = ResponsiveUtil.isDesktop(context);

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: ResponsiveUtil.pagePadding(context),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 460),
              child: Card(
                elevation: isDesktop ? 6 : 2,
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 36),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        // Indicador de conexión / configuración de servidor
                        Align(
                          alignment: Alignment.topRight,
                          child: InkWell(
                            onTap: () => _mostrarConfigServidor(context),
                            borderRadius: BorderRadius.circular(20),
                            child: Container(
                              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                              decoration: BoxDecoration(
                                color: Colors.blue.shade50,
                                borderRadius: BorderRadius.circular(20),
                                border: Border.all(color: Colors.blue.shade200),
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(Icons.dns_rounded, size: 14, color: Colors.blue.shade700),
                                  const SizedBox(width: 5),
                                  Text(
                                    ApiConstants.activeBaseUrl.replaceFirst('http://', '').replaceFirst('https://', ''),
                                    style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Colors.blue.shade800),
                                  ),
                                  const SizedBox(width: 4),
                                  Icon(Icons.edit, size: 12, color: Colors.blue.shade700),
                                ],
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: 12),

                        // Logo / Icono Bancario
                        Container(
                          width: 64,
                          height: 64,
                          decoration: BoxDecoration(
                            color: AppTheme.primaryColor.withOpacity(0.08),
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(
                            Icons.account_balance_rounded,
                            size: 34,
                            color: AppTheme.primaryColor,
                          ),
                        ),
                        const SizedBox(height: 16),
                        const Text(
                          'Proyecto Integrador Bancario',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.bold,
                            color: AppTheme.primaryColor,
                          ),
                        ),
                        const SizedBox(height: 6),
                        const Text(
                          'GestoPago - Acceso Seguro Multiplataforma',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            fontSize: 13,
                            color: AppTheme.textSecondary,
                          ),
                        ),
                        const SizedBox(height: 28),

                        // Campo Correo / Usuario
                        CustomTextField(
                          controller: _correoController,
                          label: 'Correo Electrónico',
                          hint: 'usuario@ejemplo.com',
                          prefixIcon: Icons.email_outlined,
                          keyboardType: TextInputType.emailAddress,
                          validator: (val) {
                            if (val == null || val.trim().isEmpty) return 'El correo es obligatorio';
                            if (!RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$').hasMatch(val.trim())) {
                              return 'Formato de correo inválido';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 18),

                        // Campo Contraseña
                        CustomTextField(
                          controller: _passwordController,
                          label: 'Contraseña',
                          prefixIcon: Icons.lock_outline,
                          obscureText: _obscurePassword,
                          suffixIcon: IconButton(
                            icon: Icon(
                              _obscurePassword ? Icons.visibility_off : Icons.visibility,
                              size: 20,
                              color: AppTheme.textSecondary,
                            ),
                            onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                          ),
                          validator: (val) {
                            if (val == null || val.isEmpty) return 'La contraseña es obligatoria';
                            return null;
                          },
                        ),
                        const SizedBox(height: 24),

                        // Botón de Inicio de Sesión
                        CustomButton(
                          text: 'Iniciar Sesión',
                          isLoading: authProvider.status == AuthStatus.loading,
                          onPressed: _submitLogin,
                        ),
                        const SizedBox(height: 12),

                        // Enlace para Crear Nueva Cuenta
                        Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const Text(
                              '¿Aún no tienes cuenta?',
                              style: TextStyle(fontSize: 13, color: AppTheme.textSecondary),
                            ),
                            TextButton(
                              onPressed: () async {
                                final nuevoCorreo = await Navigator.of(context).push<String>(
                                  MaterialPageRoute(builder: (_) => const RegisterScreen()),
                                );
                                if (nuevoCorreo != null && nuevoCorreo.isNotEmpty) {
                                  setState(() {
                                    _correoController.text = nuevoCorreo;
                                  });
                                }
                              },
                              child: const Text(
                                'Crear cuenta bancaria',
                                style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  color: AppTheme.primaryColor,
                                ),
                              ),
                            ),
                          ],
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
