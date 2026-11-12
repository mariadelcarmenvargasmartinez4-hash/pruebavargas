class UserSession {
  final String username;
  final String token;
  final String sessionId;
  final Long? clienteId;
  final String nombreCompleto;
  final String dispositivo;
  final bool requiere2fa;

  const UserSession({
    required this.username,
    required this.token,
    required this.sessionId,
    this.clienteId,
    required this.nombreCompleto,
    required this.dispositivo,
    this.requiere2fa = false,
  });
}

typedef Long = int;
