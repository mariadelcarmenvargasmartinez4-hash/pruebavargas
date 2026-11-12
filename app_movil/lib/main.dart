import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'core/network/api_client.dart';
import 'core/storage/secure_storage_service.dart';
import 'core/theme/app_theme.dart';
import 'data/datasources/auth_remote_datasource.dart';
import 'data/datasources/banco_remote_datasource.dart';
import 'data/repositories/auth_repository_impl.dart';
import 'data/repositories/banco_repository_impl.dart';
import 'presentation/screens/dashboard_screen.dart';
import 'presentation/screens/login_screen.dart';
import 'presentation/state/auth_provider.dart';
import 'presentation/state/banco_provider.dart';
import 'presentation/widgets/loading_spinner.dart';
import 'presentation/widgets/session_conflict_dialog.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  final storageService = SecureStorageService();

  late final AuthProvider authProvider;

  // Cliente de red con callback para detección de conflicto de sesión única
  final apiClient = ApiClient(
    storageService: storageService,
    onSessionConflict: (message) {
      authProvider.handleSessionConflict(message);
    },
  );

  final authRemoteDataSource = AuthRemoteDataSource(apiClient: apiClient);
  final bancoRemoteDataSource = BancoRemoteDataSource(apiClient: apiClient);

  final authRepository = AuthRepositoryImpl(
    remoteDataSource: authRemoteDataSource,
    storageService: storageService,
  );

  final bancoRepository = BancoRepositoryImpl(
    remoteDataSource: bancoRemoteDataSource,
  );

  authProvider = AuthProvider(authRepository: authRepository);
  final bancoProvider = BancoProvider(bancoRepository: bancoRepository);

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider.value(value: authProvider),
        ChangeNotifierProvider.value(value: bancoProvider),
      ],
      child: const GestoPagoApp(),
    ),
  );
}

class GestoPagoApp extends StatefulWidget {
  const GestoPagoApp({super.key});

  @override
  State<GestoPagoApp> createState() => _GestoPagoAppState();
}

class _GestoPagoAppState extends State<GestoPagoApp> {
  final GlobalKey<NavigatorState> _navigatorKey = GlobalKey<NavigatorState>();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AuthProvider>().checkInitialSession();
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      navigatorKey: _navigatorKey,
      title: 'Banca y Pagos GestoPago',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      home: Consumer<AuthProvider>(
        builder: (context, auth, _) {
          // Escuchar conflicto de sesión única respaldada por el servidor
          if (auth.status == AuthStatus.sessionConflict) {
            WidgetsBinding.instance.addPostFrameCallback((_) {
              showDialog(
                context: _navigatorKey.currentContext ?? context,
                barrierDismissible: false,
                builder: (ctx) => SessionConflictDialog(
                  message: auth.conflictMessage ??
                      'Se ha iniciado sesión desde otro dispositivo o la sesión fue cerrada.',
                  onAccepted: () {
                    Navigator.of(ctx).pop();
                    auth.logout();
                  },
                ),
              );
            });
          }

          if (auth.status == AuthStatus.initial ||
              (auth.status == AuthStatus.loading && auth.session == null)) {
            return const Scaffold(
              body: LoadingSpinner(message: 'Verificando sesión segura...'),
            );
          }

          if (auth.status == AuthStatus.authenticated) {
            return const DashboardScreen();
          }

          return const LoginScreen();
        },
      ),
    );
  }
}
