import '../../domain/entities/user_session.dart';

class AuthModel {
  static UserSession fromJson(Map<String, dynamic> json, {String dispositivo = 'MOVIL'}) {
    return UserSession(
      username: json['username'] ?? '',
      token: json['token'] ?? '',
      sessionId: json['sessionId'] ?? '',
      clienteId: json['clienteId'],
      nombreCompleto: json['nombreCompleto'] ?? '',
      dispositivo: dispositivo,
      requiere2fa: json['requiere2fa'] ?? false,
    );
  }
}
