import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../../core/network/api_client.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/responsive_util.dart';
import '../../data/datasources/auth_remote_datasource.dart';
import '../widgets/custom_button.dart';

class RecuperarPasswordScreen extends StatefulWidget {
  const RecuperarPasswordScreen({super.key});

  @override
  State<RecuperarPasswordScreen> createState() => _RecuperarPasswordScreenState();
}

class _RecuperarPasswordScreenState extends State<RecuperarPasswordScreen> {
  final _formKey = GlobalKey<FormState>();

  final _correoController = TextEditingController();
  final _codigoController = TextEditingController();
  final _nuevaPasswordController = TextEditingController();
  final _confirmarPasswordController = TextEditingController();

  // Etapa 0: Ingresar correo | Etapa 1: Validar código | Etapa 2: Nueva contraseña
  int _etapa = 0;
  bool _cargando = false;
  bool _ocultarNueva = true;
  bool _ocultarConfirmar = true;

  String _correoCapturado = '';
  String _resetToken = '';

  int _segundosExpiracion = 600;
  int _segundosReenvio = 0;
  Timer? _timer;

  @override
  void dispose() {
    _timer?.cancel();
    _correoController.dispose();
    _codigoController.dispose();
    _nuevaPasswordController.dispose();
    _confirmarPasswordController.dispose();
    super.dispose();
  }

  void _iniciarTemporizador(int expiracionSegundos) {
    _timer?.cancel();
    _segundosExpiracion = expiracionSegundos > 0 ? expiracionSegundos : 600;
    _segundosReenvio = 45;
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) {
        timer.cancel();
        return;
      }
      setState(() {
        if (_segundosExpiracion > 0) _segundosExpiracion--;
        if (_segundosReenvio > 0) _segundosReenvio--;
      });
      if (_segundosExpiracion <= 0 && _segundosReenvio <= 0) {
        timer.cancel();
      }
    });
  }

  String get _tiempoExpiracionFormateado {
    final min = (_segundosExpiracion ~/ 60).toString().padLeft(2, '0');
    final seg = (_segundosExpiracion % 60).toString().padLeft(2, '0');
    return '$min:$seg';
  }

  void _mostrarMensaje(String texto, {bool esError = true}) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(texto),
        backgroundColor: esError ? AppTheme.errorColor : AppTheme.successColor,
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  String? _validarCorreo(String? val) {
    if (val == null || val.trim().isEmpty) {
      return 'El correo electrónico es obligatorio';
    }
    final correo = val.trim();
    if (correo.length > 100) {
      return 'El correo no debe exceder 100 caracteres';
    }
    if (!RegExp(r'^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$').hasMatch(correo)) {
      return 'Ingrese un correo electrónico válido';
    }
    return null;
  }

  String? _validarCodigo(String? val) {
    if (val == null || val.trim().isEmpty) {
      return 'Ingrese el código recibido en su correo';
    }
    if (!RegExp(r'^\d{6}$').hasMatch(val.trim())) {
      return 'El código debe contener exactamente 6 dígitos numéricos';
    }
    return null;
  }

  String? _validarNuevaPassword(String? val) {
    if (val == null || val.isEmpty) {
      return 'La nueva contraseña es obligatoria';
    }
    if (val.length < 8 || val.length > 50) {
      return 'La contraseña debe tener entre 8 y 50 caracteres';
    }
    if (val.contains(' ')) {
      return 'La contraseña no puede contener espacios';
    }
    if (!RegExp(r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d\s]).{8,50}$').hasMatch(val)) {
      return 'Debe incluir al menos 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial';
    }
    return null;
  }

  String? _validarConfirmacion(String? val) {
    if (val == null || val.isEmpty) {
      return 'Confirme su nueva contraseña';
    }
    if (val != _nuevaPasswordController.text) {
      return 'Las contraseñas no coinciden';
    }
    return null;
  }

  Future<void> _enviarCodigo({bool esReenvio = false}) async {
    if (!esReenvio && !_formKey.currentState!.validate()) return;

    final correo = esReenvio ? _correoCapturado : _correoController.text.trim().toLowerCase();
    setState(() => _cargando = true);

    try {
      final apiClient = context.read<ApiClient>();
      final dataSource = AuthRemoteDataSource(apiClient: apiClient);
      final resp = await dataSource.solicitarRecuperacionPassword(correo);

      if (!mounted) return;

      int expiracion = 600;
      if (resp['datos'] is Map && resp['datos']['expiracionSegundos'] is int) {
        expiracion = resp['datos']['expiracionSegundos'] as int;
      }

      setState(() {
        _correoCapturado = correo;
        _codigoController.clear();
        _etapa = 1;
        _cargando = false;
      });
      _iniciarTemporizador(expiracion);

      final mensaje = (resp['mensaje'] ??
              'Si el correo electrónico está registrado en nuestro sistema, recibirás un código temporal de recuperación.')
          .toString();
      _mostrarMensaje(mensaje, esError: false);
    } catch (e) {
      if (!mounted) return;
      setState(() => _cargando = false);
      _mostrarMensaje(ApiClient.parseError(e));
    }
  }

  Future<void> _verificarCodigo() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _cargando = true);
    try {
      final apiClient = context.read<ApiClient>();
      final dataSource = AuthRemoteDataSource(apiClient: apiClient);
      final resp = await dataSource.verificarCodigoRecuperacion(
        _correoCapturado,
        _codigoController.text.trim(),
      );

      if (!mounted) return;

      final datos = resp['datos'];
      final verificado = datos is Map && datos['verificado'] == true;
      final token = datos is Map ? (datos['resetToken'] ?? '').toString() : '';

      if (!verificado || token.isEmpty) {
        setState(() => _cargando = false);
        _mostrarMensaje('No fue posible verificar el código. Intente nuevamente.');
        return;
      }

      _timer?.cancel();
      setState(() {
        _resetToken = token;
        _nuevaPasswordController.clear();
        _confirmarPasswordController.clear();
        _etapa = 2;
        _cargando = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _cargando = false);
      _mostrarMensaje(ApiClient.parseError(e));
    }
  }

  Future<void> _guardarNuevaPassword() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _cargando = true);
    try {
      final apiClient = context.read<ApiClient>();
      final dataSource = AuthRemoteDataSource(apiClient: apiClient);
      await dataSource.restablecerPassword(
        correo: _correoCapturado,
        resetToken: _resetToken,
        nuevaPassword: _nuevaPasswordController.text,
        confirmarPassword: _confirmarPasswordController.text,
      );

      if (!mounted) return;
      setState(() => _cargando = false);

      TextInput.finishAutofillContext(shouldSave: false);
      _nuevaPasswordController.clear();
      _confirmarPasswordController.clear();

      await showDialog<void>(
        context: context,
        barrierDismissible: false,
        builder: (ctx) => AlertDialog(
          icon: const Icon(Icons.check_circle, color: Colors.green, size: 48),
          title: const Text('¡Contraseña Actualizada!'),
          content: const Text(
            'Tu nueva contraseña se ha guardado de forma segura. Ahora puedes iniciar sesión con tus nuevas credenciales.',
          ),
          actions: [
            ElevatedButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Ir al Inicio de Sesión'),
            ),
          ],
        ),
      );

      if (mounted) {
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      if (!mounted) return;
      setState(() => _cargando = false);
      _mostrarMensaje(ApiClient.parseError(e));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Recuperar Contraseña'),
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: ResponsiveUtil.pagePadding(context),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 460),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 32),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Container(
                          width: 60,
                          height: 60,
                          decoration: BoxDecoration(
                            color: AppTheme.primaryColor.withOpacity(0.08),
                            shape: BoxShape.circle,
                          ),
                          child: Icon(
                            _etapa == 0
                                ? Icons.mark_email_read_outlined
                                : (_etapa == 1 ? Icons.pin_outlined : Icons.lock_reset_rounded),
                            size: 32,
                            color: AppTheme.primaryColor,
                          ),
                        ),
                        const SizedBox(height: 16),
                        Text(
                          _etapa == 0
                              ? 'Recuperación por Correo'
                              : (_etapa == 1 ? 'Validar Código Temporal' : 'Crear Nueva Contraseña'),
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                            color: AppTheme.primaryColor,
                          ),
                        ),
                        const SizedBox(height: 8),
                        if (_etapa == 0) ..._buildEtapaCorreo(),
                        if (_etapa == 1) ..._buildEtapaCodigo(),
                        if (_etapa == 2) ..._buildEtapaNuevaPassword(),
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

  List<Widget> _buildEtapaCorreo() {
    return [
      const Text(
        'Ingresa el correo electrónico con el que registraste tu cuenta bancaria para enviarte un código temporal de verificación.',
        textAlign: TextAlign.center,
        style: TextStyle(fontSize: 13, color: AppTheme.textSecondary),
      ),
      const SizedBox(height: 24),
      TextFormField(
        controller: _correoController,
        keyboardType: TextInputType.emailAddress,
        autofillHints: const <String>[],
        enableSuggestions: false,
        autocorrect: false,
        enableIMEPersonalizedLearning: false,
        autovalidateMode: AutovalidateMode.onUserInteraction,
        inputFormatters: [
          FilteringTextInputFormatter.deny(RegExp(r'\s')),
          LengthLimitingTextInputFormatter(100),
        ],
        decoration: const InputDecoration(
          labelText: 'Correo Electrónico',
          hintText: 'usuario@ejemplo.com',
          prefixIcon: Icon(Icons.email_outlined, size: 22),
          errorMaxLines: 4,
        ),
        validator: _validarCorreo,
      ),
      const SizedBox(height: 24),
      CustomButton(
        text: 'Enviar código',
        isLoading: _cargando,
        onPressed: _cargando ? null : _enviarCodigo,
      ),
      const SizedBox(height: 10),
      TextButton(
        onPressed: _cargando ? null : () => Navigator.of(context).pop(false),
        child: const Text('Regresar al inicio de sesión'),
      ),
    ];
  }

  List<Widget> _buildEtapaCodigo() {
    return [
      const Text(
        'Si el correo ingresado se encuentra registrado, hemos enviado un código temporal de 6 dígitos. Revísalo e introdúcelo a continuación:',
        textAlign: TextAlign.center,
        style: TextStyle(fontSize: 13, color: AppTheme.textSecondary),
      ),
      const SizedBox(height: 24),
      TextFormField(
        controller: _codigoController,
        keyboardType: TextInputType.number,
        autofillHints: const <String>[],
        enableSuggestions: false,
        autocorrect: false,
        enableIMEPersonalizedLearning: false,
        autovalidateMode: AutovalidateMode.onUserInteraction,
        inputFormatters: [
          FilteringTextInputFormatter.digitsOnly,
          LengthLimitingTextInputFormatter(6),
        ],
        decoration: const InputDecoration(
          labelText: 'Código de verificación',
          hintText: 'Ej. 482915',
          prefixIcon: Icon(Icons.pin_outlined, size: 22),
          errorMaxLines: 4,
        ),
        validator: _validarCodigo,
      ),
      const SizedBox(height: 14),
      Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.timer_outlined, size: 18, color: AppTheme.textSecondary),
          const SizedBox(width: 6),
          Text(
            'Vigencia del código: $_tiempoExpiracionFormateado',
            style: TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: _segundosExpiracion < 60 ? AppTheme.errorColor : AppTheme.textSecondary,
            ),
          ),
        ],
      ),
      const SizedBox(height: 22),
      CustomButton(
        text: 'Validar código',
        isLoading: _cargando,
        onPressed: (_cargando || _segundosExpiracion == 0) ? null : _verificarCodigo,
      ),
      const SizedBox(height: 10),
      TextButton(
        onPressed: (!_cargando && _segundosReenvio == 0)
            ? () => _enviarCodigo(esReenvio: true)
            : null,
        child: Text(
          _segundosReenvio > 0
              ? 'Solicitar nuevo código ($_segundosReenvio s)'
              : 'Solicitar nuevo código',
        ),
      ),
    ];
  }

  List<Widget> _buildEtapaNuevaPassword() {
    return [
      const Text(
        'Código verificado. Ingresa y confirma tu nueva contraseña cumpliendo los criterios de seguridad bancaria.',
        textAlign: TextAlign.center,
        style: TextStyle(fontSize: 13, color: AppTheme.textSecondary),
      ),
      const SizedBox(height: 24),
      TextFormField(
        controller: _nuevaPasswordController,
        obscureText: _ocultarNueva,
        autofillHints: const <String>[],
        enableSuggestions: false,
        autocorrect: false,
        enableIMEPersonalizedLearning: false,
        autovalidateMode: AutovalidateMode.onUserInteraction,
        inputFormatters: [
          FilteringTextInputFormatter.deny(RegExp(r'\s')),
          LengthLimitingTextInputFormatter(50),
        ],
        onChanged: (_) => setState(() {}),
        decoration: InputDecoration(
          labelText: 'Nueva contraseña',
          hintText: 'Mín. 8 car., 1 mayús., 1 minús., 1 núm., 1 especial',
          prefixIcon: const Icon(Icons.lock_outline, size: 22),
          errorMaxLines: 5,
          suffixIcon: IconButton(
            icon: Icon(
              _ocultarNueva ? Icons.visibility_off : Icons.visibility,
              size: 20,
              color: AppTheme.textSecondary,
            ),
            onPressed: () => setState(() => _ocultarNueva = !_ocultarNueva),
          ),
        ),
        validator: _validarNuevaPassword,
      ),
      const SizedBox(height: 16),
      TextFormField(
        controller: _confirmarPasswordController,
        obscureText: _ocultarConfirmar,
        autofillHints: const <String>[],
        enableSuggestions: false,
        autocorrect: false,
        enableIMEPersonalizedLearning: false,
        autovalidateMode: AutovalidateMode.onUserInteraction,
        inputFormatters: [
          FilteringTextInputFormatter.deny(RegExp(r'\s')),
          LengthLimitingTextInputFormatter(50),
        ],
        decoration: InputDecoration(
          labelText: 'Confirmar nueva contraseña',
          prefixIcon: const Icon(Icons.lock_reset_outlined, size: 22),
          errorMaxLines: 4,
          suffixIcon: IconButton(
            icon: Icon(
              _ocultarConfirmar ? Icons.visibility_off : Icons.visibility,
              size: 20,
              color: AppTheme.textSecondary,
            ),
            onPressed: () => setState(() => _ocultarConfirmar = !_ocultarConfirmar),
          ),
        ),
        validator: _validarConfirmacion,
      ),
      const SizedBox(height: 24),
      CustomButton(
        text: 'Guardar contraseña',
        isLoading: _cargando,
        onPressed: _cargando ? null : _guardarNuevaPassword,
      ),
    ];
  }
}
