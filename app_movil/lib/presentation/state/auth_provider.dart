import 'package:flutter/material.dart';
import '../../core/network/api_client.dart';
import '../../domain/entities/user_session.dart';
import '../../domain/repositories/auth_repository.dart';

enum AuthStatus {
  initial,
  loading,
  authenticated,
  twoFactorRequired,
  unauthenticated,
  sessionConflict
}

class AuthProvider extends ChangeNotifier {
  final AuthRepository authRepository;

  AuthStatus _status = AuthStatus.initial;
  UserSession? _session;
  String? _errorMessage;
  String? _conflictMessage;
  String _pendingUsername = '';
  String _dispositivoActual = 'MOVIL';

  AuthProvider({required this.authRepository});

  AuthStatus get status => _status;
  UserSession? get session => _session;
  String? get errorMessage => _errorMessage;
  String? get conflictMessage => _conflictMessage;
  String get pendingUsername => _pendingUsername;
  String get dispositivoActual => _dispositivoActual;

  void setDispositivo(String disp) {
    _dispositivoActual = disp;
    notifyListeners();
  }

  void handleSessionConflict(String message) {
    _status = AuthStatus.sessionConflict;
    _conflictMessage = message;
    _session = null;
    notifyListeners();
  }

  Future<void> checkInitialSession() async {
    _status = AuthStatus.loading;
    notifyListeners();

    final isAlive = await authRepository.checkActiveSession();
    if (isAlive) {
      _status = AuthStatus.authenticated;
    } else {
      _status = AuthStatus.unauthenticated;
    }
    notifyListeners();
  }

  Future<bool> login(String correo, String password) async {
    _status = AuthStatus.loading;
    _errorMessage = null;
    notifyListeners();

    try {
      final session = await authRepository.login(
        correo: correo,
        password: password,
        dispositivo: _dispositivoActual,
      );

      _session = session;
      _pendingUsername = correo;

      if (session.requiere2fa) {
        _status = AuthStatus.twoFactorRequired;
      } else {
        _status = AuthStatus.authenticated;
      }
      notifyListeners();
      return true;
    } catch (e) {
      _status = AuthStatus.unauthenticated;
      _errorMessage = ApiClient.parseError(e);
      notifyListeners();
      return false;
    }
  }

  Future<bool> solicitar2Fa() async {
    try {
      await authRepository.solicitar2Fa(_pendingUsername);
      return true;
    } catch (e) {
      _errorMessage = ApiClient.parseError(e);
      notifyListeners();
      return false;
    }
  }

  Future<bool> verificar2Fa(String pin) async {
    _status = AuthStatus.loading;
    _errorMessage = null;
    notifyListeners();

    try {
      final session = await authRepository.verificar2Fa(
        username: _pendingUsername,
        pin: pin,
        dispositivo: _dispositivoActual,
      );

      _session = session;
      _status = AuthStatus.authenticated;
      notifyListeners();
      return true;
    } catch (e) {
      _status = AuthStatus.twoFactorRequired;
      _errorMessage = ApiClient.parseError(e);
      notifyListeners();
      return false;
    }
  }

  Future<void> logout() async {
    _status = AuthStatus.loading;
    notifyListeners();

    await authRepository.logout();
    _session = null;
    _status = AuthStatus.unauthenticated;
    _errorMessage = null;
    _conflictMessage = null;
    notifyListeners();
  }

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }
}
