import 'dart:math';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/responsive_util.dart';
import '../../domain/entities/producto.dart';
import '../state/banco_provider.dart';
import '../widgets/confirmation_dialog.dart';
import '../widgets/custom_button.dart';
import '../widgets/custom_text_field.dart';

class PagosScreen extends StatefulWidget {
  const PagosScreen({super.key});

  @override
  State<PagosScreen> createState() => _PagosScreenState();
}

class _PagosScreenState extends State<PagosScreen> {
  final _referenciaController = TextEditingController();
  final _telefonoController = TextEditingController(text: '5512345678');
  final _montoController = TextEditingController(text: '150.00');
  final _formKey = GlobalKey<FormState>();

  Producto? _productoSeleccionado;
  Map<String, dynamic>? _comprobante;

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

  void _abrirBuscadorProductos(List<Producto> todosProductos) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) {
        String query = '';
        String categoriaSeleccionada = 'Todos';

        return StatefulBuilder(
          builder: (context, setModalState) {
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
                    child: filtrados.isEmpty
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
                                    if (item.monto > 0) {
                                      _montoController.text = item.monto.toStringAsFixed(2);
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
    if (!_formKey.currentState!.validate()) return;

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

    final monto = double.tryParse(_montoController.text.trim()) ?? 0.0;
    final servicioNombre = _productoSeleccionado?.descripcion ?? 'Pago de Servicio GestoPago';

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
    showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => ConfirmationDialog(
        title: 'Confirmar Transacción Bancaria',
        servicioNombre: servicioNombre,
        numeroCuenta: cuenta.numeroCuenta,
        monto: monto,
        referencia: _referenciaController.text.trim().isNotEmpty
            ? _referenciaController.text.trim()
            : _telefonoController.text.trim(),
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
            idServicio: _productoSeleccionado?.idServicio ?? 1,
            idProducto: _productoSeleccionado?.idProducto ?? 101,
            monto: monto,
            upe: upe10,
            referencia: _referenciaController.text.trim(),
            telefono: _telefonoController.text.trim(),
            pin: pin,
          );

          if (!exito) {
            throw Exception(bancoProvider.errorMessage ?? 'Error al procesar el pago.');
          }

          setState(() {
            _comprobante = {
              'servicio': servicioNombre,
              'cuenta': cuenta.numeroCuenta,
              'monto': monto,
              'fecha': DateTime.now().toString().substring(0, 19),
              'autorizacion': 'GP-${DateTime.now().millisecondsSinceEpoch.toString().substring(5)}',
            };
          });
        },
      ),
    );
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
                              InkWell(
                                onTap: () => _abrirBuscadorProductos(bancoProvider.productos),
                                borderRadius: BorderRadius.circular(10),
                                child: Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                                  decoration: BoxDecoration(
                                    color: Colors.white,
                                    border: Border.all(
                                      color: _productoSeleccionado != null ? AppTheme.primaryColor : Colors.grey.shade400,
                                      width: _productoSeleccionado != null ? 1.5 : 1,
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
                                                  ? 'Servicio seleccionado:'
                                                  : 'Servicio / Producto GestoPago',
                                              style: TextStyle(
                                                fontSize: 11,
                                                color: _productoSeleccionado != null ? AppTheme.textSecondary : Colors.grey.shade700,
                                              ),
                                            ),
                                            Text(
                                              _productoSeleccionado != null
                                                  ? '${_productoSeleccionado!.descripcion} (\$${_productoSeleccionado!.monto.toStringAsFixed(2)})'
                                                  : 'Buscar o seleccionar en catálogo (900+ servicios)...',
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
                                        child: const Icon(Icons.search, color: AppTheme.primaryColor, size: 20),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                              const SizedBox(height: 18),

                              // Referencia / UPE
                              CustomTextField(
                                controller: _referenciaController,
                                label: 'Referencia / Contrato',
                                hint: 'Número de servicio o contrato',
                                prefixIcon: Icons.tag,
                                keyboardType: TextInputType.text,
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
                                prefixIcon: Icons.attach_money,
                                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                                validator: (val) {
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
    return Card(
      elevation: 3,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const CircleAvatar(
              radius: 36,
              backgroundColor: Color(0xFFE8F5E9),
              child: Icon(Icons.check_circle_rounded, color: AppTheme.successColor, size: 48),
            ),
            const SizedBox(height: 16),
            const Text(
              '¡Pago Exitoso!',
              style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: AppTheme.successColor),
            ),
            const SizedBox(height: 4),
            const Text('Transacción autorizada y aplicada en GestoPago', style: TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
            const Divider(height: 32),
            _buildReciboRow('Servicio:', _comprobante!['servicio']),
            _buildReciboRow('Cuenta Debitada:', _comprobante!['cuenta']),
            _buildReciboRow('Importe:', '\$${(_comprobante!['monto'] as double).toStringAsFixed(2)} MXN'),
            _buildReciboRow('Folio Autorización:', _comprobante!['autorizacion']),
            _buildReciboRow('Fecha / Hora:', _comprobante!['fecha']),
            const SizedBox(height: 28),
            CustomButton(
              text: 'Realizar Otro Pago',
              icon: Icons.refresh,
              onPressed: () {
                setState(() {
                  _comprobante = null;
                  _referenciaController.clear();
                });
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildReciboRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
          Text(value, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
        ],
      ),
    );
  }
}
