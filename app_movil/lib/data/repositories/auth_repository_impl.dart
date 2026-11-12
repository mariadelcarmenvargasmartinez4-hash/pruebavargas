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
  Future<void> logout() async {
    await remoteDataSource.logout();
    await storageService.clearAll();
  }

  @override
  Future<bool> checkActiveSession() async {
    final token = await storageService.getToken();
    final sessionId = await storageService.getSessionId();
    if (token == null || sessionId == null) return false;

    try {
      final res = await remoteDataSource.getSesion();
      return res['codigo'] == 0 || (res.containsKey('datos') && res['datos'] != null);
    } catch (_) {
      await storageService.clearAll();
      return false;
    }
  }
}
