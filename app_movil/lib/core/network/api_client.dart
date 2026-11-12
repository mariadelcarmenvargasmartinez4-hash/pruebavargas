import 'package:dio/dio.dart';
import '../constants/api_constants.dart';
import '../storage/secure_storage_service.dart';

typedef OnSessionConflictCallback = void Function(String message);

class ApiClient {
  late final Dio dio;
  final SecureStorageService storageService;
  OnSessionConflictCallback? onSessionConflict;

  ApiClient({
    required this.storageService,
    this.onSessionConflict,
    String? baseUrl,
  }) {
    dio = Dio(
      BaseOptions(
        baseUrl: baseUrl ?? ApiConstants.activeBaseUrl,
        connectTimeout: const Duration(milliseconds: ApiConstants.connectTimeout),
        receiveTimeout: const Duration(milliseconds: ApiConstants.receiveTimeout),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );

    _setupInterceptors();
  }

  void _setupInterceptors() {
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          // Inyectar dinámicamente el token JWT almacenado de forma segura
          final token = await storageService.getToken();
          if (token != null && token.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          return handler.next(options);
        },
        onResponse: (response, handler) {
          return handler.next(response);
        },
        onError: (DioException error, handler) async {
          // Detección de control de sesión única respaldada por el servidor (401 Unauthorized)
          if (error.response?.statusCode == 401) {
            final data = error.response?.data;
            String msg = 'Sesión no válida o expirada.';
            if (data is Map && data.containsKey('mensaje')) {
              msg = data['mensaje'].toString();
            }

            // Si el servidor detectó inicio de sesión en otro dispositivo o sesión cerrada
            if (msg.contains('otro dispositivo') || msg.contains('Sesión finalizada')) {
              await storageService.clearAll();
              onSessionConflict?.call(msg);
            }
          }
          return handler.next(error);
        },
      ),
    );
  }

  // Traducción amigable de excepciones Dio a mensajes comprensibles
  static String parseError(dynamic error) {
    if (error is DioException) {
      if (error.type == DioExceptionType.connectionTimeout ||
          error.type == DioExceptionType.receiveTimeout) {
        return 'Tiempo de espera agotado. Verifique su conexión de red.';
      }
      if (error.type == DioExceptionType.connectionError) {
        return 'No se pudo conectar con el servidor bancario. Verifique que el backend esté activo.';
      }
      if (error.response != null) {
        final data = error.response?.data;
        if (data is Map) {
          if (data.containsKey('mensaje')) {
            return data['mensaje'].toString();
          }
          if (data.containsKey('message')) {
            return data['message'].toString();
          }
        }
        return 'Error del servidor (Código ${error.response?.statusCode})';
      }
    }
    return error.toString();
  }
}
