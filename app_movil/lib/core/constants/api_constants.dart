class ApiConstants {
  // En Celular Físico usar la IP Wi-Fi de la PC, en Emulador 10.0.2.2, en Web localhost, o Render en la nube
  static const String baseUrlWifi = 'http://192.168.100.10:8088';
  static const String baseUrlLocal = 'http://10.0.2.2:8088';
  static const String baseUrlWeb = 'http://localhost:8088';
  static const String baseUrlRender = 'https://api-registro-clientes.onrender.com';

  // Configuración de endpoint activo (Por defecto Wi-Fi de la máquina para pruebas en celular físico)
  static String activeBaseUrl = baseUrlWifi;

  // Endpoints Autenticación y 2FA
  static const String login = '/auth/login';
  static const String solicitar2fa = '/auth/2fa/solicitar';
  static const String verificar2fa = '/auth/2fa/verificar';
  static const String logout = '/auth/logout';
  static const String sesion = '/auth/sesion';
  static const String recuperacionSolicitar = '/auth/recuperacion/solicitar';
  static const String recuperacionVerificar = '/auth/recuperacion/verificar';
  static const String recuperacionRestablecer = '/auth/recuperacion/restablecer';

  // Endpoints Negocio Bancario y Servicios
  static const String clientes = '/clientes';
  static const String cuentas = '/cuentas';
  static const String pagosProcesar = '/pagos';
  static const String pagosSolicitarPin = '/pagos/pin-transaccional/solicitar';
  static const String pagosHistorial = '/pagos/historial';
  static const String productos = '/productos';
  static const String catalogos = '/api/catalogos';

  // Claves de Almacenamiento Seguro
  static const String keyJwtToken = 'jwt_auth_token';
  static const String keySessionId = 'server_session_id';
  static const String keyUsername = 'auth_username';
  static const String keyDispositivo = 'device_type';

  // Timeouts en milisegundos (la BD remota en Render puede tardar varios segundos por operación)
  static const int connectTimeout = 20000;
  static const int receiveTimeout = 60000;
}
