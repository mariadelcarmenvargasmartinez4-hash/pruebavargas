import 'package:dio/dio.dart';
import '../../core/storage/secure_storage_service.dart';
import '../../domain/entities/user_session.dart';
import '../../domain/repositories/auth_repository.dart';
import '../datasources/auth_remote_datasource.dart';
import '../models/auth_model.dart';

class AuthRepositoryImpl implements AuthRepository {
  final AuthRemoteDataSource remoteDataSource;
  final SecureStorageService storageService;

  AuthRepositoryImpl({
    required this.remoteDataSource,
    required this.storageService,
  });

  @override
  Future<UserSession> login({
    required String correo,
    required String password,
    required String dispositivo,
  }) async {
    final data = await remoteDataSource.login(correo, password, dispositivo);
    final session = AuthModel.fromJson(data, dispositivo: dispositivo);

    if (session.token.isNotEmpty) {
      await storageService.saveToken(session.token);
      await storageService.saveSessionId(session.sessionId);
      await storageService.saveUsername(session.username);
      await storageService.saveDeviceType(dispositivo);
    }
    return session;
  }

  @override
  Future<String> solicitar2Fa(String username) async {
    final data = await remoteDataSource.solicitar2Fa(username);
    if (data.containsKey('mensaje')) {
      return data['mensaje'].toString();
    }
    return 'Código 2FA generado y enviado.';
  }

  @override
  Future<UserSession> verificar2Fa({
    required String username,
    required String pin,
    required String dispositivo,
  }) async {
    final data = await remoteDataSource.verificar2Fa(username, pin, dispositivo);
    final session = AuthModel.fromJson(data, dispositivo: dispositivo);

    if (session.token.isNotEmpty) {
      await storageService.saveToken(session.token);
      await storageService.saveSessionId(session.sessionId);
      await storageService.saveUsername(session.username);
      await storageService.saveDeviceType(dispositivo);
    }
    return session;
  }

  @override
  Future<Map<String, dynamic>> registrarCliente(Map<String, dynamic> datosCliente) async {
    return await remoteDataSource.registrarCliente(datosCliente);
  }

  @override
  Future<void> logout() async {
    await remoteDataSource.logout();
    await storageService.clearAll();
  }

  @override
  Future<UserSession?> checkActiveSession() async {
    final token = await storageService.getToken();
    final sessionId = await storageService.getSessionId();
    final username = await storageService.getUsername();
    final dispositivo = await storageService.getDeviceType() ?? 'MOVIL';

    if (token == null || token.isEmpty || sessionId == null || sessionId.isEmpty) {
      return null;
    }

    try {
      final res = await remoteDataSource.getSesion();
      if (res['codigo'] == 0 || (res.containsKey('datos') && res['datos'] != null)) {
        final datos = res['datos'];
        final nombreCompleto = (datos is Map && datos['username'] != null)
            ? datos['username'].toString()
            : (username ?? 'Usuario');

        return UserSession(
          username: username ?? '',
          token: token,
          sessionId: sessionId,
          clienteId: null,
          nombreCompleto: nombreCompleto,
          dispositivo: dispositivo,
          requiere2fa: false,
        );
      }
      await storageService.clearAll();
      return null;
    } on DioException catch (e) {
      // Solo limpiar almacenamiento si el servidor explícitamente rechazó la sesión con 401
      if (e.response?.statusCode == 401) {
        await storageService.clearAll();
      }
      return null;
    } catch (_) {
      return null;
    }
  }
}
