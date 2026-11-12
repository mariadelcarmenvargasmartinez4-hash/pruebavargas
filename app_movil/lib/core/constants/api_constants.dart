class ApiConstants {
  // En Android Emulator usar 10.0.2.2, en Web/Desktop localhost, o la URL de Render desplegada
  static const String baseUrlLocal = 'http://10.0.2.2:8088';
  static const String baseUrlWeb = 'http://localhost:8088';
  static const String baseUrlRender = 'https://api-registro-clientes.onrender.com';

  // Configuración de endpoint activo
  static String activeBaseUrl = baseUrlLocal;

  // Endpoints Autenticación y 2FA
  static const String login = '/auth/login';
  static const String solicitar2fa = '/auth/2fa/solicitar';
  static const String verificar2fa = '/auth/2fa/verificar';
  static const String logout = '/auth/logout';
  static const String sesion = '/auth/sesion';

  // Endpoints Negocio Bancario y Servicios
  static const String cuentas = '/cuentas';
  static const String pagosProcesar = '/pagos';
  static const String pagosHistorial = '/pagos/historial';
  static const String productos = '/productos';
  static const String catalogos = '/api/catalogos';

  // Claves de Almacenamiento Seguro
  static const String keyJwtToken = 'jwt_auth_token';
  static const String keySessionId = 'server_session_id';
  static const String keyUsername = 'auth_username';
  static const String keyDispositivo = 'device_type';

  // Timeouts en milisegundos
  static const int connectTimeout = 10000;
  static const int receiveTimeout = 10000;
}
