import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../constants/api_constants.dart';

class SecureStorageService {
  final FlutterSecureStorage _storage;

  SecureStorageService({FlutterSecureStorage? storage})
      : _storage = storage ??
            const FlutterSecureStorage(
              aOptions: AndroidOptions(encryptedSharedPreferences: true),
              iOptions: IOSOptions(accessibility: KeychainAccessibility.first_unlock),
            );

  Future<void> saveToken(String token) async {
    await _storage.write(key: ApiConstants.keyJwtToken, value: token);
  }

  Future<String?> getToken() async {
    return await _storage.read(key: ApiConstants.keyJwtToken);
  }

  Future<void> saveSessionId(String sessionId) async {
    await _storage.write(key: ApiConstants.keySessionId, value: sessionId);
  }

  Future<String?> getSessionId() async {
    return await _storage.read(key: ApiConstants.keySessionId);
  }

  Future<void> saveUsername(String username) async {
    await _storage.write(key: ApiConstants.keyUsername, value: username);
  }

  Future<String?> getUsername() async {
    return await _storage.read(key: ApiConstants.keyUsername);
  }

  Future<void> saveDeviceType(String deviceType) async {
    await _storage.write(key: ApiConstants.keyDispositivo, value: deviceType);
  }

  Future<String?> getDeviceType() async {
    return await _storage.read(key: ApiConstants.keyDispositivo);
  }

  Future<void> clearAll() async {
    await _storage.deleteAll();
  }
}
