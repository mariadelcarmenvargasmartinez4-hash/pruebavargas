import '../../core/constants/api_constants.dart';
import '../../core/network/api_client.dart';

class BancoRemoteDataSource {
  final ApiClient apiClient;

  BancoRemoteDataSource({required this.apiClient});

  Future<List<dynamic>> getCuentas() async {
    final response = await apiClient.dio.get(ApiConstants.cuentas);
    final data = response.data;
    if (data is Map && data.containsKey('datos')) {
      final contenido = data['datos'];
      if (contenido is Map && contenido.containsKey('contenido')) {
        return contenido['contenido'] as List<dynamic>;
      }
      if (contenido is List) {
        return contenido;
      }
    }
    if (data is List) return data;
    return [];
  }

  Future<Map<String, dynamic>> getCuentaPorNumero(String numeroCuenta) async {
    final response = await apiClient.dio.get('${ApiConstants.cuentas}/$numeroCuenta');
    return response.data as Map<String, dynamic>;
  }

  Future<List<dynamic>> getProductos() async {
    final response = await apiClient.dio.get(ApiConstants.productos);
    final data = response.data;
    if (data is Map && data.containsKey('datos')) {
      return data['datos'] as List<dynamic>;
    }
    if (data is List) return data;
    return [];
  }

  Future<Map<String, dynamic>> procesarPago(Map<String, dynamic> payload) async {
    final response = await apiClient.dio.post(
      ApiConstants.pagosProcesar,
      data: payload,
    );
    return response.data as Map<String, dynamic>;
  }
}
