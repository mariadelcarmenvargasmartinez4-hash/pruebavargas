import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../../core/constants/api_constants.dart';
import '../../core/network/api_client.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/responsive_util.dart';
import '../state/auth_provider.dart';
import '../widgets/custom_button.dart';

/// Permite únicamente letras (incluyendo acentos/ñ/ü) y un solo espacio entre palabras.
/// Elimina espacios iniciales y tabulaciones en tiempo real mientras el usuario escribe.
class _SoloLetrasEspacioUnicoFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final raw = newValue.text;
    final cursor = newValue.selection.end.clamp(0, raw.length);
    final buffer = StringBuffer();
    int nuevoCursor = 0;
    final regLetra = RegExp(r'[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]');

    for (int i = 0; i < raw.length; i++) {
      final ch = raw[i];
      if (regLetra.hasMatch(ch)) {
        buffer.write(ch);
        if (i < cursor) nuevoCursor++;
      } else if (ch == ' ') {
        final actual = buffer.toString();
        if (actual.isNotEmpty && !actual.endsWith(' ')) {
          buffer.write(' ');
          if (i < cursor) nuevoCursor++;
        }
      }
    }

    final resultado = buffer.toString();
    return TextEditingValue(
      text: resultado,
      selection: TextSelection.collapsed(
        offset: nuevoCursor.clamp(0, resultado.length),
      ),
    );
  }
}

/// Elimina tabulaciones, espacios iniciales y colapsa espacios múltiples consecutivos en uno solo.
class _SinTabulacionEspacioUnicoFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final raw = newValue.text;
    final cursor = newValue.selection.end.clamp(0, raw.length);
    final buffer = StringBuffer();
    int nuevoCursor = 0;

    for (int i = 0; i < raw.length; i++) {
      final ch = raw[i];
      if (ch == '\t' || ch == '\r' || ch == '\n') {
        continue;
      }
      if (ch == ' ') {
        final actual = buffer.toString();
        if (actual.isNotEmpty && !actual.endsWith(' ')) {
          buffer.write(' ');
          if (i < cursor) nuevoCursor++;
        }
      } else {
        buffer.write(ch);
        if (i < cursor) nuevoCursor++;
      }
    }

    final resultado = buffer.toString();
    return TextEditingValue(
      text: resultado,
      selection: TextSelection.collapsed(
        offset: nuevoCursor.clamp(0, resultado.length),
      ),
    );
  }
}

/// Impide cualquier espacio en blanco o tabulación.
class _SinEspaciosNiTabsFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final raw = newValue.text;
    final cursor = newValue.selection.end.clamp(0, raw.length);
    final buffer = StringBuffer();
    int nuevoCursor = 0;

    for (int i = 0; i < raw.length; i++) {
      final ch = raw[i];
      if (!RegExp(r'\s').hasMatch(ch)) {
        buffer.write(ch);
        if (i < cursor) nuevoCursor++;
      }
    }

    final resultado = buffer.toString();
    return TextEditingValue(
      text: resultado,
      selection: TextSelection.collapsed(
        offset: nuevoCursor.clamp(0, resultado.length),
      ),
    );
  }
}

/// Formateador monetario en tiempo real: elimina tabulaciones y espacios,
/// bloquea letras/símbolos y permite únicamente dígitos con un punto y hasta 2 decimales.
class _MonedaInputFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final raw = newValue.text;
    final cursor = newValue.selection.end.clamp(0, raw.length);
    final buffer = StringBuffer();
    int nuevoCursor = 0;
    final regPermitido = RegExp(r'[0-9.]');

    for (int i = 0; i < raw.length; i++) {
      final ch = raw[i];
      if (regPermitido.hasMatch(ch)) {
        buffer.write(ch);
        if (i < cursor) nuevoCursor++;
      }
    }

    var limpio = buffer.toString();
    if (limpio.isEmpty) {
      return const TextEditingValue(
        text: '',
        selection: TextSelection.collapsed(offset: 0),
      );
    }

    if (limpio == '.') {
      return const TextEditingValue(
        text: '0.',
        selection: TextSelection.collapsed(offset: 2),
      );
    }

    if (!RegExp(r'^\d+(\.\d{0,2})?$').hasMatch(limpio)) {
      return oldValue;
    }

    return TextEditingValue(
      text: limpio,
      selection: TextSelection.collapsed(
        offset: nuevoCursor.clamp(0, limpio.length),
      ),
    );
  }
}

/// Impide superar la longitud máxima permitida y notifica para mostrar aviso al usuario.
class _ControlLimiteFormatter extends TextInputFormatter {
  final int maxLength;
  final VoidCallback onLimiteExcedido;
  final VoidCallback onDentroDeLimite;

  _ControlLimiteFormatter({
    required this.maxLength,
    required this.onLimiteExcedido,
    required this.onDentroDeLimite,
  });

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    if (newValue.text.length > maxLength) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        onLimiteExcedido();
      });
      final truncado = newValue.text.substring(0, maxLength);
      final offset = newValue.selection.end.clamp(0, maxLength);
      return TextEditingValue(
        text: truncado,
        selection: TextSelection.collapsed(offset: offset),
      );
    }
    if (newValue.text.length < oldValue.text.length &&
        newValue.text.length < maxLength) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        onDentroDeLimite();
      });
    }
    return newValue;
  }
}

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

  // Datos Personales (todos vacíos al abrir el formulario)
  final _nombreController = TextEditingController();
  final _apellidoPaternoController = TextEditingController();
  final _apellidoMaternoController = TextEditingController();
  final _fechaNacimientoController = TextEditingController();
  final _curpController = TextEditingController();
  final _rfcController = TextEditingController();

  String? _sexo;
  String _nacionalidad = 'MEXICANA';
  String? _estadoCivil;
  String? _entidadNacimiento;

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

  // Avisos en tiempo real por intento de superar el límite de caracteres por campo
  final Map<String, String> _avisosLimite = {};
  final Map<String, FocusNode> _focusNodes = {};

  // Catálogo dinámico de Colonias cargado exclusivamente desde el endpoint del backend
  List<String> _coloniasDisponibles = [];
  String? _coloniaSeleccionada;
  bool _cargandoColonias = false;

  @override
  void initState() {
    super.initState();
    _passwordController.clear();
    _cargarCatalogos();
  }

  Future<void> _cargarCatalogos() async {
    final apiClient = context.read<ApiClient>();
    try {
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

  Future<void> _cargarColonias({String? cp, String? municipio}) async {
    final cpLimpio = cp?.trim() ?? '';
    final munLimpio = municipio?.trim() ?? '';

    if (cpLimpio.length != 5 && munLimpio.isEmpty) {
      if (mounted) {
        setState(() {
          _coloniasDisponibles = [];
          _coloniaSeleccionada = null;
          _coloniaController.clear();
          _cargandoColonias = false;
        });
      }
      return;
    }

    if (mounted) {
      setState(() => _cargandoColonias = true);
    }
    try {
      final apiClient = context.read<ApiClient>();
      final queryParams = <String, dynamic>{};
      if (cpLimpio.length == 5) queryParams['cp'] = cpLimpio;
      if (munLimpio.isNotEmpty) queryParams['municipio'] = munLimpio;

      final resp = await apiClient.dio.get(
        '${ApiConstants.catalogos}/colonias',
        queryParameters: queryParams,
      );
      if (!mounted) return;
      if (cpLimpio.length == 5 && _cpController.text.trim() != cpLimpio) {
        setState(() => _cargandoColonias = false);
        return;
      }
      if (resp.statusCode == 200 && resp.data != null) {
        final data = resp.data;
        List items = [];
        if (data is Map && data['datos'] is List) {
          items = data['datos'] as List;
        } else if (data is List) {
          items = data;
        }
        final nuevasColonias = <String>[];
        for (final item in items) {
          final nombre = (item is Map
                  ? (item['descripcion'] ?? item['nombre'] ?? '')
                  : item.toString())
              .toString()
              .trim();
          if (nombre.isNotEmpty && !nuevasColonias.contains(nombre)) {
            nuevasColonias.add(nombre);
          }
        }
        setState(() {
          _coloniasDisponibles = nuevasColonias;
          // No seleccionar automáticamente ninguna colonia; limpiar selección si ya no pertenece
          if (_coloniaSeleccionada != null &&
              !nuevasColonias.contains(_coloniaSeleccionada)) {
            _coloniaSeleccionada = null;
            _coloniaController.clear();
          }
        });
      }
    } catch (_) {
      // Se mantiene dinámico sin opciones escritas manualmente en el frontend
    } finally {
      if (mounted) {
        setState(() => _cargandoColonias = false);
      }
    }
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
  final _paisController = TextEditingController();

  // Laboral y Financiero (vacíos inicialmente)
  String? _ocupacionSeleccionada;
  final _empresaController = TextEditingController();
  final _ingresoController = TextEditingController();
  final _saldoInicialController = TextEditingController();
  String? _tipoCuenta;

  // Seguridad (completamente en blanco al abrir el formulario)
  final _passwordController = TextEditingController();
  bool _obscurePassword = true;

  FocusNode _obtenerFocusNode(
    String key,
    TextEditingController controller, {
    bool limpiarEspaciosExtremos = false,
    bool actualizarCurpRfc = false,
    bool bloquearTab = false,
  }) {
    return _focusNodes.putIfAbsent(key, () {
      final node = FocusNode(
        onKeyEvent: bloquearTab
            ? (FocusNode n, KeyEvent event) {
                if (event.logicalKey == LogicalKeyboardKey.tab) {
                  return KeyEventResult.handled;
                }
                return KeyEventResult.ignored;
              }
            : null,
      );
      node.addListener(() {
        if (!node.hasFocus && mounted) {
          if (limpiarEspaciosExtremos) {
            final limpio = controller.text.trim().replaceAll(RegExp(r' {2,}'), ' ');
            if (controller.text != limpio) {
              controller.text = limpio;
              if (actualizarCurpRfc) {
                _generarCurpYRfcAutomatico();
              }
              setState(() {});
            }
          }
        }
      });
      return node;
    });
  }

  _ControlLimiteFormatter _crearLimiteFormatter(String key, int maxLen) {
    return _ControlLimiteFormatter(
      maxLength: maxLen,
      onLimiteExcedido: () {
        if (mounted && _avisosLimite[key] == null) {
          setState(() {
            _avisosLimite[key] = 'Has superado el límite de caracteres permitido';
          });
        }
      },
      onDentroDeLimite: () {
        if (mounted && _avisosLimite.containsKey(key)) {
          setState(() {
            _avisosLimite.remove(key);
          });
        }
      },
    );
  }

  @override
  void dispose() {
    _scrollController.dispose();
    for (final node in _focusNodes.values) {
      node.dispose();
    }
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

    // Si cambia el código postal, limpiar país, estado, municipio, selección anterior y opciones de colonia
    setState(() {
      _paisController.clear();
      _estadoController.clear();
      _municipioController.clear();
      _coloniaSeleccionada = null;
      _coloniaController.clear();
      _coloniasDisponibles = [];
      _serverErrors.remove('codigoPostal');
    });

    if (t.length != 5 || int.tryParse(t) == null) {
      return;
    }

    try {
      final apiClient = context.read<ApiClient>();
      final resp = await apiClient.dio.get('${ApiConstants.catalogos}/codigo-postal/$t');
      if (!mounted || _cpController.text.trim() != t) return;

      final body = resp.data;
      final datos = (body is Map && body['datos'] is Map) ? body['datos'] as Map : null;
      final paisApi = (datos?['pais'] ?? '').toString().trim();
      final estApi = (datos?['estado'] ?? '').toString().trim();
      final munApi = (datos?['municipio'] ?? '').toString().trim();

      if (paisApi.isEmpty || estApi.isEmpty || munApi.isEmpty) {
        setState(() {
          _paisController.clear();
          _estadoController.clear();
          _municipioController.clear();
          _coloniaSeleccionada = null;
          _coloniaController.clear();
          _coloniasDisponibles = [];
          _serverErrors['codigoPostal'] = 'No se encontraron resultados para el código postal ingresado.';
        });
        return;
      }

      setState(() {
        _paisController.text = paisApi;
        _estadoController.text = estApi;
        _municipioController.text = munApi;
        _serverErrors.remove('codigoPostal');
        _serverErrors.remove('pais');
        _serverErrors.remove('estado');
        _serverErrors.remove('municipio');
      });

      // Cargar únicamente las colonias del municipio correspondiente mediante el API (sin seleccionar automáticamente)
      await _cargarColonias(cp: t, municipio: munApi);
    } on DioException catch (e) {
      if (!mounted || _cpController.text.trim() != t) return;
      final msg = ApiClient.parseError(e);
      setState(() {
        _paisController.clear();
        _estadoController.clear();
        _municipioController.clear();
        _coloniaSeleccionada = null;
        _coloniaController.clear();
        _coloniasDisponibles = [];
        _serverErrors['codigoPostal'] = msg;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(msg),
          backgroundColor: AppTheme.errorColor,
          behavior: SnackBarBehavior.floating,
        ),
      );
    } catch (_) {
      if (!mounted || _cpController.text.trim() != t) return;
      setState(() {
        _paisController.clear();
        _estadoController.clear();
        _municipioController.clear();
        _coloniaSeleccionada = null;
        _coloniaController.clear();
        _coloniasDisponibles = [];
        _serverErrors['codigoPostal'] = 'No fue posible consultar el código postal ingresado.';
      });
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

    // RFC: 10 caracteres base calculados según SAT + homoclave (3 caracteres)
    String homoclaveActual = 'AA1';
    if (_rfcController.text.trim().length == 13) {
      homoclaveActual = _rfcController.text.trim().substring(10).toUpperCase();
    }
    final rfcOficial = '$cuatroLetras$aammdd$homoclaveActual';

    String? curpOficial;
    if (_sexo != null && _sexo!.isNotEmpty && _entidadNacimiento != null && _entidadNacimiento!.isNotEmpty) {
      // 3. Sexo (M / H)
      final charSexo = (_sexo == 'FEMENINO') ? 'M' : 'H';

      // 4. Entidad Federativa de Nacimiento (2 dígitos oficiales)
      final cveEntidad = _entidadNacimiento!;

      // 5. Consonantes internas
      final con1 = _primeraConsonanteInterna(pSignificativo);
      final con2 = mSignificativo.isNotEmpty ? _primeraConsonanteInterna(mSignificativo) : 'X';
      final con3 = _primeraConsonanteInterna(nSignificativo);

      // 6. Diferenciador de siglo
      final difSiglo = (anioNum >= 2000) ? 'A' : '0';

      // 7. Base CURP de 17 caracteres y Dígito Verificador oficial RENAPO
      final curp17 = '$cuatroLetras$aammdd$charSexo$cveEntidad$con1$con2$con3$difSiglo';
      final digitoVerificador = _calcularDigitoVerificadorRenapo(curp17);
      curpOficial = '$curp17$digitoVerificador';
    }

    setState(() {
      if (curpOficial != null && (forzar || !_curpEditadoManualmente || _curpController.text.isEmpty)) {
        _curpController.text = curpOficial;
      }
      if (forzar || !_rfcEditadoManualmente || _rfcController.text.isEmpty) {
        _rfcController.text = rfcOficial;
      }
    });
  }

  bool _abriendoCalendario = false;

  Future<void> _seleccionarFechaNacimiento() async {
    if (_abriendoCalendario || !mounted) return;
    _abriendoCalendario = true;
    FocusManager.instance.primaryFocus?.unfocus();

    try {
      final hoy = DateTime.now();
      final primeraFecha = DateTime(1920, 1, 1);
      final fechaBaseCalendario = DateTime(hoy.year - 18, hoy.month, hoy.day);
      var fechaInicial =
          DateTime.tryParse(_fechaNacimientoController.text.trim()) ?? fechaBaseCalendario;
      if (fechaInicial.isBefore(primeraFecha) || fechaInicial.isAfter(hoy)) {
        fechaInicial = fechaBaseCalendario;
      }

      final nuevaFecha = await showDatePicker(
        context: context,
        initialDate: fechaInicial,
        firstDate: primeraFecha,
        lastDate: hoy,
        initialEntryMode: DatePickerEntryMode.calendar,
        helpText: 'Seleccionar fecha de nacimiento',
        cancelText: 'Cancelar',
        confirmText: 'Aceptar',
        fieldLabelText: 'Fecha de nacimiento',
        fieldHintText: 'AAAA-MM-DD',
        errorFormatText: 'Formato inválido (AAAA-MM-DD)',
        errorInvalidText: 'Fecha fuera del rango permitido',
      );

      if (nuevaFecha != null && mounted) {
        final y = nuevaFecha.year.toString().padLeft(4, '0');
        final m = nuevaFecha.month.toString().padLeft(2, '0');
        final d = nuevaFecha.day.toString().padLeft(2, '0');
        setState(() {
          _fechaNacimientoController.text = '$y-$m-$d';
        });
        _limpiarErrorServidor('fechaNacimiento');
        _generarCurpYRfcAutomatico(forzar: true);
      }
    } finally {
      _abriendoCalendario = false;
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
    if (_sexo != null && _sexo!.isNotEmpty) {
      final charSexo = curp[10];
      final esperadoSexo = (_sexo == 'FEMENINO') ? 'M' : 'H';
      if (charSexo != esperadoSexo) {
        return 'La posición 11 del CURP ($charSexo) no coincide con el sexo ($_sexo: $esperadoSexo)';
      }
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
    if (_entidadNacimiento != null &&
        _entidadNacimiento!.isNotEmpty &&
        curp.length >= 13 &&
        curp.substring(11, 13) != _entidadNacimiento) {
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
    if (val.contains('\t')) {
      return 'El ingreso mensual no permite tabulaciones';
    }
    if (val.startsWith(' ') || val.endsWith(' ')) {
      return 'El ingreso mensual no permite espacios al inicio ni al final';
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
    if (v.contains('\t')) return 'El campo $campo no permite tabulaciones';
    if (v.startsWith(' ')) return 'El campo $campo no puede iniciar con espacios';
    if (v.endsWith(' ')) return 'El campo $campo no puede terminar con espacios';
    if (v.contains('  ')) return 'No se permiten múltiples espacios internos consecutivos';
    if (v.trim().length < min) return 'El campo $campo debe tener al menos $min caracteres';
    if (v.length > max) return 'El campo $campo no puede exceder $max caracteres';
    if (regex != null && !regex.hasMatch(v.trim())) {
      return msgRegex ?? 'El campo $campo tiene un formato inválido';
    }
    return null;
  }

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
  String? _vColonia(String? v) {
    if (v == null || v.trim().isEmpty) {
      return 'Seleccione una colonia del catálogo';
    }
    return _validarTextoDomicilioLetras(v, 'colonia', min: 3, max: 60);
  }
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

  String? _vPais(String? v) => _validarTextoDomicilioLetras(v, 'país', min: 2, max: 50);

  String? _vCp(String? v) {
    final t = (v ?? '').trim();
    if (t.isEmpty) return 'El código postal es obligatorio y no debe estar vacío';
    if (!RegExp(r'^\d{5}$').hasMatch(t)) return 'El código postal debe contener exactamente 5 dígitos numéricos';
    return null;
  }

  String? _vEmpresa(String? v) => _validarTexto(v, 'empresa', 100);

  String? _vTelAlt(String? v) {
    final t = v ?? '';
    if (t.isEmpty) return null;
    if (!RegExp(r'^\d+$').hasMatch(t)) {
      return 'El teléfono opcional solo permite números';
    }
    if (t.length != 10) {
      return 'Debe ingresar exactamente 10 dígitos numéricos (capturados: ${t.length})';
    }
    return null;
  }

  bool get _isFormularioValido {
    final coloniaActual = _coloniaSeleccionada ?? _coloniaController.text;
    return _validarNombreCompleto(_nombreController.text) == null &&
        _validarNombreOApellido(_apellidoPaternoController.text, 'apellido paterno') == null &&
        _validarNombreOApellido(_apellidoMaternoController.text, 'apellido materno') == null &&
        _validarFechaNacimiento(_fechaNacimientoController.text) == null &&
        _validarCurp(_curpController.text) == null &&
        _validarRfc(_rfcController.text) == null &&
        _sexo != null &&
        _sexo!.isNotEmpty &&
        _entidadNacimiento != null &&
        _entidadNacimiento!.isNotEmpty &&
        _estadoCivil != null &&
        _estadoCivil!.isNotEmpty &&
        _validarCorreo(_correoController.text) == null &&
        _validarTelefono(_telefonoController.text) == null &&
        _validarPassword(_passwordController.text) == null &&
        _validarIngreso(_ingresoController.text) == null &&
        _validarSaldoInicial(_saldoInicialController.text) == null &&
        _vTelAlt(_telefonoAltController.text) == null &&
        _vCalle(_calleController.text) == null &&
        _vNumExt(_numeroExtController.text) == null &&
        _vNumInt(_numeroIntController.text) == null &&
        _vColonia(coloniaActual) == null &&
        _vMunicipio(_municipioController.text) == null &&
        _vEstado(_estadoController.text) == null &&
        !_serverErrors.containsKey('codigoPostal') &&
        _vCp(_cpController.text) == null &&
        _vPais(_paisController.text) == null &&
        _ocupacionSeleccionada != null &&
        _ocupacionSeleccionada!.isNotEmpty &&
        _tipoCuenta != null &&
        _tipoCuenta!.isNotEmpty &&
        _vEmpresa(_empresaController.text) == null;
  }

  void _normalizarEspaciosEnControladores() {
    final controladoresTexto = [
      _nombreController,
      _apellidoPaternoController,
      _apellidoMaternoController,
      _paisController,
      _estadoController,
      _municipioController,
      _calleController,
      _empresaController,
      _ingresoController,
      _saldoInicialController,
      _correoController,
      _telefonoController,
      _telefonoAltController,
    ];
    for (final ctrl in controladoresTexto) {
      final limpio = ctrl.text.trim().replaceAll(RegExp(r' {2,}'), ' ');
      if (ctrl.text != limpio) {
        ctrl.text = limpio;
      }
    }
  }

  Future<void> _submitRegistro() async {
    _normalizarEspaciosEnControladores();
    // Limpiar errores del servidor previos
    setState(() => _serverErrors = {});

    if (!_formKey.currentState!.validate()) return;

    final authProvider = context.read<AuthProvider>();
    final coloniaFinal = (_coloniaSeleccionada ?? _coloniaController.text).trim();

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
      'colonia': coloniaFinal,
      'municipio': _municipioController.text.trim(),
      'estado': _estadoController.text.trim(),
      'codigoPostal': _cpController.text.trim(),
      'pais': _paisController.text.trim(),
      'ocupacion': _ocupacionSeleccionada,
      'empresa': _empresaController.text.trim(),
      'ingresoMensual': double.tryParse(_ingresoController.text.trim()) ?? 0.0,
      'saldoInicial': double.tryParse(_saldoInicialController.text.trim()) ?? 1500.00,
      'tipoCuenta': _tipoCuenta,
      'password': _passwordController.text,
    };

    final resultado = await authProvider.registrarCliente(datos);

    if (!mounted) return;

    // Si falló y el backend respondió errores mapeados por campo (HTTP 400 o 409)
    if (resultado == null) {
      if (authProvider.registroFieldErrors.isNotEmpty) {
        setState(() {
          _serverErrors = Map<String, String>.from(authProvider.registroFieldErrors);
          // Único caso en que se retrocede: llevar al usuario a la pantalla del campo con error del servidor
          final pasoError = _pasoDeErrorServidor(_serverErrors);
          if (pasoError != null && pasoError < _paso) {
            _paso = pasoError;
          }
        });
        if (_scrollController.hasClients) {
          _scrollController.jumpTo(0);
        }
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
              TextInput.finishAutofillContext(shouldSave: false);
              _correoController.clear();
              _passwordController.clear();
              Navigator.of(ctx).pop();
              Navigator.of(context).pop(true);
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

    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) _confirmarRegresarInicio();
      },
      child: Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        leadingWidth: 110,
        leading: TextButton.icon(
          onPressed: _confirmarRegresarInicio,
          icon: const Icon(Icons.home, color: Colors.white),
          label: const Text('Inicio', style: TextStyle(color: Colors.white)),
        ),
        title: const Text('Crear Nueva Cuenta Bancaria'),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          controller: _scrollController,
          padding: ResponsiveUtil.pagePadding(context),
          child: Center(
            child: ConstrainedBox(
              constraints: BoxConstraints(maxWidth: ResponsiveUtil.contentWidth(context)),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Form(
                    key: _formKey,
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

                        // Indicador de progreso (solo informativo, no seleccionable)
                        _buildIndicadorProgreso(),
                        const SizedBox(height: 20),

                        // PANTALLA 1: DATOS PERSONALES
                        if (_paso == 0) ...[
                        _buildSectionHeader('1. Datos Personales', Icons.person_outline),
                        const SizedBox(height: 12),
                        _campo(
                          controller: _nombreController,
                          label: 'Nombre completo *',
                          hint: 'Ej. María del Carmen, Juan Carlos, José Luis Antonio',
                          icon: Icons.badge_outlined,
                          errorKey: 'nombre',
                          textCapitalization: TextCapitalization.words,
                          limpiarEspaciosExtremos: true,
                          actualizarCurpRfc: true,
                          formatters: [
                            _SoloLetrasEspacioUnicoFormatter(),
                            _crearLimiteFormatter('nombre', 100),
                          ],
                          onChanged: (_) => _generarCurpYRfcAutomatico(),
                          validator: _validarNombreCompleto,
                        ),
                        const SizedBox(height: 12),
                        _FilaResponsiva(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(
                              child: _campo(
                                controller: _apellidoPaternoController,
                                label: 'Apellido Paterno *',
                                hint: 'Ej. Perez',
                                icon: Icons.person,
                                errorKey: 'apellidoPaterno',
                                textCapitalization: TextCapitalization.words,
                                limpiarEspaciosExtremos: true,
                                actualizarCurpRfc: true,
                                formatters: [
                                  _SoloLetrasEspacioUnicoFormatter(),
                                  _crearLimiteFormatter('apellidoPaterno', 50),
                                ],
                                onChanged: (_) => _generarCurpYRfcAutomatico(),
                                validator: (v) => _validarNombreOApellido(v, 'apellido paterno'),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: _campo(
                                controller: _apellidoMaternoController,
                                label: 'Apellido Materno *',
                                hint: 'Ej. Lopez',
                                icon: Icons.person,
                                errorKey: 'apellidoMaterno',
                                textCapitalization: TextCapitalization.words,
                                limpiarEspaciosExtremos: true,
                                actualizarCurpRfc: true,
                                formatters: [
                                  _SoloLetrasEspacioUnicoFormatter(),
                                  _crearLimiteFormatter('apellidoMaterno', 50),
                                ],
                                onChanged: (_) => _generarCurpYRfcAutomatico(),
                                validator: (v) => _validarNombreOApellido(v, 'apellido materno'),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),

                        // Fecha de Nacimiento con Selector Calendario
                        _FilaResponsiva(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(
                              child: _campo(
                                controller: _fechaNacimientoController,
                                label: 'Fecha de Nacimiento (Mayor de 18) *',
                                hint: 'AAAA-MM-DD',
                                icon: Icons.cake_outlined,
                                errorKey: 'fechaNacimiento',
                                readOnly: true,
                                onTap: _seleccionarFechaNacimiento,
                                suffixIcon: IconButton(
                                  tooltip: 'Abrir calendario',
                                  icon: const Icon(Icons.calendar_month, color: AppTheme.primaryColor),
                                  onPressed: _seleccionarFechaNacimiento,
                                ),
                                formatters: [
                                  FilteringTextInputFormatter.allow(RegExp(r'[0-9-]')),
                                  _crearLimiteFormatter('fechaNacimiento', 10),
                                ],
                                onChanged: (_) => _generarCurpYRfcAutomatico(forzar: true),
                                validator: _validarFechaNacimiento,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),

                        // Sexo y Entidad Federativa de Nacimiento (sin selección por defecto)
                        _FilaResponsiva(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(
                              flex: 4,
                              child: DropdownButtonFormField<String>(
                                value: _sexo,
                                autovalidateMode: AutovalidateMode.onUserInteraction,
                                decoration: InputDecoration(
                                  labelText: 'Sexo *',
                                  errorText: _serverErrors['sexo'],
                                  errorMaxLines: 4,
                                ),
                                hint: const Text('Seleccione', overflow: TextOverflow.ellipsis),
                                items: const [
                                  DropdownMenuItem(value: 'MASCULINO', child: Text('Masculino (H)')),
                                  DropdownMenuItem(value: 'FEMENINO', child: Text('Femenino (M)')),
                                ],
                                onChanged: (v) {
                                  setState(() => _sexo = v);
                                  _limpiarErrorServidor('sexo');
                                  _generarCurpYRfcAutomatico(forzar: true);
                                },
                                validator: (v) =>
                                    (v == null || v.isEmpty) ? 'Seleccione el sexo' : null,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              flex: 6,
                              child: DropdownButtonFormField<String>(
                                value: _entidadesFederativasCatalogo.any((e) => e['codigo'] == _entidadNacimiento)
                                    ? _entidadNacimiento
                                    : null,
                                autovalidateMode: AutovalidateMode.onUserInteraction,
                                decoration: InputDecoration(
                                  labelText: 'Entidad Federativa de Nacimiento *',
                                  errorText: _serverErrors['entidadNacimiento'],
                                  errorMaxLines: 4,
                                ),
                                hint: const Text('Seleccione entidad federativa', overflow: TextOverflow.ellipsis),
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
                                  setState(() => _entidadNacimiento = v);
                                  _limpiarErrorServidor('entidadNacimiento');
                                  _generarCurpYRfcAutomatico(forzar: true);
                                },
                                validator: (v) => (v == null || v.isEmpty)
                                    ? 'Seleccione su entidad federativa'
                                    : null,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),

                        // Estado Civil dinámico desde catálogo (vacío inicialmente)
                        _FilaResponsiva(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(
                              child: DropdownButtonFormField<String>(
                                value: _estadosCivilesCatalogo.any((item) =>
                                        (item['descripcion'] ?? item['codigo']).toString() == _estadoCivil)
                                    ? _estadoCivil
                                    : null,
                                autovalidateMode: AutovalidateMode.onUserInteraction,
                                decoration: InputDecoration(
                                  labelText: 'Estado Civil *',
                                  errorText: _serverErrors['estadoCivil'],
                                  errorMaxLines: 4,
                                ),
                                hint: const Text('Seleccione estado civil', overflow: TextOverflow.ellipsis),
                                isExpanded: true,
                                items: _estadosCivilesCatalogo.map((item) {
                                  final val = (item['descripcion'] ?? item['codigo'] ?? '').toString();
                                  final nom = (item['nombre'] ?? item['descripcion'] ?? val).toString();
                                  return DropdownMenuItem<String>(
                                    value: val,
                                    child: Text(nom, overflow: TextOverflow.ellipsis),
                                  );
                                }).toList(),
                                onChanged: (v) {
                                  setState(() => _estadoCivil = v);
                                  _limpiarErrorServidor('estadoCivil');
                                },
                                validator: (v) =>
                                    (v == null || v.isEmpty) ? 'Seleccione su estado civil' : null,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),

                        // CURP y RFC: 100% editables, no readonly, no disabled, calculados automáticamente
                        _FilaResponsiva(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(
                              child: _campo(
                                controller: _curpController,
                                label: 'CURP (18 caracteres) *',
                                hint: 'AAAA000000HAAAAA00',
                                icon: Icons.fingerprint,
                                errorKey: 'curp',
                                textCapitalization: TextCapitalization.characters,
                                formatters: [
                                  _SinEspaciosNiTabsFormatter(),
                                  FilteringTextInputFormatter.allow(RegExp(r'[a-zA-Z0-9]')),
                                  _crearLimiteFormatter('curp', 18),
                                ],
                                onChanged: (_) {
                                  _curpEditadoManualmente = true;
                                },
                                validator: _validarCurp,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: _campo(
                                controller: _rfcController,
                                label: 'RFC (12 o 13 caracteres) *',
                                hint: 'AAAA000000AAA',
                                icon: Icons.credit_card,
                                errorKey: 'rfc',
                                textCapitalization: TextCapitalization.characters,
                                formatters: [
                                  _SinEspaciosNiTabsFormatter(),
                                  FilteringTextInputFormatter.allow(RegExp(r'[a-zA-Z0-9&Ññ]')),
                                  _crearLimiteFormatter('rfc', 13),
                                ],
                                onChanged: (_) {
                                  _rfcEditadoManualmente = true;
                                },
                                validator: _validarRfc,
                              ),
                            ),
                          ],
                        ),

                        ],

                        // PANTALLA 2: CONTACTO Y SEGURIDAD
                        if (_paso == 1) ...[
                        _buildSectionHeader('2. Contacto y Credenciales de Acceso', Icons.security_outlined),
                        const SizedBox(height: 12),
                        _campo(
                          controller: _correoController,
                          label: 'Correo Electrónico (Usuario de acceso) *',
                          hint: 'ejemplo@correo.com',
                          icon: Icons.email_outlined,
                          errorKey: 'correoElectronico',
                          altErrorKey: 'correo',
                          keyboardType: TextInputType.emailAddress,
                          limpiarEspaciosExtremos: true,
                          formatters: [
                            _SinEspaciosNiTabsFormatter(),
                            _crearLimiteFormatter('correoElectronico', 100),
                          ],
                          validator: _validarCorreo,
                        ),
                        const SizedBox(height: 12),
                        _campo(
                          controller: _telefonoController,
                          label: 'Teléfono Móvil (Exactamente 10 dígitos) *',
                          hint: '5512345678',
                          icon: Icons.phone_android_outlined,
                          errorKey: 'telefonoMovil',
                          keyboardType: TextInputType.phone,
                          formatters: [
                            FilteringTextInputFormatter.digitsOnly,
                            _crearLimiteFormatter('telefonoMovil', 10),
                          ],
                          validator: _validarTelefono,
                        ),
                        const SizedBox(height: 12),
                        _campo(
                          controller: _telefonoAltController,
                          label: 'Teléfono Alternativo (Opcional, 10 dígitos)',
                          hint: '8182345678',
                          icon: Icons.phone_outlined,
                          errorKey: 'telefonoAlternativo',
                          keyboardType: TextInputType.phone,
                          formatters: [
                            FilteringTextInputFormatter.digitsOnly,
                            _crearLimiteFormatter('telefonoAlternativo', 10),
                          ],
                          validator: _vTelAlt,
                        ),
                        const SizedBox(height: 12),
                        _campo(
                          controller: _passwordController,
                          label: 'Contraseña de Acceso *',
                          hint: 'Mínimo 8 car., 1 mayús., 1 núm., 1 especial',
                          icon: Icons.lock_outline,
                          errorKey: 'password',
                          obscureText: _obscurePassword,
                          desactivarAutocompletado: true,
                          formatters: [
                            _SinEspaciosNiTabsFormatter(),
                            _crearLimiteFormatter('password', 50),
                          ],
                          suffixIcon: IconButton(
                            icon: Icon(_obscurePassword ? Icons.visibility_off : Icons.visibility),
                            onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                          ),
                          validator: _validarPassword,
                        ),

                        ],

                        // PANTALLA 3: DOMICILIO
                        if (_paso == 2) ...[
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
                                formatters: [
                                  FilteringTextInputFormatter.digitsOnly,
                                  _crearLimiteFormatter('codigoPostal', 5),
                                ],
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
                                textCapitalization: TextCapitalization.words,
                                limpiarEspaciosExtremos: true,
                                formatters: [
                                  _SoloLetrasEspacioUnicoFormatter(),
                                  _crearLimiteFormatter('pais', 50),
                                ],
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
                                textCapitalization: TextCapitalization.words,
                                limpiarEspaciosExtremos: true,
                                formatters: [
                                  _SoloLetrasEspacioUnicoFormatter(),
                                  _crearLimiteFormatter('estado', 40),
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
                                textCapitalization: TextCapitalization.words,
                                limpiarEspaciosExtremos: true,
                                formatters: [
                                  _SoloLetrasEspacioUnicoFormatter(),
                                  _crearLimiteFormatter('municipio', 60),
                                ],
                                validator: _vMunicipio,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        DropdownButtonFormField<String>(
                          key: ValueKey(
                            'colonia_${_cpController.text.trim()}_${_coloniasDisponibles.length}',
                          ),
                          value: _coloniasDisponibles.contains(_coloniaSeleccionada)
                              ? _coloniaSeleccionada
                              : null,
                          autovalidateMode: AutovalidateMode.onUserInteraction,
                          isExpanded: true,
                          decoration: InputDecoration(
                            labelText: 'Colonia *',
                            prefixIcon: const Icon(Icons.holiday_village_outlined, size: 22),
                            errorText: _serverErrors['colonia'],
                            errorMaxLines: 5,
                            suffixIcon: _cargandoColonias
                                ? const Padding(
                                    padding: EdgeInsets.all(12),
                                    child: SizedBox(
                                      width: 18,
                                      height: 18,
                                      child: CircularProgressIndicator(strokeWidth: 2),
                                    ),
                                  )
                                : (_coloniasDisponibles.isEmpty && _cpController.text.trim().length == 5
                                    ? IconButton(
                                        tooltip: 'Recargar colonias del código postal',
                                        icon: const Icon(Icons.refresh, size: 20),
                                        onPressed: () => _cargarColonias(
                                          cp: _cpController.text,
                                          municipio: _municipioController.text,
                                        ),
                                      )
                                    : null),
                          ),
                          hint: Text(
                            _cargandoColonias
                                ? 'Consultando colonias del municipio...'
                                : (_coloniasDisponibles.isEmpty
                                    ? 'Ingrese un código postal válido para ver colonias'
                                    : 'Seleccione manualmente su colonia'),
                            overflow: TextOverflow.ellipsis,
                          ),
                          items: _coloniasDisponibles.map((colonia) {
                            return DropdownMenuItem<String>(
                              value: colonia,
                              child: Text(colonia, overflow: TextOverflow.ellipsis),
                            );
                          }).toList(),
                          onChanged: (val) {
                            setState(() {
                              _coloniaSeleccionada = val;
                              _coloniaController.text = val ?? '';
                            });
                            _limpiarErrorServidor('colonia');
                          },
                          validator: (val) => _vColonia(val ?? _coloniaController.text),
                        ),
                        const SizedBox(height: 12),
                        _campo(
                          controller: _calleController,
                          label: 'Calle *',
                          hint: 'Avenida Constitucion',
                          icon: Icons.signpost_outlined,
                          errorKey: 'calle',
                          textCapitalization: TextCapitalization.words,
                          limpiarEspaciosExtremos: true,
                          formatters: [
                            _SoloLetrasEspacioUnicoFormatter(),
                            _crearLimiteFormatter('calle', 60),
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
                                  _crearLimiteFormatter('numeroExterior', 6),
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
                                  _crearLimiteFormatter('numeroInterior', 6),
                                ],
                                validator: _vNumInt,
                              ),
                            ),
                          ],
                        ),

                        ],

                        // PANTALLA 4: INFORMACIÓN LABORAL
                        if (_paso == 3) ...[
                        _buildSectionHeader('4. Información Laboral', Icons.work_outline),
                        const SizedBox(height: 12),
                        DropdownButtonFormField<String>(
                          value: _ocupacionSeleccionada,
                          autovalidateMode: AutovalidateMode.onUserInteraction,
                          decoration: InputDecoration(
                            labelText: 'Ocupación *',
                            prefixIcon: const Icon(Icons.badge_outlined, color: AppTheme.primaryColor),
                            errorText: _serverErrors['ocupacion'],
                            errorMaxLines: 4,
                          ),
                          hint: const Text('Seleccione su ocupación', overflow: TextOverflow.ellipsis),
                          isExpanded: true,
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
                            setState(() => _ocupacionSeleccionada = v);
                            _limpiarErrorServidor('ocupacion');
                          },
                          validator: (v) =>
                              (v == null || v.isEmpty) ? 'Seleccione su ocupación' : null,
                        ),
                        const SizedBox(height: 12),
                        _campo(
                          controller: _empresaController,
                          label: 'Empresa *',
                          hint: 'Grupo Finanzas del Norte',
                          icon: Icons.business_outlined,
                          errorKey: 'empresa',
                          limpiarEspaciosExtremos: true,
                          formatters: [
                            _SinTabulacionEspacioUnicoFormatter(),
                            _crearLimiteFormatter('empresa', 100),
                          ],
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
                          limpiarEspaciosExtremos: true,
                          bloquearTab: true,
                          suffixIcon: _buildSpinnerIngreso(),
                          formatters: [
                            _MonedaInputFormatter(),
                            _crearLimiteFormatter('ingresoMensual', 13),
                          ],
                          validator: _validarIngreso,
                        ),

                        ],

                        // PANTALLA 5: CUENTA Y SALDO INICIAL
                        if (_paso == 4) ...[
                        _buildSectionHeader('5. Configuración de Cuenta Bancaria', Icons.account_balance_outlined),
                        const SizedBox(height: 12),
                        _FilaResponsiva(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(
                              child: DropdownButtonFormField<String>(
                                value: _tipoCuenta,
                                autovalidateMode: AutovalidateMode.onUserInteraction,
                                decoration: const InputDecoration(
                                  labelText: 'Tipo de Cuenta *',
                                  errorMaxLines: 4,
                                ),
                                hint: const Text('Seleccione tipo de cuenta', overflow: TextOverflow.ellipsis),
                                isExpanded: true,
                                items: const [
                                  DropdownMenuItem(value: 'DEBITO', child: Text('Débito')),
                                  DropdownMenuItem(value: 'AHORRO', child: Text('Ahorro')),
                                  DropdownMenuItem(value: 'NOMINA', child: Text('Nómina')),
                                  DropdownMenuItem(value: 'CHEQUES', child: Text('Cheques')),
                                ],
                                onChanged: (v) => setState(() => _tipoCuenta = v),
                                validator: (v) =>
                                    (v == null || v.isEmpty) ? 'Seleccione un tipo de cuenta' : null,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: _campo(
                                controller: _saldoInicialController,
                                label: 'Saldo Inicial (MXN) *',
                                hint: 'Mayor o igual a 0.00 (máx. 2 dec.)',
                                icon: Icons.attach_money,
                                errorKey: 'saldoInicial',
                                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                                limpiarEspaciosExtremos: true,
                                formatters: [
                                  _MonedaInputFormatter(),
                                  _crearLimiteFormatter('saldoInicial', 15),
                                ],
                                validator: _validarSaldoInicial,
                              ),
                            ),
                          ],
                        ),

                        ],

                        const SizedBox(height: 28),

                        // PANTALLAS 1-4: botón Siguiente (se habilita con los campos obligatorios válidos de la pantalla actual)
                        // PANTALLA 5: botón actual para crear la cuenta bancaria
                        if (_paso < _totalPasos - 1)
                          CustomButton(
                            text: 'Siguiente',
                            onPressed: _pasoValido(_paso) ? _avanzarPaso : null,
                          )
                        else
                          CustomButton(
                            text: 'Crear Mi Cuenta Bancaria',
                            isLoading: authProvider.status == AuthStatus.loading,
                            onPressed: formHabilitado ? _submitRegistro : null,
                          ),
                        if (!(_paso < _totalPasos - 1 ? _pasoValido(_paso) : formHabilitado) &&
                            authProvider.status != AuthStatus.loading) ...[
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
                          onPressed: _confirmarRegresarInicio,
                          child: const Text('¿Ya tienes cuenta? Regresar al inicio'),
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
      ),
    );
  }

  // --- NAVEGACIÓN POR PANTALLAS (solo avanzar con Siguiente) ---
  static const int _totalPasos = 5;
  int _paso = 0;
  final ScrollController _scrollController = ScrollController();

  bool _pasoValido(int paso) {
    switch (paso) {
      case 0:
        return _validarNombreCompleto(_nombreController.text) == null &&
            _validarNombreOApellido(_apellidoPaternoController.text, 'apellido paterno') == null &&
            _validarNombreOApellido(_apellidoMaternoController.text, 'apellido materno') == null &&
            _validarFechaNacimiento(_fechaNacimientoController.text) == null &&
            _sexo != null &&
            _sexo!.isNotEmpty &&
            _entidadNacimiento != null &&
            _entidadNacimiento!.isNotEmpty &&
            _estadoCivil != null &&
            _estadoCivil!.isNotEmpty &&
            _validarCurp(_curpController.text) == null &&
            _validarRfc(_rfcController.text) == null;
      case 1:
        return _validarCorreo(_correoController.text) == null &&
            _validarTelefono(_telefonoController.text) == null &&
            _vTelAlt(_telefonoAltController.text) == null &&
            _validarPassword(_passwordController.text) == null;
      case 2:
        return !_serverErrors.containsKey('codigoPostal') &&
            _vCp(_cpController.text) == null &&
            _vPais(_paisController.text) == null &&
            _vEstado(_estadoController.text) == null &&
            _vMunicipio(_municipioController.text) == null &&
            _vColonia(_coloniaSeleccionada ?? _coloniaController.text) == null &&
            _vCalle(_calleController.text) == null &&
            _vNumExt(_numeroExtController.text) == null &&
            _vNumInt(_numeroIntController.text) == null;
      case 3:
        return _ocupacionSeleccionada != null &&
            _ocupacionSeleccionada!.isNotEmpty &&
            _vEmpresa(_empresaController.text) == null &&
            _validarIngreso(_ingresoController.text) == null;
      default:
        return _tipoCuenta != null &&
            _tipoCuenta!.isNotEmpty &&
            _validarSaldoInicial(_saldoInicialController.text) == null;
    }
  }

  void _avanzarPaso() {
    _normalizarEspaciosEnControladores();
    if (!(_formKey.currentState?.validate() ?? false) || !_pasoValido(_paso)) {
      setState(() {});
      return;
    }
    if (_paso < _totalPasos - 1) {
      setState(() => _paso++);
      if (_scrollController.hasClients) {
        _scrollController.jumpTo(0);
      }
    }
  }

  // Devuelve la pantalla (0-4) más temprana que contiene un campo con error devuelto por el servidor
  int? _pasoDeErrorServidor(Map<String, String> errores) {
    const pasoPorCampo = <String, int>{
      'nombre': 0, 'apellidoPaterno': 0, 'apellidoMaterno': 0, 'fechaNacimiento': 0,
      'curp': 0, 'rfc': 0, 'sexo': 0, 'entidadNacimiento': 0, 'estadoCivil': 0,
      'correoElectronico': 1, 'correo': 1, 'telefonoMovil': 1, 'telefonoAlternativo': 1, 'password': 1,
      'codigoPostal': 2, 'pais': 2, 'estado': 2, 'municipio': 2, 'colonia': 2,
      'calle': 2, 'numeroExterior': 2, 'numeroInterior': 2,
      'ocupacion': 3, 'empresa': 3, 'ingresoMensual': 3,
      'saldoInicial': 4, 'tipoCuenta': 4,
    };
    int? menor;
    for (final campo in errores.keys) {
      final paso = pasoPorCampo[campo];
      if (paso != null && (menor == null || paso < menor)) {
        menor = paso;
      }
    }
    return menor;
  }

  bool get _hayDatosCapturados {
    final hayTexto = [
      _nombreController,
      _apellidoPaternoController,
      _apellidoMaternoController,
      _fechaNacimientoController,
      _curpController,
      _rfcController,
      _correoController,
      _telefonoController,
      _telefonoAltController,
      _passwordController,
      _cpController,
      _paisController,
      _estadoController,
      _municipioController,
      _coloniaController,
      _calleController,
      _numeroExtController,
      _numeroIntController,
      _empresaController,
      _ingresoController,
      _saldoInicialController,
    ].any((c) => c.text.trim().isNotEmpty);

    final haySeleccion = _sexo != null ||
        _entidadNacimiento != null ||
        _estadoCivil != null ||
        _coloniaSeleccionada != null ||
        _ocupacionSeleccionada != null ||
        _tipoCuenta != null;

    return hayTexto || haySeleccion;
  }

  Future<void> _confirmarRegresarInicio() async {
    if (!_hayDatosCapturados) {
      Navigator.of(context).pop();
      return;
    }
    final confirmar = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        title: const Text('¿Estás seguro de regresar al inicio?'),
        content: const Text('Perderás la información capturada en este registro.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Sí, regresar al inicio'),
          ),
        ],
      ),
    );
    if (confirmar == true && mounted) {
      Navigator.of(context).pop();
    }
  }

  Widget _buildIndicadorProgreso() {
    const etiquetas = ['Datos', 'Contacto', 'Domicilio', 'Laboral', 'Cuenta'];
    return Row(
      children: List.generate(_totalPasos, (i) {
        final completado = i < _paso;
        final actual = i == _paso;
        final color = (completado || actual) ? AppTheme.primaryColor : Colors.grey;
        return Expanded(
          child: Column(
            children: [
              CircleAvatar(
                radius: 13,
                backgroundColor: actual ? AppTheme.primaryColor : (completado ? color : Colors.grey.shade300),
                child: completado
                    ? const Icon(Icons.check, size: 15, color: Colors.white)
                    : Text(
                        '${i + 1}',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: actual ? Colors.white : Colors.grey.shade700,
                        ),
                      ),
              ),
              const SizedBox(height: 4),
              Text(
                etiquetas[i],
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  fontSize: 10,
                  fontWeight: actual ? FontWeight.bold : FontWeight.normal,
                  color: color,
                ),
              ),
            ],
          ),
        );
      }),
    );
  }

  Widget _campo({
    required TextEditingController controller,
    required String label,
    required String hint,
    required IconData icon,
    required String errorKey,
    required String? Function(String?) validator,
    String? altErrorKey,
    TextInputType? keyboardType,
    List<TextInputFormatter>? formatters,
    ValueChanged<String>? onChanged,
    Widget? suffixIcon,
    VoidCallback? onTap,
    bool readOnly = false,
    bool obscureText = false,
    bool desactivarAutocompletado = false,
    bool limpiarEspaciosExtremos = false,
    bool actualizarCurpRfc = false,
    bool bloquearTab = false,
    TextCapitalization textCapitalization = TextCapitalization.none,
  }) {
    final focusNode = _obtenerFocusNode(
      errorKey,
      controller,
      limpiarEspaciosExtremos: limpiarEspaciosExtremos,
      actualizarCurpRfc: actualizarCurpRfc,
      bloquearTab: bloquearTab,
    );
    final avisoLimite = _avisosLimite[errorKey];
    final errorServidor = _serverErrors[errorKey] ??
        (altErrorKey != null ? _serverErrors[altErrorKey] : null);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        TextFormField(
          controller: controller,
          focusNode: focusNode,
          autovalidateMode: AutovalidateMode.onUserInteraction,
          inputFormatters: formatters,
          textCapitalization: textCapitalization,
          readOnly: readOnly,
          onTap: onTap,
          obscureText: obscureText,
          keyboardType: keyboardType ?? TextInputType.text,
          autofillHints: desactivarAutocompletado ? const <String>[] : null,
          enableSuggestions: !desactivarAutocompletado,
          autocorrect: !desactivarAutocompletado,
          enableIMEPersonalizedLearning: !desactivarAutocompletado,
          validator: validator,
          onChanged: (val) {
            _limpiarErrorServidor(errorKey);
            if (altErrorKey != null) {
              _limpiarErrorServidor(altErrorKey);
            }
            if (onChanged != null) onChanged(val);
            setState(() {});
          },
          decoration: InputDecoration(
            labelText: label,
            hintText: hint,
            errorText: errorServidor,
            errorMaxLines: 5,
            helperMaxLines: 3,
            prefixIcon: Icon(icon, size: 22),
            suffixIcon: suffixIcon,
          ),
        ),
        if (avisoLimite != null)
          Padding(
            padding: const EdgeInsets.only(top: 4, left: 12),
            child: Text(
              avisoLimite,
              style: const TextStyle(
                color: AppTheme.errorColor,
                fontSize: 12,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
      ],
    );
  }

  // --- Control numérico (spinner) del Ingreso Mensual: flechas subir / bajar ---
  static const double _ingresoPaso = 500.0;
  static const double _ingresoMaximo = 9999999999.99;

  void _ajustarIngreso(double delta) {
    final actual = double.tryParse(_ingresoController.text.trim()) ?? 0.0;
    var nuevo = actual + delta;
    if (nuevo < 0) nuevo = 0; // nunca negativo
    if (nuevo > _ingresoMaximo) nuevo = _ingresoMaximo; // respeta el máximo existente
    final tieneDecimales = nuevo != nuevo.truncateToDouble();
    final texto = tieneDecimales ? nuevo.toStringAsFixed(2) : nuevo.toStringAsFixed(0);
    setState(() {
      _ingresoController.text = texto;
      _ingresoController.selection = TextSelection.collapsed(offset: texto.length);
      _serverErrors.remove('ingresoMensual');
      _avisosLimite.remove('ingresoMensual');
    });
    _formKey.currentState?.validate();
  }

  Widget _buildSpinnerIngreso() {
    return SizedBox(
      width: 40,
      height: 48,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          InkWell(
            onTap: () => _ajustarIngreso(_ingresoPaso),
            child: const SizedBox(
              height: 22,
              width: 40,
              child: Icon(Icons.keyboard_arrow_up, size: 22, color: AppTheme.primaryColor),
            ),
          ),
          InkWell(
            onTap: () => _ajustarIngreso(-_ingresoPaso),
            child: const SizedBox(
              height: 22,
              width: 40,
              child: Icon(Icons.keyboard_arrow_down, size: 22, color: AppTheme.primaryColor),
            ),
          ),
        ],
      ),
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
