import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../../core/constants/api_constants.dart';
import '../../core/network/api_client.dart';
import '../../core/storage/secure_storage_service.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/responsive_util.dart';
import '../state/auth_provider.dart';
import '../widgets/custom_button.dart';
import '../widgets/custom_text_field.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

/// Fila adaptable: en pantallas angostas (celular) apila los campos en columna
/// para que no se vean amontonados; en pantallas anchas conserva las columnas.
class _FilaResponsiva extends StatelessWidget {
  final List<Widget> children;
  final CrossAxisAlignment crossAxisAlignment;

  const _FilaResponsiva({
    required this.children,
    this.crossAxisAlignment = CrossAxisAlignment.center,
  });

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        if (constraints.maxWidth < 560) {
          final apilados = <Widget>[];
          for (final w in children) {
            if (w is SizedBox && w.width != null) {
              apilados.add(const SizedBox(height: 12));
            } else if (w is Expanded) {
              apilados.add(w.child);
            } else {
              apilados.add(w);
            }
          }
          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: apilados,
          );
        }
        return Row(crossAxisAlignment: crossAxisAlignment, children: children);
      },
    );
  }
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();

  // Palabras reservadas prohibidas (alineado con TextoNombreValidator del backend)
  static const Set<String> _palabrasProhibidas = {
    'EXECUTE', 'EXEC', 'SELECT', 'INSERT', 'UPDATE', 'DELETE', 'DROP', 'ALTER',
    'CREATE', 'TRUNCATE', 'SCRIPT', 'JAVASCRIPT', 'NULL', 'UNDEFINED', 'TRUE',
    'FALSE', 'SI', 'NO', 'EVAL', 'WHERE', 'FROM', 'TABLE', 'JOIN', 'UNION',
    'GRANT', 'REVOKE', 'FETCH', 'FUNCTION', 'PROCEDURE', 'DECLARE'
  };

  // Errores específicos mapeados desde el backend (HTTP 400/409/422)
  Map<String, String> _serverErrors = {};

  // Datos Personales
  final _nombreController = TextEditingController();
  final _apellidoPaternoController = TextEditingController();
  final _apellidoMaternoController = TextEditingController();
  final _fechaNacimientoController = TextEditingController(text: '1995-05-15');
  final _curpController = TextEditingController();
  final _rfcController = TextEditingController();

  String _sexo = 'MASCULINO';
  String _nacionalidad = 'MEXICANA';
  String _estadoCivil = 'SOLTERO';
  String _entidadNacimiento = 'DF';

  // Banderas de edición manual para respetar escrituras del usuario
  bool _curpEditadoManualmente = false;
  bool _rfcEditadoManualmente = false;

  // Catálogo dinámico de Entidades Federativas de México (32 entidades)
  List<Map<String, String>> _entidadesFederativasCatalogo = [
    {'codigo': 'AS', 'nombre': 'Aguascalientes'},
    {'codigo': 'BC', 'nombre': 'Baja California'},
    {'codigo': 'BS', 'nombre': 'Baja California Sur'},
    {'codigo': 'CC', 'nombre': 'Campeche'},
    {'codigo': 'CL', 'nombre': 'Coahuila'},
    {'codigo': 'CM', 'nombre': 'Colima'},
    {'codigo': 'CS', 'nombre': 'Chiapas'},
    {'codigo': 'CH', 'nombre': 'Chihuahua'},
    {'codigo': 'DF', 'nombre': 'Ciudad de México'},
    {'codigo': 'DG', 'nombre': 'Durango'},
    {'codigo': 'GT', 'nombre': 'Guanajuato'},
    {'codigo': 'GR', 'nombre': 'Guerrero'},
    {'codigo': 'HG', 'nombre': 'Hidalgo'},
    {'codigo': 'JC', 'nombre': 'Jalisco'},
    {'codigo': 'MC', 'nombre': 'Estado de México'},
    {'codigo': 'MN', 'nombre': 'Michoacán'},
    {'codigo': 'MS', 'nombre': 'Morelos'},
    {'codigo': 'NT', 'nombre': 'Nayarit'},
    {'codigo': 'NL', 'nombre': 'Nuevo León'},
    {'codigo': 'OC', 'nombre': 'Oaxaca'},
    {'codigo': 'PL', 'nombre': 'Puebla'},
    {'codigo': 'QT', 'nombre': 'Querétaro'},
    {'codigo': 'QR', 'nombre': 'Quintana Roo'},
    {'codigo': 'SP', 'nombre': 'San Luis Potosí'},
    {'codigo': 'SL', 'nombre': 'Sinaloa'},
    {'codigo': 'SR', 'nombre': 'Sonora'},
    {'codigo': 'TC', 'nombre': 'Tabasco'},
    {'codigo': 'TS', 'nombre': 'Tamaulipas'},
    {'codigo': 'TL', 'nombre': 'Tlaxcala'},
    {'codigo': 'VZ', 'nombre': 'Veracruz'},
    {'codigo': 'YN', 'nombre': 'Yucatán'},
    {'codigo': 'ZS', 'nombre': 'Zacatecas'},
    {'codigo': 'NE', 'nombre': 'Nacido en el Extranjero'},
  ];

  // Catálogo dinámico de Estado Civil con formato Soltero(a), Casado(a)...
  List<Map<String, dynamic>> _estadosCivilesCatalogo = [
    {'codigo': 'SOL', 'descripcion': 'SOLTERO', 'nombre': 'Soltero(a)'},
    {'codigo': 'CAS', 'descripcion': 'CASADO', 'nombre': 'Casado(a)'},
    {'codigo': 'DIV', 'descripcion': 'DIVORCIADO', 'nombre': 'Divorciado(a)'},
    {'codigo': 'VIU', 'descripcion': 'VIUDO', 'nombre': 'Viudo(a)'},
    {'codigo': 'UNL', 'descripcion': 'UNION LIBRE', 'nombre': 'Unión libre'},
  ];

  @override
  void initState() {
    super.initState();
    _cargarCatalogos();
  }

  Future<void> _cargarCatalogos() async {
    try {
      final apiClient = ApiClient(storageService: SecureStorageService());
      final respEntidades = await apiClient.dio.get('${ApiConstants.catalogos}/entidades-federativas/todas');
      if (respEntidades.statusCode == 200 && respEntidades.data is List) {
        final List lista = respEntidades.data;
        if (lista.isNotEmpty && mounted) {
          setState(() {
            _entidadesFederativasCatalogo = lista.map<Map<String, String>>((item) {
              return {
                'codigo': (item['codigo'] ?? item['clave'] ?? '').toString(),
                'nombre': (item['nombre'] ?? item['descripcion'] ?? '').toString(),
              };
            }).where((e) => e['codigo']!.isNotEmpty).toList();
          });
        }
      }
    } catch (_) {}

    try {
      final apiClient = ApiClient(storageService: SecureStorageService());
      final respEstadosCiviles = await apiClient.dio.get('${ApiConstants.catalogos}/estados-civiles?size=50');
      if (respEstadosCiviles.statusCode == 200 && respEstadosCiviles.data != null && mounted) {
        final data = respEstadosCiviles.data;
        List items = [];
        if (data is Map && data.containsKey('datos') && data['datos'] != null) {
          final d = data['datos'];
          if (d is Map && d.containsKey('contenido') && d['contenido'] is List) {
            items = d['contenido'];
          } else if (d is List) {
            items = d;
          }
        } else if (data is List) {
          items = data;
        }

        if (items.isNotEmpty && mounted) {
          setState(() {
            _estadosCivilesCatalogo = items.map<Map<String, dynamic>>((item) {
              final rawDesc = (item['descripcion'] ?? item['codigo'] ?? '').toString();
              return {
                'id': item['id'],
                'codigo': item['codigo'],
                'descripcion': rawDesc,
                'nombre': _formatearEstadoCivil(rawDesc),
              };
            }).toList();
          });
        }
      }
    } catch (_) {}
  }

  static String _formatearEstadoCivil(String raw) {
    final r = raw.trim().toUpperCase();
    if (r.contains('SOLTER')) return 'Soltero(a)';
    if (r.contains('CASAD')) return 'Casado(a)';
    if (r.contains('DIVORC')) return 'Divorciado(a)';
    if (r.contains('VIUD')) return 'Viudo(a)';
    if (r.contains('UNION') || r.contains('UNIÓN') || r.contains('LIBRE') || r == 'UNL') return 'Unión libre';
    return raw;
  }

  // Contacto y Domicilio
  final _correoController = TextEditingController();
  final _telefonoController = TextEditingController();
  final _telefonoAltController = TextEditingController();
  final _calleController = TextEditingController();
  final _numeroExtController = TextEditingController();
  final _numeroIntController = TextEditingController();
  final _coloniaController = TextEditingController();
  final _municipioController = TextEditingController();
  final _estadoController = TextEditingController();
  final _cpController = TextEditingController();
  final _paisController = TextEditingController(text: 'Mexico');

  // Laboral y Financiero
  String _ocupacionSeleccionada = 'Analista Financiero';
  final _empresaController = TextEditingController();
  final _ingresoController = TextEditingController();
  final _saldoInicialController = TextEditingController(text: '1500.00');
  String _tipoCuenta = 'DEBITO';

  // Seguridad
  final _passwordController = TextEditingController(text: 'Segura123#');
  bool _obscurePassword = true;

  @override
  void dispose() {
    _nombreController.dispose();
    _apellidoPaternoController.dispose();
    _apellidoMaternoController.dispose();
    _fechaNacimientoController.dispose();
    _curpController.dispose();
    _rfcController.dispose();
    _correoController.dispose();
    _telefonoController.dispose();
    _telefonoAltController.dispose();
    _numeroIntController.dispose();
    _calleController.dispose();
    _numeroExtController.dispose();
    _coloniaController.dispose();
    _municipioController.dispose();
    _estadoController.dispose();
    _cpController.dispose();
    _paisController.dispose();
    _empresaController.dispose();
    _ingresoController.dispose();
    _saldoInicialController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _limpiarErrorServidor(String campo) {
    if (_serverErrors.containsKey(campo)) {
      setState(() => _serverErrors.remove(campo));
    }
  }

  String _resolverMunicipioSepomex(int cpNum, String prefijo) {
    // Catálogo detallado SEPOMEX para todos los municipios de Guanajuato (36000 - 38999)
    if (cpNum >= 36000 && cpNum <= 38999) {
      if (cpNum <= 36099 || (cpNum >= 36200 && cpNum <= 36269)) return 'Guanajuato';
      if ((cpNum >= 36100 && cpNum <= 36199) || (cpNum >= 36270 && cpNum <= 36299)) return 'Silao de la Victoria';
      if (cpNum >= 36300 && cpNum <= 36399) return 'San Francisco del Rincon';
      if (cpNum >= 36400 && cpNum <= 36469) return 'Purisima del Rincon';
      if (cpNum >= 36470 && cpNum <= 36499) return 'Manuel Doblado';
      if ((cpNum >= 36500 && cpNum <= 36699) || (cpNum >= 36800 && cpNum <= 36849)) return 'Irapuato';
      if ((cpNum >= 36700 && cpNum <= 36799) || (cpNum >= 36850 && cpNum <= 36889)) return 'Salamanca';
      if (cpNum >= 36890 && cpNum <= 36899) return 'Pueblo Nuevo';
      if (cpNum >= 36900 && cpNum <= 36959) return 'Penjamo';
      if (cpNum >= 36960 && cpNum <= 36969) return 'Cueramaro';
      if (cpNum >= 36970 && cpNum <= 36989) return 'Abasolo';
      if (cpNum >= 36990 && cpNum <= 36999) return 'Huanimaro';
      if (cpNum >= 37000 && cpNum <= 37599) return 'Leon';
      if (cpNum >= 37600 && cpNum <= 37629) return 'San Felipe';
      if (cpNum >= 37630 && cpNum <= 37699) return 'Ocampo';
      if (cpNum >= 37700 && cpNum <= 37799) return 'San Miguel de Allende';
      if (cpNum >= 37800 && cpNum <= 37849) return 'Dolores Hidalgo Cuna de la Independencia Nacional';
      if (cpNum >= 37850 && cpNum <= 37899) return 'San Diego de la Unión';
      if (cpNum >= 37900 && cpNum <= 37919) return 'San Luis de la Paz';
      if (cpNum >= 37920 && cpNum <= 37929) return 'Victoria';
      if (cpNum >= 37930 && cpNum <= 37939) return 'Xichu';
      if (cpNum >= 37940 && cpNum <= 37949) return 'Atarjea';
      if (cpNum >= 37950 && cpNum <= 37959) return 'Santa Catarina';
      if (cpNum >= 37960 && cpNum <= 37969) return 'Doctor Mora';
      if (cpNum >= 37970 && cpNum <= 37979) return 'Tierra Blanca';
      if (cpNum >= 37980 && cpNum <= 37999) return 'San Jose Iturbide';
      if (cpNum >= 38000 && cpNum <= 38159) return 'Celaya';
      if (cpNum >= 38160 && cpNum <= 38199) return 'Apaseo el Grande';
      if (cpNum >= 38200 && cpNum <= 38259) return 'Comonfort';
      if (cpNum >= 38260 && cpNum <= 38299) return 'Villagran';
      if (cpNum >= 38300 && cpNum <= 38399) return 'Cortazar';
      if (cpNum >= 38400 && cpNum <= 38469) return 'Valle de Santiago';
      if (cpNum >= 38470 && cpNum <= 38499) return 'Jaral del Progreso';
      if (cpNum >= 38500 && cpNum <= 38559) return 'Apaseo el Alto';
      if (cpNum >= 38560 && cpNum <= 38599) return 'Jerecuaro';
      if (cpNum >= 38600 && cpNum <= 38699) return 'Acambaro';
      if (cpNum >= 38700 && cpNum <= 38759) return 'Tarimoro';
      if (cpNum >= 38760 && cpNum <= 38779) return 'Coroneo';
      if (cpNum >= 38780 && cpNum <= 38799) return 'Tarandacuao';
      if (cpNum >= 38800 && cpNum <= 38889) return 'Moroleon';
      if (cpNum >= 38890 && cpNum <= 38899) return 'Uriangato';
      if (cpNum >= 38900 && cpNum <= 38969) return 'Salvatierra';
      if (cpNum >= 38970 && cpNum <= 38979) return 'Santiago Maravatio';
      return 'Yuriria';
    }

    // Alcaldías CDMX y municipios cabecera SEPOMEX por prefijo en todo México
    switch (prefijo) {
      case '01': return 'Alvaro Obregon';
      case '02': return 'Azcapotzalco';
      case '03': return 'Benito Juarez';
      case '04': return 'Coyoacan';
      case '05': return 'Cuajimalpa de Morelos';
      case '06': return 'Cuauhtemoc';
      case '07': return 'Gustavo A Madero';
      case '08': return 'Iztacalco';
      case '09': return 'Iztapalapa';
      case '10': return 'La Magdalena Contreras';
      case '11': return 'Miguel Hidalgo';
      case '12': return 'Milpa Alta';
      case '13': return 'Tlahuac';
      case '14': return 'Tlalpan';
      case '15': return 'Venustiano Carranza';
      case '16': return 'Xochimilco';
      case '20': return 'Aguascalientes';
      case '21': return 'Mexicali';
      case '22': return 'Tijuana';
      case '23': return 'La Paz';
      case '24': return 'Campeche';
      case '25': return 'Saltillo';
      case '26': return 'Piedras Negras';
      case '27': return 'Torreon';
      case '28': return 'Colima';
      case '29': return 'Tuxtla Gutierrez';
      case '30': return 'Tapachula';
      case '31': return 'Chihuahua';
      case '32': return 'Juarez';
      case '33': return 'Delicias';
      case '34': return 'Durango';
      case '35': return 'Gomez Palacio';
      case '39': return 'Chilpancingo de los Bravo';
      case '40': case '41': return 'Acapulco de Juarez';
      case '42': case '43': return 'Pachuca de Soto';
      case '44': return 'Guadalajara';
      case '45': return 'Zapopan';
      case '46': case '47': return 'Tepatitlan de Morelos';
      case '48': case '49': return 'Puerto Vallarta';
      case '50': case '51': case '52': return 'Toluca';
      case '53': return 'Naucalpan de Juarez';
      case '54': return 'Tlalnepantla de Baz';
      case '55': return 'Ecatepec de Morelos';
      case '56': case '57': return 'Nezahualcoyotl';
      case '58': return 'Morelia';
      case '59': return 'Zamora';
      case '60': case '61': return 'Uruapan';
      case '62': return 'Cuernavaca';
      case '63': return 'Tepic';
      case '64': return 'Monterrey';
      case '65': return 'Apodaca';
      case '66': return 'San Pedro Garza Garcia';
      case '67': return 'Guadalupe';
      case '68': case '69': case '70': case '71': return 'Oaxaca de Juarez';
      case '72': case '73': case '74': return 'Puebla';
      case '75': return 'Tehuacan';
      case '76': return cpNum >= 76800 ? 'San Juan del Rio' : 'Queretaro';
      case '77': return cpNum < 77500 ? 'Othon P Blanco' : 'Benito Juarez';
      case '78': return 'San Luis Potosi';
      case '79': return 'Ciudad Valles';
      case '80': case '81': return 'Culiacan';
      case '82': return 'Mazatlan';
      case '83': return 'Hermosillo';
      case '84': return 'Nogales';
      case '85': return 'Cajeme';
      case '86': return 'Centro';
      case '87': return 'Victoria';
      case '88': return 'Reynosa';
      case '89': return 'Tampico';
      case '90': return 'Tlaxcala';
      case '91': return 'Xalapa';
      case '92': case '93': return 'Tuxpan';
      case '94': return 'Cordoba';
      case '95': case '96': return 'Coatzacoalcos';
      case '97': return 'Merida';
      case '98': return 'Zacatecas';
      case '99': return 'Fresnillo';
      default: return 'Guanajuato';
    }
  }

  Future<void> _autollenarPorCodigoPostal(String cp) async {
    final t = cp.trim();
    if (t.length == 5) {
      final cpNum = int.tryParse(t);
      if (cpNum == null) return;

      final prefijo = t.substring(0, 2);
      String nuevoEstado = 'Ciudad de Mexico';

      switch (prefijo) {
        case '01': case '02': case '03': case '04': case '05': case '06':
        case '07': case '08': case '09': case '10': case '11': case '12':
        case '13': case '14': case '15': case '16':
          nuevoEstado = 'Ciudad de Mexico'; break;
        case '20': nuevoEstado = 'Aguascalientes'; break;
        case '21': case '22': nuevoEstado = 'Baja California'; break;
        case '23': nuevoEstado = 'Baja California Sur'; break;
        case '24': nuevoEstado = 'Campeche'; break;
        case '25': case '26': case '27': nuevoEstado = 'Coahuila'; break;
        case '28': nuevoEstado = 'Colima'; break;
        case '29': case '30': nuevoEstado = 'Chiapas'; break;
        case '31': case '32': case '33': nuevoEstado = 'Chihuahua'; break;
        case '34': case '35': nuevoEstado = 'Durango'; break;
        case '36': case '37': case '38': nuevoEstado = 'Guanajuato'; break;
        case '39': case '40': case '41': nuevoEstado = 'Guerrero'; break;
        case '42': case '43': nuevoEstado = 'Hidalgo'; break;
        case '44': case '45': case '46': case '47': case '48': case '49':
          nuevoEstado = 'Jalisco'; break;
        case '50': case '51': case '52': case '53': case '54': case '55':
        case '56': case '57':
          nuevoEstado = 'Estado de Mexico'; break;
        case '58': case '59': case '60': case '61': nuevoEstado = 'Michoacan'; break;
        case '62': nuevoEstado = 'Morelos'; break;
        case '63': nuevoEstado = 'Nayarit'; break;
        case '64': case '65': case '66': case '67': nuevoEstado = 'Nuevo Leon'; break;
        case '68': case '69': case '70': case '71': nuevoEstado = 'Oaxaca'; break;
        case '72': case '73': case '74': case '75': nuevoEstado = 'Puebla'; break;
        case '76': nuevoEstado = 'Queretaro'; break;
        case '77': nuevoEstado = 'Quintana Roo'; break;
        case '78': case '79': nuevoEstado = 'San Luis Potosi'; break;
        case '80': case '81': case '82': nuevoEstado = 'Sinaloa'; break;
        case '83': case '84': case '85': nuevoEstado = 'Sonora'; break;
        case '86': nuevoEstado = 'Tabasco'; break;
        case '87': case '88': case '89': nuevoEstado = 'Tamaulipas'; break;
        case '90': nuevoEstado = 'Tlaxcala'; break;
        case '91': case '92': case '93': case '94': case '95': case '96':
          nuevoEstado = 'Veracruz'; break;
        case '97': nuevoEstado = 'Yucatan'; break;
        case '98': case '99': nuevoEstado = 'Zacatecas'; break;
      }

      final nuevoMunicipio = _resolverMunicipioSepomex(cpNum, prefijo);

      setState(() {
        _estadoController.text = nuevoEstado;
        _municipioController.text = nuevoMunicipio;
      });
      _limpiarErrorServidor('estado');
      _limpiarErrorServidor('municipio');

      // Refinamiento asíncrono con catálogo oficial SEPOMEX para municipio y colonia exactos
      try {
        final dioCp = Dio(BaseOptions(
          connectTimeout: const Duration(seconds: 5),
          receiveTimeout: const Duration(seconds: 8),
        ));
        final resp = await dioCp.get('https://sepomex.kurenn.dev/api/v1/zip_codes', queryParameters: {'zip_code': t});
        if (!mounted || _cpController.text.trim() != t) return;
        final data = resp.data;
        if (data is Map && data['zip_codes'] is List && (data['zip_codes'] as List).isNotEmpty) {
          final item = (data['zip_codes'] as List).first;
          if (item is Map) {
            String limpiarSoloLetras(String raw) => raw
                .replaceAll(RegExp(r'[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]'), ' ')
                .replaceAll(RegExp(r'\s+'), ' ')
                .trim();

            final estApi = limpiarSoloLetras((item['d_estado'] ?? '').toString());
            final munApi = limpiarSoloLetras((item['d_mnpio'] ?? '').toString());
            final colApi = limpiarSoloLetras((item['d_asenta'] ?? '').toString());

            setState(() {
              if (_vEstado(estApi) == null) {
                _estadoController.text = estApi;
              }
              if (_vMunicipio(munApi) == null) {
                _municipioController.text = munApi;
              }
              if (_coloniaController.text.trim().isEmpty && _vColonia(colApi) == null) {
                _coloniaController.text = colApi;
                _limpiarErrorServidor('colonia');
              }
            });
          }
        }
      } catch (_) {
        // Si no hay respuesta externa, se conserva la resolución local SEPOMEX inmediata
      }
    }
  }

  // --- ESPECIFICACIONES OFICIALES RENAPO Y SAT ---

  static const Set<String> _palabrasInconvenientesCurp = {
    'BACA', 'BAIL', 'BAKA', 'BUEI', 'BUEY', 'CACA', 'CACO', 'CAGA', 'CAGO', 'CAKA', 'CAKO',
    'COGE', 'COGI', 'COJA', 'COJE', 'COJI', 'COJO', 'COLA', 'CULO', 'FALO', 'FETO', 'GETA',
    'GUEI', 'GUEY', 'JETA', 'JOTO', 'KACA', 'KACO', 'KAGA', 'KAGO', 'KAKA', 'KAKO', 'KOGE',
    'KOGI', 'KOJA', 'KOJE', 'KOJI', 'KOJO', 'KOLA', 'KULO', 'LILO', 'LOCA', 'LOCO', 'LOKA',
    'LOKO', 'MAME', 'MAMO', 'MEAR', 'MEAS', 'MEON', 'MIAR', 'MION', 'MOCO', 'MOKO', 'MULA',
    'MULO', 'NACA', 'NACO', 'PEDA', 'PEDO', 'PENE', 'PIPI', 'PITO', 'POPO', 'PUTA', 'PUTO',
    'QULO', 'RATA', 'ROBA', 'ROBE', 'ROBO', 'RUIN', 'SENO', 'TETA', 'VACA', 'VAGA', 'VAGO',
    'VAKA', 'VUEI', 'VUEY', 'WUEI', 'WUEY'
  };

  static const Set<String> _preposicionesOmitir = {
    'DA', 'DAS', 'DE', 'DEL', 'DER', 'DI', 'DIE', 'DD', 'EL', 'LA', 'LOS', 'LAS', 'LE', 'LES', 'MAC', 'MC', 'VAN', 'VON', 'Y'
  };

  static String _normalizarTexto(String s) {
    var res = s.trim().toUpperCase();
    res = res.replaceAll('Á', 'A').replaceAll('É', 'E').replaceAll('Í', 'I').replaceAll('Ó', 'O').replaceAll('Ú', 'U').replaceAll('Ü', 'U');
    return res;
  }

  static String _obtenerPalabraSignificativa(String compuesto) {
    final partes = _normalizarTexto(compuesto).split(RegExp(r'\s+'));
    for (final p in partes) {
      if (!_preposicionesOmitir.contains(p) && p.isNotEmpty) {
        return p;
      }
    }
    return partes.isNotEmpty ? partes.first : '';
  }

  static String _obtenerNombreParaCurp(String nombreCompleto) {
    final partes = _normalizarTexto(nombreCompleto).split(RegExp(r'\s+'));
    final validas = partes.where((p) => !_preposicionesOmitir.contains(p) && p.isNotEmpty).toList();
    if (validas.isEmpty) return '';
    if (validas.length > 1) {
      final primer = validas.first;
      if (primer == 'MARIA' || primer == 'MA' || primer == 'MA.' || primer == 'JOSE' || primer == 'J' || primer == 'J.') {
        return validas[1];
      }
    }
    return validas.first;
  }

  static String _primeraVocalInterna(String s) {
    final norm = _normalizarTexto(s);
    const vocales = 'AEIOU';
    for (int i = 1; i < norm.length; i++) {
      if (vocales.contains(norm[i])) return norm[i];
    }
    return 'X';
  }

  static String _primeraConsonanteInterna(String s) {
    final norm = _normalizarTexto(s);
    const consonantes = 'BCDFGHJKLMNPQRSTVWXYZ';
    for (int i = 1; i < norm.length; i++) {
      if (consonantes.contains(norm[i])) return norm[i];
    }
    return 'X';
  }

  static String _calcularDigitoVerificadorRenapo(String curp17) {
    const diccionario = '0123456789ABCDEFGHIJKLMNÑOPQRSTUVWXYZ';
    int suma = 0;
    for (int i = 0; i < 17; i++) {
      final char = curp17[i];
      final pos = diccionario.indexOf(char);
      final factor = 18 - i;
      suma += (pos >= 0 ? pos : 0) * factor;
    }
    final residuo = suma % 10;
    final digito = (10 - residuo) % 10;
    return digito.toString();
  }

  void _generarCurpYRfcAutomatico({bool forzar = false}) {
    final nombreCompleto = _nombreController.text.trim();
    final apPaterno = _apellidoPaternoController.text.trim();
    final apMaterno = _apellidoMaternoController.text.trim();
    final fecha = _fechaNacimientoController.text.trim();

    if (nombreCompleto.isEmpty || apPaterno.isEmpty || apMaterno.isEmpty || fecha.isEmpty) {
      return;
    }

    final pSignificativo = _obtenerPalabraSignificativa(apPaterno);
    final mSignificativo = _obtenerPalabraSignificativa(apMaterno);
    final nSignificativo = _obtenerNombreParaCurp(nombreCompleto);

    if (pSignificativo.isEmpty || nSignificativo.isEmpty) return;

    // 1. Primeras 4 letras
    final c1 = pSignificativo[0];
    final c2 = _primeraVocalInterna(pSignificativo);
    final c3 = mSignificativo.isNotEmpty ? mSignificativo[0] : 'X';
    final c4 = nSignificativo[0];

    var cuatroLetras = '$c1$c2$c3$c4';
    if (_palabrasInconvenientesCurp.contains(cuatroLetras)) {
      cuatroLetras = '${cuatroLetras[0]}X${cuatroLetras.substring(2)}';
    }

    // 2. Fecha AAMMDD
    final partesFecha = fecha.split('-');
    if (partesFecha.length != 3 || partesFecha[0].length != 4) return;
    final anioStr = partesFecha[0];
    final mesStr = partesFecha[1].padLeft(2, '0');
    final diaStr = partesFecha[2].padLeft(2, '0');
    final aammdd = '${anioStr.substring(2)}$mesStr$diaStr';
    final anioNum = int.tryParse(anioStr) ?? 1995;

    // 3. Sexo (M / H)
    final charSexo = (_sexo == 'FEMENINO') ? 'M' : 'H';

    // 4. Entidad Federativa de Nacimiento (2 dígitos oficiales)
    final cveEntidad = _entidadNacimiento;

    // 5. Consonantes internas
    final con1 = _primeraConsonanteInterna(pSignificativo);
    final con2 = mSignificativo.isNotEmpty ? _primeraConsonanteInterna(mSignificativo) : 'X';
    final con3 = _primeraConsonanteInterna(nSignificativo);

    // 6. Diferenciador de siglo
    final difSiglo = (anioNum >= 2000) ? 'A' : '0';

    // 7. Base CURP de 17 caracteres y Dígito Verificador oficial RENAPO
    final curp17 = '$cuatroLetras$aammdd$charSexo$cveEntidad$con1$con2$con3$difSiglo';
    final digitoVerificador = _calcularDigitoVerificadorRenapo(curp17);
    final curpOficial = '$curp17$digitoVerificador';

    // RFC: 10 caracteres base calculados según SAT + homoclave (3 caracteres)
    String homoclaveActual = 'AA1';
    if (_rfcController.text.trim().length == 13) {
      homoclaveActual = _rfcController.text.trim().substring(10).toUpperCase();
    }
    final rfcOficial = '$cuatroLetras$aammdd$homoclaveActual';

    setState(() {
      if (forzar || !_curpEditadoManualmente || _curpController.text.isEmpty) {
        _curpController.text = curpOficial;
      }
      if (forzar || !_rfcEditadoManualmente || _rfcController.text.isEmpty) {
        _rfcController.text = rfcOficial;
      }
    });
  }

  Future<void> _seleccionarFechaNacimiento() async {
    final fechaActual = DateTime.tryParse(_fechaNacimientoController.text.trim()) ?? DateTime(1995, 5, 15);
    final nuevaFecha = await showDatePicker(
      context: context,
      initialDate: fechaActual,
      firstDate: DateTime(1920),
      lastDate: DateTime.now(),
      locale: const Locale('es', 'MX'),
    );

    if (nuevaFecha != null) {
      final y = nuevaFecha.year.toString().padLeft(4, '0');
      final m = nuevaFecha.month.toString().padLeft(2, '0');
      final d = nuevaFecha.day.toString().padLeft(2, '0');
      setState(() {
        _fechaNacimientoController.text = '$y-$m-$d';
      });
      _limpiarErrorServidor('fechaNacimiento');
      _generarCurpYRfcAutomatico(forzar: true);
    }
  }

  // --- REGLAS DE VALIDACIÓN ALINEADAS CON EL BACKEND ---

  String? _validarNombreCompleto(String? val) {
    if (val == null || val.isEmpty) {
      return 'El nombre completo es obligatorio y no debe estar vacío';
    }
    if (val.startsWith(' ')) {
      return 'El nombre completo no puede iniciar con espacios';
    }
    if (val.endsWith(' ')) {
      return 'El nombre completo no puede terminar con espacios';
    }
    if (val.contains('  ')) {
      return 'No se permiten múltiples espacios internos consecutivos';
    }
    final trimmed = val.trim();
    if (trimmed.length < 2) {
      return 'El nombre completo debe tener al menos 2 caracteres reales';
    }
    if (trimmed.length > 100) {
      return 'El nombre completo no puede exceder 100 caracteres';
    }
    if (!RegExp(r'^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+$').hasMatch(val)) {
      return 'El nombre completo solo puede aceptar letras y espacios';
    }
    final palabras = trimmed.split(RegExp(r'\s+'));
    for (final p in palabras) {
      if (_palabrasProhibidas.contains(p.toUpperCase())) {
        return 'El nombre completo no puede ser una palabra reservada o de programación';
      }
    }
    return null;
  }

  String? _validarNombreOApellido(String? val, String campo, {bool obligatorio = true}) {
    if (val == null || val.isEmpty) {
      return obligatorio ? 'El $campo es obligatorio y no debe estar vacío' : null;
    }
    if (val.startsWith(' ')) {
      return 'El $campo no puede iniciar con espacios';
    }
    if (val.endsWith(' ')) {
      return 'El $campo no puede terminar con espacios';
    }
    if (val.contains('  ')) {
      return 'No se permiten múltiples espacios internos consecutivos';
    }
    final trimmed = val.trim();
    if (trimmed.length < 2) {
      return 'El $campo debe tener al menos 2 caracteres reales';
    }
    if (trimmed.length > 50) {
      return 'El $campo no puede exceder 50 caracteres';
    }
    if (!RegExp(r'^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+$').hasMatch(val)) {
      return 'El $campo solo puede aceptar letras y espacios';
    }
    final palabras = trimmed.split(RegExp(r'\s+'));
    for (final p in palabras) {
      if (_palabrasProhibidas.contains(p.toUpperCase())) {
        return 'El $campo no puede ser una palabra reservada o de programación';
      }
    }
    return null;
  }

  String? _validarFechaNacimiento(String? val) {
    if (val == null || val.trim().isEmpty) {
      return 'La fecha de nacimiento es obligatoria';
    }
    final fecha = DateTime.tryParse(val.trim());
    if (fecha == null) {
      return 'Formato inválido (debe ser AAAA-MM-DD)';
    }
    final hoy = DateTime.now();
    if (fecha.isAfter(hoy)) {
      return 'La fecha de nacimiento no puede ser una fecha futura';
    }
    int edad = hoy.year - fecha.year;
    if (hoy.month < fecha.month || (hoy.month == fecha.month && hoy.day < fecha.day)) {
      edad--;
    }
    if (edad < 18) {
      return 'El cliente debe ser mayor de edad (18 años o más). Edad: $edad';
    }
    if (edad > 100) {
      return 'La fecha de nacimiento no debe exceder los 100 años';
    }
    return null;
  }

  String? _validarCurp(String? val) {
    if (val == null || val.trim().isEmpty) {
      return 'El CURP es obligatorio y no debe estar vacío';
    }
    final curp = val.trim().toUpperCase();
    if (curp.length != 18) {
      return 'El CURP debe contener exactamente 18 caracteres';
    }
    if (!RegExp(r'^[A-Z]{4}\d{6}[HM][A-Z]{5}[A-Z0-9]\d$').hasMatch(curp)) {
      return 'El CURP no cumple con el formato oficial (ej. AAAA000000HAAAAA00)';
    }

    // Validación cruzada con Sexo (posición 11, índice 10)
    final charSexo = curp[10];
    final esperadoSexo = (_sexo == 'FEMENINO') ? 'M' : 'H';
    if (charSexo != esperadoSexo) {
      return 'La posición 11 del CURP ($charSexo) no coincide con el sexo ($_sexo: $esperadoSexo)';
    }

    // Validación cruzada con Fecha de Nacimiento (posiciones 5 a 10)
    final f = _fechaNacimientoController.text.trim();
    final partes = f.split('-');
    if (partes.length == 3 && partes[0].length == 4) {
      final aammdd = '${partes[0].substring(2)}${partes[1]}${partes[2]}';
      if (curp.substring(4, 10) != aammdd) {
        return 'Los dígitos 5-10 del CURP (${curp.substring(4, 10)}) no coinciden con la fecha ($aammdd)';
      }
    }

    // Validación cruzada con Entidad Federativa de Nacimiento (posiciones 12 a 13)
    if (curp.length >= 13 && curp.substring(11, 13) != _entidadNacimiento) {
      return 'Los caracteres 12-13 del CURP (${curp.substring(11, 13)}) no coinciden con la entidad de nacimiento ($_entidadNacimiento)';
    }

    // Validación cruzada con Apellidos y Nombre según especificación RENAPO
    final ap = _apellidoPaternoController.text.trim().toUpperCase();
    final am = _apellidoMaternoController.text.trim().toUpperCase();
    final nom = _nombreController.text.trim().toUpperCase();
    if (ap.isNotEmpty && am.isNotEmpty && nom.isNotEmpty) {
      final pSig = _obtenerPalabraSignificativa(ap);
      final mSig = _obtenerPalabraSignificativa(am);
      final nSig = _obtenerNombreParaCurp(nom);
      if (pSig.isNotEmpty && nSig.isNotEmpty) {
        // En palabras altisonantes se sustituye la 2da letra con 'X'
        if (curp[0] != pSig[0] || (mSig.isNotEmpty && curp[2] != mSig[0]) || curp[3] != nSig[0]) {
          return 'Las iniciales del CURP (${curp.substring(0, 4)}) no coinciden con los apellidos y nombre ingresados';
        }
      }
    }

    return null;
  }

  String? _validarRfc(String? val) {
    if (val == null || val.trim().isEmpty) {
      return 'El RFC es obligatorio y no debe estar vacío';
    }
    final rfc = val.trim().toUpperCase();
    if (rfc.length != 12 && rfc.length != 13) {
      return 'El RFC debe tener exactamente 12 o 13 caracteres';
    }
    if (!RegExp(r'^[A-ZÑ&]{4}\d{6}[A-Z0-9]{2,3}$').hasMatch(rfc)) {
      return 'El RFC no cumple con el formato oficial (ejemplo: AAAA000000AAA)';
    }

    // Validación cruzada con CURP (para persona física de 13 caracteres)
    if (rfc.length == 13 && _curpController.text.trim().length >= 10) {
      final prefijoCurp = _curpController.text.trim().toUpperCase().substring(0, 10);
      final prefijoRfc = rfc.substring(0, 10);
      if (prefijoRfc != prefijoCurp) {
        return 'Los primeros 10 caracteres del RFC ($prefijoRfc) deben coincidir con el CURP ($prefijoCurp)';
      }
    }

    return null;
  }

  String? _validarCorreo(String? val) {
    if (val == null || val.trim().isEmpty) {
      return 'El correo electrónico es obligatorio y no debe estar vacío';
    }
    final email = val.trim();
    if (email.length > 100) {
      return 'El correo electrónico no puede exceder 100 caracteres';
    }
    if (!RegExp(r'^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$').hasMatch(email)) {
      return 'El formato de correo electrónico es inválido';
    }
    return null;
  }

  String? _validarTelefono(String? val) {
    if (val == null || val.trim().isEmpty) {
      return 'El teléfono móvil es obligatorio y no debe estar vacío';
    }
    final tel = val.trim();
    if (!RegExp(r'^\d{10}$').hasMatch(tel)) {
      return 'El teléfono móvil solo acepta 10 dígitos numéricos (sin guiones ni espacios)';
    }
    return null;
  }

  String? _validarPassword(String? val) {
    if (val == null || val.isEmpty) {
      return 'La contraseña es obligatoria y no debe estar vacía';
    }
    if (val.length < 8 || val.length > 50) {
      return 'La contraseña debe tener entre 8 y 50 caracteres';
    }
    if (val.contains(' ')) {
      return 'La contraseña no puede contener espacios en blanco';
    }
    if (!RegExp(r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d\s]).{8,50}$').hasMatch(val)) {
      return 'Debe contener al menos 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial';
    }
    return null;
  }

  String? _validarSaldoInicial(String? val) {
    if (val == null || val.trim().isEmpty) return null;
    final parsed = double.tryParse(val.trim());
    if (parsed == null) {
      return 'Debe ser un valor numérico decimal válido';
    }
    if (parsed < 0) {
      return 'El saldo inicial no puede ser negativo';
    }
    if (!RegExp(r'^\d+(\.\d{1,2})?$').hasMatch(val.trim())) {
      return 'Máximo 2 decimales permitidos (ej. 1500.50)';
    }
    if (parsed > 999999999999.99) {
      return 'El saldo inicial excede el límite máximo permitido';
    }
    return null;
  }

  String? _validarIngreso(String? val) {
    if (val == null || val.trim().isEmpty) {
      return 'El ingreso mensual es obligatorio';
    }
    final parsed = double.tryParse(val.trim());
    if (parsed == null || parsed <= 0) {
      return 'El ingreso mensual debe ser mayor a cero';
    }
    if (!RegExp(r'^\d+(\.\d{1,2})?$').hasMatch(val.trim())) {
      return 'Máximo 2 decimales permitidos (ej. 25000.00)';
    }
    if (parsed > 9999999999.99) {
      return 'El ingreso mensual excede el límite máximo permitido';
    }
    return null;
  }

  // Texto obligatorio con longitud máxima y regex opcional (alineado con @NotBlank/@Size/@Pattern)
  String? _validarTexto(String? val, String campo, int max, {RegExp? regex, String? msgRegex, bool obligatorio = true, int min = 1}) {
    final v = val ?? '';
    if (v.trim().isEmpty) {
      return obligatorio ? 'El campo $campo es obligatorio y no debe estar vacío' : null;
    }
    if (v.trim().length < min) return 'El campo $campo debe tener al menos $min caracteres';
    if (v.length > max) return 'El campo $campo no puede exceder $max caracteres';
    if (regex != null && !regex.hasMatch(v.trim())) {
      return msgRegex ?? 'El campo $campo tiene un formato inválido';
    }
    return null;
  }

  static final RegExp _reLetras = RegExp(r"^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ .'-]{2,100}$");
  static final RegExp _reColonia = RegExp(r"^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ .'-]{2,100}$");

  // Mismas reglas estrictas que Nombre para campos alfabéticos de domicilio
  String? _validarTextoDomicilioLetras(String? val, String campo, {required int min, required int max}) {
    if (val == null || val.isEmpty) {
      return 'El campo $campo es obligatorio y no debe estar vacío';
    }
    if (val.startsWith(' ')) {
      return 'El campo $campo no puede iniciar con espacios';
    }
    if (val.endsWith(' ')) {
      return 'El campo $campo no puede terminar con espacios';
    }
    if (val.contains('  ')) {
      return 'No se permiten múltiples espacios internos consecutivos';
    }
    final trimmed = val.trim();
    if (trimmed.length < min) {
      return 'El campo $campo debe tener al menos $min letras';
    }
    if (val.length > max) {
      return 'El campo $campo no puede exceder $max caracteres';
    }
    if (!RegExp(r'^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+$').hasMatch(val)) {
      return 'El campo $campo solo puede aceptar letras y espacios';
    }
    final palabras = trimmed.split(RegExp(r'\s+'));
    for (final p in palabras) {
      if (_palabrasProhibidas.contains(p.toUpperCase())) {
        return 'El campo $campo no puede ser una palabra reservada o de programación';
      }
    }
    return null;
  }

  String? _vCalle(String? v) => _validarTextoDomicilioLetras(v, 'calle', min: 3, max: 60);
  String? _vColonia(String? v) => _validarTextoDomicilioLetras(v, 'colonia', min: 3, max: 60);
  String? _vMunicipio(String? v) => _validarTextoDomicilioLetras(v, 'municipio o alcaldía', min: 3, max: 60);
  String? _vEstado(String? v) => _validarTextoDomicilioLetras(v, 'estado', min: 4, max: 40);

  String? _vNumExt(String? v) {
    final t = v ?? '';
    if (t.trim().isEmpty) return 'El número exterior es obligatorio y no debe estar vacío';
    if (!RegExp(r'^\d{1,6}$').hasMatch(t)) {
      return 'El número exterior solo acepta números (de 1 a 6 dígitos)';
    }
    return null;
  }

  String? _vNumInt(String? v) {
    final t = v ?? '';
    if (t.isEmpty) return null;
    if (!RegExp(r'^\d{1,6}$').hasMatch(t)) {
      return 'El número interior solo acepta números (máximo 6 dígitos)';
    }
    return null;
  }
  String? _vPais(String? v) => _validarTexto(v, 'país', 50,
      regex: _reLetras, msgRegex: 'El país solo puede contener letras y espacios válidos');
  String? _vCp(String? v) {
    final t = (v ?? '').trim();
    if (t.isEmpty) return 'El código postal es obligatorio y no debe estar vacío';
    if (!RegExp(r'^\d{5}$').hasMatch(t)) return 'El código postal debe contener exactamente 5 dígitos numéricos';
    return null;
  }

  String? _vEmpresa(String? v) => _validarTexto(v, 'empresa', 100);
  String? _vTelAlt(String? v) {
    final t = (v ?? '').trim();
    if (t.isEmpty) return null;
    if (!RegExp(r'^\d{10,15}$').hasMatch(t)) {
      return 'El teléfono alternativo solo acepta dígitos (entre 10 y 15)';
    }
    return null;
  }

  bool get _isFormularioValido {
    return _validarNombreCompleto(_nombreController.text) == null &&
        _validarNombreOApellido(_apellidoPaternoController.text, 'apellido paterno') == null &&
        _validarNombreOApellido(_apellidoMaternoController.text, 'apellido materno') == null &&
        _validarFechaNacimiento(_fechaNacimientoController.text) == null &&
        _validarCurp(_curpController.text) == null &&
        _validarRfc(_rfcController.text) == null &&
        _entidadNacimiento.isNotEmpty &&
        _validarCorreo(_correoController.text) == null &&
        _validarTelefono(_telefonoController.text) == null &&
        _validarPassword(_passwordController.text) == null &&
        _validarIngreso(_ingresoController.text) == null &&
        _validarSaldoInicial(_saldoInicialController.text) == null &&
        _vTelAlt(_telefonoAltController.text) == null &&
        _vCalle(_calleController.text) == null &&
        _vNumExt(_numeroExtController.text) == null &&
        _vNumInt(_numeroIntController.text) == null &&
        _vColonia(_coloniaController.text) == null &&
        _vMunicipio(_municipioController.text) == null &&
        _vEstado(_estadoController.text) == null &&
        _vCp(_cpController.text) == null &&
        _vPais(_paisController.text) == null &&
        _ocupacionSeleccionada.isNotEmpty &&
        _vEmpresa(_empresaController.text) == null;
  }

  Future<void> _submitRegistro() async {
    // Limpiar errores del servidor previos
    setState(() => _serverErrors = {});

    if (!_formKey.currentState!.validate()) return;

    final authProvider = context.read<AuthProvider>();

    final datos = {
      'nombre': _nombreController.text.trim(),
      'segundoNombre': null,
      'apellidoPaterno': _apellidoPaternoController.text.trim(),
      'apellidoMaterno': _apellidoMaternoController.text.trim(),
      'fechaNacimiento': _fechaNacimientoController.text.trim(),
      'curp': _curpController.text.trim().toUpperCase(),
      'rfc': _rfcController.text.trim().toUpperCase(),
      'sexo': _sexo,
      'entidadNacimiento': _entidadNacimiento,
      'nacionalidad': _nacionalidad,
      'estadoCivil': _estadoCivil,
      'correoElectronico': _correoController.text.trim().toLowerCase(),
      'telefonoMovil': _telefonoController.text.trim(),
      'telefonoAlternativo': _telefonoAltController.text.trim().isNotEmpty ? _telefonoAltController.text.trim() : null,
      'calle': _calleController.text.trim(),
      'numeroExterior': _numeroExtController.text.trim(),
      'numeroInterior': _numeroIntController.text.trim().isNotEmpty ? _numeroIntController.text.trim() : null,
      'colonia': _coloniaController.text.trim(),
      'municipio': _municipioController.text.trim(),
      'estado': _estadoController.text.trim(),
      'codigoPostal': _cpController.text.trim(),
      'pais': _paisController.text.trim(),
      'ocupacion': _ocupacionSeleccionada,
      'empresa': _empresaController.text.trim(),
      'ingresoMensual': double.tryParse(_ingresoController.text.trim()) ?? 0.0,
      'saldoInicial': double.tryParse(_saldoInicialController.text.trim()) ?? 1500.00,
      'tipoCuenta': _tipoCuenta,
      'password': _passwordController.text.trim(),
    };

    final resultado = await authProvider.registrarCliente(datos);

    if (!mounted) return;

    // Si falló y el backend respondió errores mapeados por campo (HTTP 400 o 409)
    if (resultado == null) {
      if (authProvider.registroFieldErrors.isNotEmpty) {
        setState(() {
          _serverErrors = Map<String, String>.from(authProvider.registroFieldErrors);
        });
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(authProvider.errorMessage ?? 'Por favor verifique los campos marcados con error.'),
          backgroundColor: AppTheme.errorColor,
        ),
      );
      return;
    }

    String numCuenta = '';
    if (resultado.containsKey('datos') && resultado['datos'] != null) {
      final d = resultado['datos'];
      if (d is Map && d.containsKey('cuentaPrincipal') && d['cuentaPrincipal'] != null) {
        numCuenta = d['cuentaPrincipal']['numeroCuenta'] ?? '';
      }
    }

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        icon: const Icon(Icons.check_circle, color: Colors.green, size: 48),
        title: const Text('¡Registro Exitoso!'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Tu cuenta bancaria ha sido creada correctamente en el sistema.'),
            const SizedBox(height: 10),
            if (numCuenta.isNotEmpty)
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppTheme.primaryColor.withOpacity(0.08),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.credit_card, color: AppTheme.primaryColor),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Número de Cuenta:\n$numCuenta',
                        style: const TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
                  ],
                ),
              ),
            const SizedBox(height: 10),
            const Text(
              'Por tu máxima seguridad bancaria, cada compra o pago de servicios generará un PIN dinámico de un solo uso enviado directamente a tu correo registrado. Ya puedes iniciar sesión.',
              style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
            ),
          ],
        ),
        actions: [
          ElevatedButton(
            onPressed: () {
              Navigator.of(ctx).pop();
              Navigator.of(context).pop(_correoController.text.trim().toLowerCase());
            },
            child: const Text('Ir al Inicio de Sesión'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();
    final formHabilitado = _isFormularioValido && authProvider.status != AuthStatus.loading;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Crear Nueva Cuenta Bancaria'),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: ResponsiveUtil.pagePadding(context),
          child: Center(
            child: ConstrainedBox(
              constraints: BoxConstraints(maxWidth: ResponsiveUtil.contentWidth(context)),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Form(
                    key: _formKey,
                    autovalidateMode: AutovalidateMode.onUserInteraction,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        const Text(
                          'Registro de Cliente y Cuenta Digital',
                          style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 6),
                        const Text(
                          'Completa tus datos oficiales para abrir tu cuenta con saldo inicial y acceso seguro a Banca GestoPago.',
                          style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                        ),
                        const Divider(height: 28),

                        // SECCIÓN 1: DATOS PERSONALES
                        _buildSectionHeader('1. Datos Personales', Icons.person_outline),
                        const SizedBox(height: 12),
                        CustomTextField(
                          controller: _nombreController,
                          label: 'Nombre completo *',
                          hint: 'Ej. María del Carmen, Juan Carlos, José Luis Antonio',
                          prefixIcon: Icons.badge_outlined,
                          errorText: _serverErrors['nombre'],
                          textCapitalization: TextCapitalization.words,
                          inputFormatters: [LengthLimitingTextInputFormatter(100)],
                          onChanged: (_) {
                            _limpiarErrorServidor('nombre');
                            _generarCurpYRfcAutomatico();
                            setState(() {});
                          },
                          validator: _validarNombreCompleto,
                        ),
                        const SizedBox(height: 12),
                        _FilaResponsiva(
                          children: [
                            Expanded(
                              child: CustomTextField(
                                controller: _apellidoPaternoController,
                                label: 'Apellido Paterno *',
                                hint: 'Ej. Perez',
                                prefixIcon: Icons.person,
                                errorText: _serverErrors['apellidoPaterno'],
                                textCapitalization: TextCapitalization.words,
                                inputFormatters: [LengthLimitingTextInputFormatter(50)],
                                onChanged: (_) {
                                  _limpiarErrorServidor('apellidoPaterno');
                                  _generarCurpYRfcAutomatico();
                                  setState(() {});
                                },
                                validator: (v) => _validarNombreOApellido(v, 'apellido paterno'),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: CustomTextField(
                                controller: _apellidoMaternoController,
                                label: 'Apellido Materno *',
                                hint: 'Ej. Lopez',
                                prefixIcon: Icons.person,
                                errorText: _serverErrors['apellidoMaterno'],
                                textCapitalization: TextCapitalization.words,
                                inputFormatters: [LengthLimitingTextInputFormatter(50)],
                                onChanged: (_) {
                                  _limpiarErrorServidor('apellidoMaterno');
                                  _generarCurpYRfcAutomatico();
                                  setState(() {});
                                },
                                validator: (v) => _validarNombreOApellido(v, 'apellido materno'),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),

                        // Fecha de Nacimiento con Selector Calendario
                        _FilaResponsiva(
                          children: [
                            Expanded(
                              child: CustomTextField(
                                controller: _fechaNacimientoController,
                                label: 'Fecha de Nacimiento (Mayor de 18) *',
                                hint: 'AAAA-MM-DD',
                                prefixIcon: Icons.cake_outlined,
                                errorText: _serverErrors['fechaNacimiento'],
                                suffixIcon: IconButton(
                                  icon: const Icon(Icons.calendar_month, color: AppTheme.primaryColor),
                                  onPressed: _seleccionarFechaNacimiento,
                                ),
                                onChanged: (_) {
                                  _limpiarErrorServidor('fechaNacimiento');
                                  _generarCurpYRfcAutomatico(forzar: true);
                                  setState(() {});
                                },
                                validator: _validarFechaNacimiento,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),

                        // Sexo y Entidad Federativa de Nacimiento (32 estados independientes del domicilio)
                        _FilaResponsiva(
                          children: [
                            Expanded(
                              flex: 4,
                              child: DropdownButtonFormField<String>(
                                value: _sexo,
                                decoration: InputDecoration(
                                  labelText: 'Sexo *',
                                  errorText: _serverErrors['sexo'],
                                ),
                                items: const [
                                  DropdownMenuItem(value: 'MASCULINO', child: Text('Masculino (H)')),
                                  DropdownMenuItem(value: 'FEMENINO', child: Text('Femenino (M)')),
                                ],
                                onChanged: (v) {
                                  setState(() => _sexo = v ?? 'MASCULINO');
                                  _limpiarErrorServidor('sexo');
                                  _generarCurpYRfcAutomatico(forzar: true);
                                },
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              flex: 6,
                              child: DropdownButtonFormField<String>(
                                value: _entidadNacimiento,
                                decoration: InputDecoration(
                                  labelText: 'Entidad Federativa de Nacimiento *',
                                  errorText: _serverErrors['entidadNacimiento'],
                                ),
                                isExpanded: true,
                                items: _entidadesFederativasCatalogo.map((entidad) {
                                  return DropdownMenuItem<String>(
                                    value: entidad['codigo'],
                                    child: Text(
                                      '${entidad['nombre']} (${entidad['codigo']})',
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  );
                                }).toList(),
                                onChanged: (v) {
                                  setState(() => _entidadNacimiento = v ?? 'DF');
                                  _limpiarErrorServidor('entidadNacimiento');
                                  _generarCurpYRfcAutomatico();
                                },
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),

                        // Estado Civil dinámico desde catálogo con formato Soltero(a), Casado(a)...
                        _FilaResponsiva(
                          children: [
                            Expanded(
                              child: DropdownButtonFormField<String>(
                                value: _estadoCivil,
                                decoration: InputDecoration(
                                  labelText: 'Estado Civil *',
                                  errorText: _serverErrors['estadoCivil'],
                                ),
                                isExpanded: true,
                                items: _estadosCivilesCatalogo.map((item) {
                                  final val = (item['descripcion'] ?? item['codigo'] ?? 'SOLTERO').toString();
                                  final nom = (item['nombre'] ?? item['descripcion'] ?? val).toString();
                                  return DropdownMenuItem<String>(
                                    value: val,
                                    child: Text(nom, overflow: TextOverflow.ellipsis),
                                  );
                                }).toList(),
                                onChanged: (v) {
                                  setState(() => _estadoCivil = v ?? 'SOLTERO');
                                  _limpiarErrorServidor('estadoCivil');
                                },
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),

                        // CURP y RFC: 100% editables, no readonly, no disabled, calculados automáticamente
                        _FilaResponsiva(
                          children: [
                            Expanded(
                              child: CustomTextField(
                                controller: _curpController,
                                label: 'CURP (18 caracteres) *',
                                hint: 'AAAA000000HAAAAA00',
                                prefixIcon: Icons.fingerprint,
                                errorText: _serverErrors['curp'],
                                textCapitalization: TextCapitalization.characters,
                                inputFormatters: [
                                  LengthLimitingTextInputFormatter(18),
                                  FilteringTextInputFormatter.allow(RegExp(r'[a-zA-Z0-9]')),
                                ],
                                onChanged: (v) {
                                  _curpEditadoManualmente = true;
                                  _limpiarErrorServidor('curp');
                                  setState(() {});
                                },
                                validator: _validarCurp,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: CustomTextField(
                                controller: _rfcController,
                                label: 'RFC (12 o 13 caracteres) *',
                                hint: 'AAAA000000AAA',
                                prefixIcon: Icons.credit_card,
                                errorText: _serverErrors['rfc'],
                                textCapitalization: TextCapitalization.characters,
                                inputFormatters: [
                                  LengthLimitingTextInputFormatter(13),
                                  FilteringTextInputFormatter.allow(RegExp(r'[a-zA-Z0-9&Ññ]')),
                                ],
                                onChanged: (v) {
                                  _rfcEditadoManualmente = true;
                                  _limpiarErrorServidor('rfc');
                                  setState(() {});
                                },
                                validator: _validarRfc,
                              ),
                            ),
                          ],
                        ),

                        const Divider(height: 32),

                        // SECCIÓN 2: CONTACTO Y SEGURIDAD
                        _buildSectionHeader('2. Contacto y Credenciales de Acceso', Icons.security_outlined),
                        const SizedBox(height: 12),
                        CustomTextField(
                          controller: _correoController,
                          label: 'Correo Electrónico (Usuario de acceso) *',
                          hint: 'ejemplo@correo.com',
                          prefixIcon: Icons.email_outlined,
                          errorText: _serverErrors['correoElectronico'] ?? _serverErrors['correo'],
                          keyboardType: TextInputType.emailAddress,
                          inputFormatters: [LengthLimitingTextInputFormatter(100)],
                          onChanged: (_) {
                            _limpiarErrorServidor('correoElectronico');
                            _limpiarErrorServidor('correo');
                            setState(() {});
                          },
                          validator: _validarCorreo,
                        ),
                        const SizedBox(height: 12),
                        CustomTextField(
                          controller: _telefonoController,
                          label: 'Teléfono Móvil (Exactamente 10 dígitos) *',
                          hint: '5512345678',
                          prefixIcon: Icons.phone_android_outlined,
                          errorText: _serverErrors['telefonoMovil'],
                          keyboardType: TextInputType.phone,
                          inputFormatters: [
                            FilteringTextInputFormatter.digitsOnly,
                            LengthLimitingTextInputFormatter(10),
                          ],
                          onChanged: (_) {
                            _limpiarErrorServidor('telefonoMovil');
                            setState(() {});
                          },
                          validator: _validarTelefono,
                        ),
                        const SizedBox(height: 12),
                        _campo(
                          controller: _telefonoAltController,
                          label: 'Teléfono Alternativo (Opcional, 10 a 15 dígitos)',
                          hint: '8182345678',
                          icon: Icons.phone_outlined,
                          errorKey: 'telefonoAlternativo',
                          keyboardType: TextInputType.phone,
                          formatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(15)],
                          validator: _vTelAlt,
                        ),
                        const SizedBox(height: 12),
                        CustomTextField(
                          controller: _passwordController,
                          label: 'Contraseña de Acceso *',
                          hint: 'Mínimo 8 car., 1 mayús., 1 núm., 1 especial',
                          prefixIcon: Icons.lock_outline,
                          errorText: _serverErrors['password'],
                          obscureText: _obscurePassword,
                          inputFormatters: [LengthLimitingTextInputFormatter(50)],
                          suffixIcon: IconButton(
                            icon: Icon(_obscurePassword ? Icons.visibility_off : Icons.visibility),
                            onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                          ),
                          onChanged: (_) {
                            _limpiarErrorServidor('password');
                            setState(() {});
                          },
                          validator: _validarPassword,
                        ),

                        const Divider(height: 32),

                        // SECCIÓN 3: DOMICILIO
                        _buildSectionHeader('3. Domicilio', Icons.home_outlined),
                        const SizedBox(height: 12),
                        _FilaResponsiva(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(
                              child: _campo(
                                controller: _cpController,
                                label: 'Código Postal (5 dígitos) *',
                                hint: '37900',
                                icon: Icons.markunread_mailbox_outlined,
                                errorKey: 'codigoPostal',
                                keyboardType: TextInputType.number,
                                formatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(5)],
                                onChanged: _autollenarPorCodigoPostal,
                                validator: _vCp,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: _campo(
                                controller: _paisController,
                                label: 'País *',
                                hint: 'Mexico',
                                icon: Icons.public,
                                errorKey: 'pais',
                                formatters: [LengthLimitingTextInputFormatter(50)],
                                validator: _vPais,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        _FilaResponsiva(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(
                              child: _campo(
                                controller: _estadoController,
                                label: 'Estado *',
                                hint: 'Guanajuato',
                                icon: Icons.map_outlined,
                                errorKey: 'estado',
                                formatters: [
                                  LengthLimitingTextInputFormatter(40),
                                  FilteringTextInputFormatter.allow(RegExp(r'[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]')),
                                ],
                                validator: _vEstado,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: _campo(
                                controller: _municipioController,
                                label: 'Municipio / Alcaldía *',
                                hint: 'San Luis de la Paz',
                                icon: Icons.location_city_outlined,
                                errorKey: 'municipio',
                                formatters: [
                                  LengthLimitingTextInputFormatter(60),
                                  FilteringTextInputFormatter.allow(RegExp(r'[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]')),
                                ],
                                validator: _vMunicipio,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        _campo(
                          controller: _coloniaController,
                          label: 'Colonia *',
                          hint: 'Centro',
                          icon: Icons.holiday_village_outlined,
                          errorKey: 'colonia',
                          formatters: [
                            LengthLimitingTextInputFormatter(60),
                            FilteringTextInputFormatter.allow(RegExp(r'[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]')),
                          ],
                          validator: _vColonia,
                        ),
                        const SizedBox(height: 12),
                        _campo(
                          controller: _calleController,
                          label: 'Calle *',
                          hint: 'Avenida Constitucion',
                          icon: Icons.signpost_outlined,
                          errorKey: 'calle',
                          formatters: [
                            LengthLimitingTextInputFormatter(60),
                            FilteringTextInputFormatter.allow(RegExp(r'[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]')),
                          ],
                          validator: _vCalle,
                        ),
                        const SizedBox(height: 12),
                        _FilaResponsiva(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(
                              child: _campo(
                                controller: _numeroExtController,
                                label: 'Número Exterior *',
                                hint: '450',
                                icon: Icons.numbers,
                                errorKey: 'numeroExterior',
                                keyboardType: TextInputType.number,
                                formatters: [
                                  FilteringTextInputFormatter.digitsOnly,
                                  LengthLimitingTextInputFormatter(6),
                                ],
                                validator: _vNumExt,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: _campo(
                                controller: _numeroIntController,
                                label: 'Número Interior (Opcional)',
                                hint: '12',
                                icon: Icons.apartment_outlined,
                                errorKey: 'numeroInterior',
                                keyboardType: TextInputType.number,
                                formatters: [
                                  FilteringTextInputFormatter.digitsOnly,
                                  LengthLimitingTextInputFormatter(6),
                                ],
                                validator: _vNumInt,
                              ),
                            ),
                          ],
                        ),

                        const Divider(height: 32),

                        // SECCIÓN 4: INFORMACIÓN LABORAL
                        _buildSectionHeader('4. Información Laboral', Icons.work_outline),
                        const SizedBox(height: 12),
                        DropdownButtonFormField<String>(
                          value: _ocupacionSeleccionada,
                          decoration: InputDecoration(
                            labelText: 'Ocupación *',
                            prefixIcon: const Icon(Icons.badge_outlined, color: AppTheme.primaryColor),
                            errorText: _serverErrors['ocupacion'],
                          ),
                          items: const [
                            DropdownMenuItem(value: 'Analista Financiero', child: Text('Analista Financiero')),
                            DropdownMenuItem(value: 'Ingeniero de Software / TI', child: Text('Ingeniero de Software / TI')),
                            DropdownMenuItem(value: 'Administrador de Empresas', child: Text('Administrador de Empresas')),
                            DropdownMenuItem(value: 'Contador Público', child: Text('Contador Público')),
                            DropdownMenuItem(value: 'Médico / Especialista de la Salud', child: Text('Médico / Especialista de la Salud')),
                            DropdownMenuItem(value: 'Abogado / Consultor Jurídico', child: Text('Abogado / Consultor Jurídico')),
                            DropdownMenuItem(value: 'Docente / Académico', child: Text('Docente / Académico')),
                            DropdownMenuItem(value: 'Arquitecto / Ingeniero Civil', child: Text('Arquitecto / Ingeniero Civil')),
                            DropdownMenuItem(value: 'Comerciante / Ventas', child: Text('Comerciante / Ventas')),
                            DropdownMenuItem(value: 'Empresario / Director General', child: Text('Empresario / Director General')),
                            DropdownMenuItem(value: 'Mercadólogo / Publicista', child: Text('Mercadólogo / Publicista')),
                            DropdownMenuItem(value: 'Diseñador Gráfico / Multimedia', child: Text('Diseñador Gráfico / Multimedia')),
                            DropdownMenuItem(value: 'Estudiante', child: Text('Estudiante')),
                            DropdownMenuItem(value: 'Profesionista Independiente', child: Text('Profesionista Independiente')),
                            DropdownMenuItem(value: 'Otro', child: Text('Otro')),
                          ],
                          onChanged: (v) {
                            setState(() => _ocupacionSeleccionada = v ?? 'Analista Financiero');
                            _limpiarErrorServidor('ocupacion');
                          },
                        ),
                        const SizedBox(height: 12),
                        _campo(
                          controller: _empresaController,
                          label: 'Empresa *',
                          hint: 'Grupo Finanzas del Norte',
                          icon: Icons.business_outlined,
                          errorKey: 'empresa',
                          formatters: [LengthLimitingTextInputFormatter(100)],
                          validator: _vEmpresa,
                        ),
                        const SizedBox(height: 12),
                        _campo(
                          controller: _ingresoController,
                          label: 'Ingreso Mensual (MXN) *',
                          hint: 'Mayor a 0.00 (máx. 2 decimales)',
                          icon: Icons.payments_outlined,
                          errorKey: 'ingresoMensual',
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          formatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.]'))],
                          validator: _validarIngreso,
                        ),

                        const Divider(height: 32),

                        // SECCIÓN 5: CUENTA Y SALDO INICIAL
                        _buildSectionHeader('5. Configuración de Cuenta Bancaria', Icons.account_balance_outlined),
                        const SizedBox(height: 12),
                        _FilaResponsiva(
                          children: [
                            Expanded(
                              child: DropdownButtonFormField<String>(
                                value: _tipoCuenta,
                                decoration: const InputDecoration(labelText: 'Tipo de Cuenta'),
                                items: const [
                                  DropdownMenuItem(value: 'DEBITO', child: Text('Débito')),
                                  DropdownMenuItem(value: 'AHORRO', child: Text('Ahorro')),
                                ],
                                onChanged: (v) => setState(() => _tipoCuenta = v ?? 'DEBITO'),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: CustomTextField(
                                controller: _saldoInicialController,
                                label: 'Saldo Inicial (MXN) *',
                                hint: 'Mayor o igual a 0.00 (máx. 2 dec.)',
                                prefixIcon: Icons.attach_money,
                                errorText: _serverErrors['saldoInicial'],
                                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                                onChanged: (_) {
                                  _limpiarErrorServidor('saldoInicial');
                                  setState(() {});
                                },
                                validator: _validarSaldoInicial,
                              ),
                            ),
                          ],
                        ),

                        const SizedBox(height: 28),

                        // BOTÓN REGISTRAR REACTIVO (SE DESHABILITA SI HAY ERRORES O CAMPOS INCOMPLETOS)
                        CustomButton(
                          text: 'Crear Mi Cuenta Bancaria',
                          isLoading: authProvider.status == AuthStatus.loading,
                          onPressed: formHabilitado ? _submitRegistro : null,
                        ),
                        if (!formHabilitado && authProvider.status != AuthStatus.loading) ...[
                          const SizedBox(height: 8),
                          const Center(
                            child: Text(
                              'Completa todos los campos obligatorios sin errores para continuar',
                              style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                            ),
                          ),
                        ],
                        const SizedBox(height: 12),
                        TextButton(
                          onPressed: () => Navigator.of(context).pop(),
                          child: const Text('¿Ya tienes cuenta? Regresar al Inicio de Sesión'),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _campo({
    required TextEditingController controller,
    required String label,
    required String hint,
    required IconData icon,
    required String errorKey,
    required String? Function(String?) validator,
    TextInputType? keyboardType,
    List<TextInputFormatter>? formatters,
    ValueChanged<String>? onChanged,
  }) {
    return CustomTextField(
      controller: controller,
      label: label,
      hint: hint,
      prefixIcon: icon,
      errorText: _serverErrors[errorKey],
      keyboardType: keyboardType ?? TextInputType.text,
      inputFormatters: formatters,
      onChanged: (val) {
        _limpiarErrorServidor(errorKey);
        if (onChanged != null) onChanged(val);
        setState(() {});
      },
      validator: validator,
    );
  }

  Widget _buildSectionHeader(String title, IconData icon) {
    return Row(
      children: [
        Icon(icon, size: 20, color: AppTheme.primaryColor),
        const SizedBox(width: 8),
        Text(
          title,
          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: AppTheme.primaryColor),
        ),
      ],
    );
  }
}
