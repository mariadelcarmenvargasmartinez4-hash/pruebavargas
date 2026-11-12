import '../../core/constants/api_constants.dart';
import '../../core/network/api_client.dart';

class AuthRemoteDataSource {
  final ApiClient apiClient;

  AuthRemoteDataSource({required this.apiClient});

  Future<Map<String, dynamic>> login(String correo, String password, String dispositivo) async {
    final response = await apiClient.dio.post(
      ApiConstants.login,
      data: {
        'correo': correo,
        'password': password,
        'dispositivo': dispositivo,
      },
    );
    return response.data as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> solicitar2Fa(String username) async {
    final response = await apiClient.dio.post(
      ApiConstants.solicitar2fa,
      data: {'username': username},
    );
    return response.data as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> verificar2Fa(String username, String pin, String dispositivo) async {
    final response = await apiClient.dio.post(
      ApiConstants.verificar2fa,
      data: {
        'username': username,
        'pin': pin,
        'dispositivo': dispositivo,
      },
    );
    return response.data as Map<String, dynamic>;
  }

  Future<void> logout() async {
    try {
      await apiClient.dio.post(ApiConstants.logout);
    } catch (_) {
      // Ignorar error si el token ya expiró
    }
  }

  Future<Map<String, dynamic>> getSesion() async {
    final response = await apiClient.dio.get(ApiConstants.sesion);
    return response.data as Map<String, dynamic>;
  }
}
