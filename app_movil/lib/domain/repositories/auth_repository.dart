import '../entities/user_session.dart';

abstract class AuthRepository {
  Future<UserSession> login({
    required String correo,
    required String password,
    required String dispositivo,
  });

  Future<String> solicitar2Fa(String username);

  Future<UserSession> verificar2Fa({
    required String username,
    required String pin,
    required String dispositivo,
  });

  Future<Map<String, dynamic>> registrarCliente(Map<String, dynamic> datosCliente);

  Future<void> logout();

  Future<UserSession?> checkActiveSession();
}
