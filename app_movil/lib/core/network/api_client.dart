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

  void setBaseUrl(String newUrl) {
    dio.options.baseUrl = newUrl;
    ApiConstants.activeBaseUrl = newUrl;
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
          final path = error.requestOptions.path;
          final isAuthEndpoint = path.contains('/auth/login') ||
              path.contains('/auth/2fa') ||
              path.contains('/clientes');

          // Detección de control de sesión única respaldada por el servidor (401 Unauthorized)
          if (!isAuthEndpoint && error.response?.statusCode == 401) {
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

  // Extrae mapa de errores específicos por campo ({ "campo": "mensaje" }) devueltos por el backend (HTTP 400/409/422)
  static Map<String, String> extractFieldErrors(dynamic error) {
    final Map<String, String> fieldErrors = {};
    if (error is DioException && error.response != null) {
      final data = error.response?.data;
      if (data is Map) {
        if (data.containsKey('datos') && data['datos'] is Map) {
          final datos = data['datos'] as Map;
          datos.forEach((key, value) {
            if (value != null) {
              fieldErrors[key.toString()] = value.toString();
            }
          });
        }

        // Mapeo contextual de códigos 409 Conflict o duplicidades
        final statusCode = error.response?.statusCode;
        final mensaje = (data['mensaje'] ?? data['message'] ?? '').toString();
        final msgLower = mensaje.toLowerCase();

        if (statusCode == 409 || msgLower.contains('ya está registrado') || msgLower.contains('duplicad')) {
          if (msgLower.contains('curp')) {
            fieldErrors['curp'] = mensaje;
          } else if (msgLower.contains('rfc')) {
            fieldErrors['rfc'] = mensaje;
          } else if (msgLower.contains('correo')) {
            fieldErrors['correoElectronico'] = mensaje;
          }
        }
      }
    }
    return fieldErrors;
  }
}
