import 'dart:math';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/responsive_util.dart';
import '../../domain/entities/producto.dart';
import '../../domain/entities/transaccion.dart';
import '../state/banco_provider.dart';
import '../widgets/comprobante_pago_widget.dart';
import '../widgets/confirmation_dialog.dart';
import '../widgets/custom_button.dart';
import '../widgets/custom_text_field.dart';

/// Permite exclusivamente letras (A-Z, a-z) y números (0-9), sin espacios ni símbolos,
/// notificando cuando el usuario intenta escribir caracteres no permitidos.
class _SoloAlfanumericoReferenciaFormatter extends TextInputFormatter {
  final VoidCallback onCaracterNoPermitido;
  final VoidCallback onCaracterValido;

  _SoloAlfanumericoReferenciaFormatter({
    required this.onCaracterNoPermitido,
    required this.onCaracterValido,
  });

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final raw = newValue.text;
    final tieneInvalidos = RegExp(r'[^A-Za-z0-9]').hasMatch(raw);

    if (!tieneInvalidos) {
      if (raw != oldValue.text) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          onCaracterValido();
        });
      }
      return newValue;
    }

    WidgetsBinding.instance.addPostFrameCallback((_) {
      onCaracterNoPermitido();
    });

    final cursor = newValue.selection.end.clamp(0, raw.length);
    final buffer = StringBuffer();
    int nuevoCursor = 0;
    final regPermitido = RegExp(r'[A-Za-z0-9]');

    for (int i = 0; i < raw.length; i++) {
      final ch = raw[i];
      if (regPermitido.hasMatch(ch)) {
        buffer.write(ch);
        if (i < cursor) nuevoCursor++;
      }
    }

    final limpio = buffer.toString();
    return TextEditingValue(
      text: limpio,
      selection: TextSelection.collapsed(
        offset: nuevoCursor.clamp(0, limpio.length),
      ),
    );
  }
}

class PagosScreen extends StatefulWidget {
  const PagosScreen({super.key});

  @override
  State<PagosScreen> createState() => _PagosScreenState();
}

class _PagosScreenState extends State<PagosScreen> {
  final _referenciaController = TextEditingController();
  final _telefonoController = TextEditingController(text: '5512345678');
  final _montoController = TextEditingController();
  final _formKey = GlobalKey<FormState>();

  Producto? _productoSeleccionado;
  Transaccion? _comprobante;
  String? _avisoReferenciaNoPermitida;
  String? _errorProductoSeleccion;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final bp = context.read<BancoProvider>();
      if (bp.productos.isEmpty && !bp.isLoadingProductos) {
        bp.cargarProductos(forzar: true);
      }
    });
  }

  String? _validarReferencia(String? val) {
    if (val == null || val.trim().isEmpty) {
      return 'La referencia es obligatoria';
    }
    if (!RegExp(r'^[A-Za-z0-9]+$').hasMatch(val.trim())) {
      return 'Solo se permiten letras (A-Z, a-z) y números (0-9), sin espacios ni símbolos';
    }
    return null;
  }

  /// Genera un UPE de exactamente 10 caracteres (A-Z y 0-9) que SIEMPRE combina
  /// al menos una letra y al menos un número. Usa un generador criptográfico.
  static String _generarUpe() {
    const letras = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
    const numeros = '0123456789';
    const todos = letras + numeros;
    final rnd = Random.secure();
    final chars = <String>[
      letras[rnd.nextInt(letras.length)],
      numeros[rnd.nextInt(numeros.length)],
      for (int i = 0; i < 8; i++) todos[rnd.nextInt(todos.length)],
    ]..shuffle(rnd);
    return chars.join();
  }

  @override
  void dispose() {
    _referenciaController.dispose();
    _telefonoController.dispose();
    _montoController.dispose();
    super.dispose();
  }

  Widget _buildLogoBadge(Producto prod, {double size = 42}) {
    final s = prod.servicio.toUpperCase();
    final d = prod.descripcion.toUpperCase();
    final full = '$s $d';

    Color bg;
    Color fg = Colors.white;
    IconData icon;
    String badgeText = '';

    if (full.contains('CFE') || full.contains('LUZ')) {
      bg = const Color(0xFF007A3D); // CFE Verde Oficial
      icon = Icons.bolt;
      badgeText = 'CFE';
    } else if (full.contains('TELMEX')) {
      bg = const Color(0xFF005696); // Telmex Azul
      icon = Icons.phone_in_talk;
      badgeText = 'TEL';
    } else if (full.contains('TELCEL')) {
      bg = const Color(0xFF002F6C); // Telcel Azul Marino
      icon = Icons.signal_cellular_alt;
      badgeText = 'TCL';
    } else if (full.contains('MOVISTAR')) {
      bg = const Color(0xFF019DF4); // Movistar Cyan
      icon = Icons.phonelink_ring;
      badgeText = 'MOV';
    } else if (full.contains('AT&T') || full.contains('AT-T')) {
      bg = const Color(0xFF00A8E0); // AT&T Azul
      icon = Icons.cell_tower;
      badgeText = 'ATT';
    } else if (full.contains('AMAZON')) {
      bg = const Color(0xFF232F3E); // Amazon Dark
      icon = Icons.shopping_bag;
      badgeText = 'AMZ';
    } else if (full.contains('NETFLIX')) {
      bg = const Color(0xFFE50914); // Netflix Rojo
      icon = Icons.movie;
      badgeText = 'NFLX';
    } else if (full.contains('SPOTIFY')) {
      bg = const Color(0xFF1DB954); // Spotify Verde
      icon = Icons.music_note;
      badgeText = 'SPOT';
    } else if (full.contains('TOTALPLAY')) {
      bg = const Color(0xFFE6007E); // Totalplay Magenta
      icon = Icons.wifi;
      badgeText = 'TP';
    } else if (full.contains('IZZI')) {
      bg = const Color(0xFFFF5000); // Izzi Naranja
      icon = Icons.tv;
      badgeText = 'IZZI';
    } else if (full.contains('SKY') || full.contains('DISH')) {
      bg = const Color(0xFF0072C6);
      icon = Icons.satellite_alt;
      badgeText = 'SKY';
    } else if (full.contains('AGUA') || full.contains('SACMEX') || full.contains('AGUAKAN') || full.contains('CESPT') || full.contains('SADM') || full.contains('SALTILLO')) {
      bg = const Color(0xFF0091EA); // Agua Azul
      icon = Icons.water_drop;
      badgeText = 'AGUA';
    } else if (full.contains('GAS') || full.contains('NATURGY')) {
      bg = const Color(0xFFFF6D00); // Gas Naranja
      icon = Icons.local_fire_department;
      badgeText = 'GAS';
    } else if (full.contains('PASE') || full.contains('TELEVIA') || full.contains('PEAJE')) {
      bg = const Color(0xFF00897B); // Peaje Verde Azulado
      icon = Icons.directions_car;
      badgeText = 'TAG';
    } else if (full.contains('XBOX') || full.contains('PLAYSTATION') || full.contains('NINTENDO')) {
      bg = const Color(0xFF107C10);
      icon = Icons.sports_esports;
      badgeText = 'GAME';
    } else {
      bg = AppTheme.primaryColor;
      icon = Icons.receipt_long;
      badgeText = s.isNotEmpty ? (s.length >= 3 ? s.substring(0, 3) : s) : 'GP';
    }

    return Container(
      width: size,
      height: size,
      margin: const EdgeInsets.only(right: 12),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(10),
        boxShadow: [
          BoxShadow(
            color: bg.withOpacity(0.3),
            blurRadius: 4,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Stack(
        alignment: Alignment.center,
        children: [
          Icon(icon, size: size * 0.52, color: fg.withOpacity(0.9)),
          Positioned(
            bottom: 2,
            right: 3,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 2.5, vertical: 0.5),
              decoration: BoxDecoration(
                color: Colors.black.withOpacity(0.45),
                borderRadius: BorderRadius.circular(3),
              ),
              child: Text(
                badgeText,
                style: TextStyle(
                  color: Colors.white,
                  fontSize: size * 0.20,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 0.2,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _seleccionarCuentaOrigen(BancoProvider bancoProvider) {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 20, horizontal: 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Seleccionar Cuenta de Pago', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            const SizedBox(height: 12),
            ...bancoProvider.cuentas.map((c) => ListTile(
                  leading: const Icon(Icons.account_balance_wallet, color: AppTheme.primaryColor),
                  title: Text('${c.tipoCuenta} - ${c.numeroCuenta}', style: const TextStyle(fontWeight: FontWeight.bold)),
                  subtitle: Text('Saldo disponible: \$${c.saldo.toStringAsFixed(2)}'),
                  trailing: bancoProvider.cuentaSeleccionada?.id == c.id
                      ? const Icon(Icons.check_circle, color: AppTheme.primaryColor)
                      : null,
                  onTap: () {
                    bancoProvider.seleccionarCuenta(c);
                    Navigator.of(ctx).pop();
                  },
                )),
          ],
        ),
      ),
    );
  }

  void _abrirBuscadorProductos(List<Producto> _) {
    final bpInicial = context.read<BancoProvider>();
    if (bpInicial.productos.isEmpty && !bpInicial.isLoadingProductos) {
      bpInicial.cargarProductos(forzar: true);
    }

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) {
        String query = '';
        String categoriaSeleccionada = 'Todos';

        return StatefulBuilder(
          builder: (context, setModalState) {
            final bancoProvModal = context.watch<BancoProvider>();
            final todosProductos = bancoProvModal.productos;
            final cargandoCatalogo = bancoProvModal.isLoadingProductos;

            final filtrados = todosProductos.where((p) {
              final textoCompleto = '${p.descripcion} ${p.servicio}'.toLowerCase();
              final matchQuery = query.isEmpty || textoCompleto.contains(query.toLowerCase());
              if (!matchQuery) return false;
              if (categoriaSeleccionada == 'Todos') return true;

              if (categoriaSeleccionada == 'Luz / CFE') {
                return textoCompleto.contains('luz') || textoCompleto.contains('cfe') || textoCompleto.contains('electr');
              } else if (categoriaSeleccionada == 'Agua') {
                return textoCompleto.contains('agua') || textoCompleto.contains('sacmex') || textoCompleto.contains('drenaje');
              } else if (categoriaSeleccionada == 'Telefonía') {
                return textoCompleto.contains('tel') || textoCompleto.contains('movil') || textoCompleto.contains('cel') || textoCompleto.contains('att') || textoCompleto.contains('telcel') || textoCompleto.contains('movistar');
              } else if (categoriaSeleccionada == 'Gas') {
                return textoCompleto.contains('gas') || textoCompleto.contains('naturgy');
              } else if (categoriaSeleccionada == 'TV / Cable') {
                return textoCompleto.contains('tv') || textoCompleto.contains('sky') || textoCompleto.contains('cable') || textoCompleto.contains('izzi') || textoCompleto.contains('totalplay') || textoCompleto.contains('dish') || textoCompleto.contains('megacable');
              } else if (categoriaSeleccionada == 'Peaje') {
                return textoCompleto.contains('peaje') || textoCompleto.contains('tag') || textoCompleto.contains('pase') || textoCompleto.contains('viapass');
              }
              return true;
            }).toList();

            return Container(
              height: MediaQuery.of(context).size.height * 0.85,
              decoration: const BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
              ),
              child: Column(
                children: [
                  Container(
                    margin: const EdgeInsets.only(top: 12, bottom: 8),
                    width: 40,
                    height: 4,
                    decoration: BoxDecoration(
                      color: Colors.grey.shade300,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    child: Row(
                      children: [
                        const Icon(Icons.search, color: AppTheme.primaryColor),
                        const SizedBox(width: 8),
                        const Expanded(
                          child: Text(
                            'Catálogo de Productos y Servicios',
                            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                          ),
                        ),
                        IconButton(
                          icon: const Icon(Icons.close),
                          onPressed: () => Navigator.of(context).pop(),
                        ),
                      ],
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                    child: TextField(
                      autofocus: true,
                      decoration: InputDecoration(
                        hintText: 'Buscar por servicio o empresa (ej. CFE, Telmex, Netflix)...',
                        prefixIcon: const Icon(Icons.search),
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
                        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                      ),
                      onChanged: (val) {
                        setModalState(() {
                          query = val;
                        });
                      },
                    ),
                  ),
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    child: Row(
                      children: ['Todos', 'Luz / CFE', 'Agua', 'Telefonía', 'Gas', 'TV / Cable', 'Peaje'].map((cat) {
                        final isSel = categoriaSeleccionada == cat;
                        return Padding(
                          padding: const EdgeInsets.only(right: 8),
                          child: ChoiceChip(
                            label: Text(cat, style: TextStyle(color: isSel ? Colors.white : Colors.black87, fontSize: 12)),
                            selected: isSel,
                            selectedColor: AppTheme.primaryColor,
                            onSelected: (sel) {
                              if (sel) {
                                setModalState(() {
                                  categoriaSeleccionada = cat;
                                });
                              }
                            },
                          ),
                        );
                      }).toList(),
                    ),
                  ),
                  const Divider(height: 1),
                  Expanded(
                    child: cargandoCatalogo
                        ? const Center(
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                CircularProgressIndicator(),
                                SizedBox(height: 14),
                                Text(
                                  'Cargando productos y servicios reales de GestoPago...',
                                  style: TextStyle(color: AppTheme.textSecondary, fontWeight: FontWeight.w500),
                                ),
                              ],
                            ),
                          )
                        : todosProductos.isEmpty
                            ? Center(
                                child: Padding(
                                  padding: const EdgeInsets.all(24),
                                  child: Column(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Icon(Icons.cloud_sync_outlined, size: 48, color: Colors.grey.shade400),
                                      const SizedBox(height: 12),
                                      Text(
                                        bancoProvModal.productosError ?? 'No hay productos cargados actualmente.',
                                        textAlign: TextAlign.center,
                                        style: const TextStyle(color: AppTheme.textSecondary),
                                      ),
                                      const SizedBox(height: 14),
                                      OutlinedButton.icon(
                                        onPressed: () => bancoProvModal.cargarProductos(forzar: true),
                                        icon: const Icon(Icons.refresh),
                                        label: const Text('Actualizar catálogo de GestoPago'),
                                      ),
                                    ],
                                  ),
                                ),
                              )
                            : filtrados.isEmpty
                                ? const Center(
                                    child: Text(
                                      'No se encontraron productos que coincidan con la búsqueda',
                                      style: TextStyle(color: AppTheme.textSecondary),
                                    ),
                                  )
                                : ListView.separated(
                                    itemCount: filtrados.length,
                                    separatorBuilder: (_, __) => const Divider(height: 1),
                                    itemBuilder: (ctx, idx) {
                                      final item = filtrados[idx];
                                      return ListTile(
                                        leading: _buildLogoBadge(item),
                                        title: Text(item.descripcion, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
                                        subtitle: Text('ID: ${item.idProducto} • Servicio: ${item.servicio}', style: const TextStyle(fontSize: 11)),
                                        trailing: Text(
                                          item.monto > 0 ? '\$${item.monto.toStringAsFixed(2)}' : 'Monto abierto',
                                          style: const TextStyle(fontWeight: FontWeight.bold, color: AppTheme.primaryColor),
                                        ),
                                        onTap: () {
                                          setState(() {
                                            _productoSeleccionado = item;
                                            _errorProductoSeleccion = null;
                                            if (item.monto > 0) {
                                              _montoController.text = item.monto.toStringAsFixed(2);
                                            } else {
                                              _montoController.clear();
                                            }
                                          });
                                          Navigator.of(context).pop();
                                        },
                                      );
                                    },
                                  ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  Future<void> _abrirModalConfirmacion() async {
    setState(() => _avisoReferenciaNoPermitida = null);

    if (_productoSeleccionado == null) {
      setState(() {
        _errorProductoSeleccion = 'Debe seleccionar primero un producto o servicio de GestoPago.';
      });
      _formKey.currentState?.validate();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Primero debe seleccionar un producto o servicio antes de realizar la operación.'),
          backgroundColor: AppTheme.errorColor,
        ),
      );
      return;
    }

    setState(() => _errorProductoSeleccion = null);
    if (!_formKey.currentState!.validate()) return;

    final refActual = _referenciaController.text.trim();
    if (refActual.isEmpty || !RegExp(r'^[A-Za-z0-9]+$').hasMatch(refActual)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Debe ingresar una referencia válida (solo letras y números, sin espacios ni símbolos).'),
          backgroundColor: AppTheme.errorColor,
        ),
      );
      return;
    }

    final bancoProvider = context.read<BancoProvider>();
    final cuenta = bancoProvider.cuentaSeleccionada;

    if (cuenta == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Por favor seleccione una cuenta bancaria origen.'),
          backgroundColor: AppTheme.errorColor,
        ),
      );
      return;
    }

    final productoActual = _productoSeleccionado!;
    final monto = productoActual.monto > 0
        ? productoActual.monto
        : (double.tryParse(_montoController.text.trim()) ?? 0.0);
    final servicioNombre = '${productoActual.descripcion} (${productoActual.servicio})';

    // 1. Mostrar diálogo de progreso mientras se genera y envía el PIN dinámico por correo
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => const Center(
        child: Card(
          child: Padding(
            padding: EdgeInsets.all(24.0),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                CircularProgressIndicator(),
                SizedBox(height: 16),
                Text('Enviando PIN dinámico a tu correo...', style: TextStyle(fontWeight: FontWeight.w600)),
              ],
            ),
          ),
        ),
      ),
    );

    final exitoPin = await bancoProvider.solicitarPinTransaccional(
      numCuenta: cuenta.numeroCuenta,
      monto: monto,
      servicio: servicioNombre,
    );

    if (!mounted) return;
    Navigator.of(context).pop(); // Cerrar diálogo de progreso

    if (!exitoPin) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(bancoProvider.errorMessage ?? 'No fue posible despachar el PIN dinámico de autorización a su correo.'),
          backgroundColor: AppTheme.errorColor,
        ),
      );
      return;
    }

    // 2. Abrir diálogo modal de confirmación con el campo para ingresar el PIN recibido en correo
    // UPE de exactamente 10 caracteres combinando letras y números, generado una sola vez por operación
    final upe10 = _generarUpe();
    final mostrarComprobante = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => ConfirmationDialog(
        title: 'Confirmar Transacción Bancaria',
        servicioNombre: servicioNombre,
        numeroCuenta: cuenta.numeroCuenta,
        monto: monto,
        referencia: _referenciaController.text.trim(),
        upe: upe10,
        onResendPin: () async {
          await bancoProvider.solicitarPinTransaccional(
            numCuenta: cuenta.numeroCuenta,
            monto: monto,
            servicio: servicioNombre,
          );
        },
        onConfirm: (pin) async {
          final exito = await bancoProvider.pagarServicio(
            idServicio: productoActual.idServicio,
            idProducto: productoActual.idProducto,
            monto: monto,
            upe: upe10,
            referencia: _referenciaController.text.trim(),
            telefono: _telefonoController.text.trim(),
            pin: pin,
          );

          // Si hubo error de validación previa (ej. PIN incorrecto) y NO existe comprobante del proveedor,
          // permanecer en el diálogo de confirmación mostrando el mensaje de validación.
          if (!exito && bancoProvider.ultimoComprobante == null) {
            throw Exception(bancoProvider.errorMessage ?? 'Error al procesar el pago.');
          }
        },
      ),
    );

    if (!mounted) return;
    if (mostrarComprobante == true && bancoProvider.ultimoComprobante != null) {
      final comprobanteReal = bancoProvider.ultimoComprobante!;
      final fueAprobado = comprobanteReal.estatus.toUpperCase() == 'APLICADO';
      await ComprobantePagoWidget.mostrarModal(
        context,
        transaccion: comprobanteReal,
        fallbackProducto: productoActual.descripcion,
        fallbackServicio: productoActual.servicio,
        onClosed: () {
          if (mounted && fueAprobado) {
            setState(() {
              _comprobante = null;
              _referenciaController.clear();
            });
          }
        },
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final bancoProvider = context.watch<BancoProvider>();
    final cuenta = bancoProvider.cuentaSeleccionada;

    return Scaffold(
      appBar: AppBar(title: const Text('Pago de Servicios y Compras')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: ResponsiveUtil.pagePadding(context),
          child: Center(
            child: ConstrainedBox(
              constraints: BoxConstraints(maxWidth: ResponsiveUtil.contentWidth(context)),
              child: _comprobante != null
                  ? _buildComprobanteExito()
                  : Card(
                      child: Padding(
                        padding: const EdgeInsets.all(24),
                        child: Form(
                          key: _formKey,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              const Text(
                                'Formulario de Pago GestoPago',
                                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                              ),
                              const SizedBox(height: 6),
                              const Text(
                                'Seleccione el servicio o producto e ingrese los datos de referencia',
                                style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                              ),
                              const Divider(height: 28),

                              // Cuenta Origen
                              InkWell(
                                onTap: bancoProvider.cuentas.length > 1
                                    ? () => _seleccionarCuentaOrigen(bancoProvider)
                                    : null,
                                borderRadius: BorderRadius.circular(10),
                                child: Container(
                                  padding: const EdgeInsets.all(12),
                                  decoration: BoxDecoration(
                                    color: Colors.grey.shade50,
                                    borderRadius: BorderRadius.circular(10),
                                    border: Border.all(color: Colors.grey.shade200),
                                  ),
                                  child: Row(
                                    children: [
                                      const Icon(Icons.account_balance_wallet_outlined, color: AppTheme.primaryColor),
                                      const SizedBox(width: 12),
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            Row(
                                              children: [
                                                const Text('Cuenta a debitar:', style: TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
                                                if (bancoProvider.cuentas.length > 1) ...[
                                                  const Spacer(),
                                                  const Text('Cambiar cuenta', style: TextStyle(fontSize: 11, color: AppTheme.primaryColor, fontWeight: FontWeight.bold)),
                                                  const Icon(Icons.arrow_drop_down, size: 16, color: AppTheme.primaryColor),
                                                ],
                                              ],
                                            ),
                                            Text(
                                              cuenta != null
                                                  ? '${cuenta.tipoCuenta} - ${cuenta.numeroCuenta} (Saldo: \$${cuenta.saldo.toStringAsFixed(2)})'
                                                  : 'No hay cuenta seleccionada',
                                              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                                            ),
                                          ],
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                              const SizedBox(height: 18),

                              // Selector y Buscador Interactivo de Catálogo de Productos
                              Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  InkWell(
                                    onTap: () => _abrirBuscadorProductos(bancoProvider.productos),
                                    borderRadius: BorderRadius.circular(10),
                                    child: Container(
                                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                                      decoration: BoxDecoration(
                                        color: Colors.white,
                                        border: Border.all(
                                          color: _errorProductoSeleccion != null
                                              ? AppTheme.errorColor
                                              : (_productoSeleccionado != null ? AppTheme.primaryColor : Colors.grey.shade400),
                                          width: (_errorProductoSeleccion != null || _productoSeleccionado != null) ? 1.5 : 1,
                                        ),
                                        borderRadius: BorderRadius.circular(10),
                                      ),
                                      child: Row(
                                        children: [
                                          _productoSeleccionado != null
                                              ? _buildLogoBadge(_productoSeleccionado!)
                                              : const Icon(Icons.category_outlined, color: AppTheme.primaryColor),
                                          const SizedBox(width: 8),
                                          Expanded(
                                            child: Column(
                                              crossAxisAlignment: CrossAxisAlignment.start,
                                              children: [
                                                Text(
                                                  _productoSeleccionado != null
                                                      ? '${_productoSeleccionado!.servicio} • ID ${_productoSeleccionado!.idProducto}'
                                                      : 'Servicio / Producto GestoPago *',
                                                  style: TextStyle(
                                                    fontSize: 11,
                                                    fontWeight: _productoSeleccionado != null ? FontWeight.w600 : FontWeight.normal,
                                                    color: _productoSeleccionado != null ? AppTheme.secondaryColor : Colors.grey.shade700,
                                                  ),
                                                ),
                                                Text(
                                                  _productoSeleccionado != null
                                                      ? (_productoSeleccionado!.monto > 0
                                                          ? '${_productoSeleccionado!.descripcion} (\$${_productoSeleccionado!.monto.toStringAsFixed(2)} MXN)'
                                                          : '${_productoSeleccionado!.descripcion} (Monto abierto)')
                                                      : (bancoProvider.isLoadingProductos
                                                          ? 'Cargando catálogo de GestoPago...'
                                                          : 'Seleccionar producto o servicio (${bancoProvider.productos.isNotEmpty ? "${bancoProvider.productos.length} disponibles" : "toca para ver catálogo"})...'),
                                                  style: TextStyle(
                                                    fontWeight: _productoSeleccionado != null ? FontWeight.bold : FontWeight.normal,
                                                    color: _productoSeleccionado != null ? Colors.black87 : Colors.grey.shade600,
                                                    fontSize: 13,
                                                  ),
                                                  overflow: TextOverflow.ellipsis,
                                                ),
                                              ],
                                            ),
                                          ),
                                          Container(
                                            padding: const EdgeInsets.all(6),
                                            decoration: BoxDecoration(
                                              color: AppTheme.primaryColor.withOpacity(0.08),
                                              borderRadius: BorderRadius.circular(8),
                                            ),
                                            child: bancoProvider.isLoadingProductos
                                                ? const SizedBox(
                                                    width: 18,
                                                    height: 18,
                                                    child: CircularProgressIndicator(strokeWidth: 2),
                                                  )
                                                : const Icon(Icons.search, color: AppTheme.primaryColor, size: 20),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ),
                                  if (_errorProductoSeleccion != null)
                                    Padding(
                                      padding: const EdgeInsets.only(top: 6, left: 12, right: 4),
                                      child: Text(
                                        _errorProductoSeleccion!,
                                        style: const TextStyle(
                                          color: AppTheme.errorColor,
                                          fontSize: 12,
                                          fontWeight: FontWeight.w500,
                                        ),
                                      ),
                                    ),
                                ],
                              ),
                              const SizedBox(height: 18),

                              // Referencia / UPE
                              Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  CustomTextField(
                                    controller: _referenciaController,
                                    label: 'Referencia / Contrato',
                                    hint: 'Número de servicio o contrato (solo letras y números)',
                                    prefixIcon: Icons.tag,
                                    keyboardType: TextInputType.text,
                                    autovalidateMode: AutovalidateMode.onUserInteraction,
                                    inputFormatters: [
                                      _SoloAlfanumericoReferenciaFormatter(
                                        onCaracterNoPermitido: () {
                                          if (mounted && _avisoReferenciaNoPermitida == null) {
                                            setState(() {
                                              _avisoReferenciaNoPermitida =
                                                  'Carácter no permitido: la referencia solo acepta letras (A-Z, a-z) y números (0-9), sin espacios ni símbolos.';
                                            });
                                          }
                                        },
                                        onCaracterValido: () {
                                          if (mounted && _avisoReferenciaNoPermitida != null) {
                                            setState(() {
                                              _avisoReferenciaNoPermitida = null;
                                            });
                                          }
                                        },
                                      ),
                                    ],
                                    validator: _validarReferencia,
                                  ),
                                  if (_avisoReferenciaNoPermitida != null)
                                    Padding(
                                      padding: const EdgeInsets.only(top: 5, left: 12, right: 4),
                                      child: Text(
                                        _avisoReferenciaNoPermitida!,
                                        style: const TextStyle(
                                          color: AppTheme.errorColor,
                                          fontSize: 12,
                                          fontWeight: FontWeight.w500,
                                        ),
                                      ),
                                    ),
                                ],
                              ),
                              const SizedBox(height: 18),

                              // Teléfono
                              CustomTextField(
                                controller: _telefonoController,
                                label: 'Teléfono Móvil (10 dígitos)',
                                prefixIcon: Icons.phone_android_outlined,
                                keyboardType: TextInputType.phone,
                                validator: (val) {
                                  if (val == null || val.trim().isEmpty) return 'El teléfono es requerido';
                                  if (!RegExp(r'^\d{10}$').hasMatch(val.trim())) return 'Debe contener exactamente 10 dígitos';
                                  return null;
                                },
                              ),
                              const SizedBox(height: 18),

                              // Monto a pagar
                              CustomTextField(
                                controller: _montoController,
                                label: 'Monto a Pagar (MXN)',
                                hint: _productoSeleccionado == null
                                    ? 'Seleccione primero un producto o servicio'
                                    : 'Ingrese el importe en MXN',
                                prefixIcon: Icons.attach_money,
                                readOnly: _productoSeleccionado != null && _productoSeleccionado!.monto > 0,
                                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                                validator: (val) {
                                  if (_productoSeleccionado == null) return 'Seleccione primero un producto o servicio';
                                  if (val == null || val.trim().isEmpty) return 'El monto es obligatorio';
                                  final num = double.tryParse(val.trim());
                                  if (num == null || num <= 0) return 'El monto debe ser mayor a 0';
                                  return null;
                                },
                              ),
                              const SizedBox(height: 28),

                              CustomButton(
                                text: 'Continuar con el Pago',
                                icon: Icons.arrow_forward_rounded,
                                isLoading: bancoProvider.isLoading,
                                onPressed: _abrirModalConfirmacion,
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

  Widget _buildComprobanteExito() {
    if (_comprobante == null) return const SizedBox.shrink();
    return ComprobantePagoWidget(
      transaccion: _comprobante!,
      fallbackProducto: _productoSeleccionado?.descripcion,
      fallbackServicio: _productoSeleccionado?.servicio,
      onCerrar: () {
        Navigator.of(context).popUntil((route) => route.isFirst);
      },
    );
  }
}
