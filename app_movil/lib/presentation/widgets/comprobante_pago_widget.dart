import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import '../../domain/entities/transaccion.dart';

/// Componente visual corporativo para mostrar comprobantes de pago y compra GestoPago
/// alineado con la identidad visual de Banca Digital GestoPago (AppTheme), adaptable a móvil y web.
class ComprobantePagoWidget extends StatelessWidget {
  final Transaccion transaccion;
  final String? fallbackProducto;
  final String? fallbackServicio;
  final VoidCallback onCerrar;

  const ComprobantePagoWidget({
    super.key,
    required this.transaccion,
    required this.onCerrar,
    this.fallbackProducto,
    this.fallbackServicio,
  });

  static Future<void> mostrarModal(
    BuildContext context, {
    required Transaccion transaccion,
    String? fallbackProducto,
    String? fallbackServicio,
    VoidCallback? onClosed,
  }) async {
    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      barrierColor: Colors.black.withOpacity(0.55),
      builder: (ctx) => Dialog(
        backgroundColor: Colors.transparent,
        elevation: 0,
        insetPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 20),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 460),
          child: SingleChildScrollView(
            child: ComprobantePagoWidget(
              transaccion: transaccion,
              fallbackProducto: fallbackProducto,
              fallbackServicio: fallbackServicio,
              onCerrar: () {
                onClosed?.call();
                Navigator.of(ctx).popUntil((route) => route.isFirst);
              },
            ),
          ),
        ),
      ),
    );
  }

  bool get _esAprobado {
    final st = transaccion.estatus.trim().toUpperCase();
    return st == 'APLICADO' || st == 'APROBADO' || st == 'PAGADO';
  }

  bool get _esRechazado {
    final st = transaccion.estatus.trim().toUpperCase();
    return st.startsWith('RECHAZAD');
  }

  String get _nombreProductoEfectivo {
    if (transaccion.nombreProducto != null && transaccion.nombreProducto!.trim().isNotEmpty) {
      return transaccion.nombreProducto!.trim();
    }
    if (fallbackProducto != null && fallbackProducto!.trim().isNotEmpty) {
      return fallbackProducto!.trim();
    }
    if (transaccion.nombreServicio != null && transaccion.nombreServicio!.trim().isNotEmpty) {
      return transaccion.nombreServicio!.trim();
    }
    if (fallbackServicio != null && fallbackServicio!.trim().isNotEmpty) {
      return fallbackServicio!.trim();
    }
    return _tipoOperacionLegible;
  }

  String get _nombreServicioEfectivo {
    if (transaccion.nombreServicio != null && transaccion.nombreServicio!.trim().isNotEmpty) {
      return transaccion.nombreServicio!.trim();
    }
    if (fallbackServicio != null && fallbackServicio!.trim().isNotEmpty) {
      return fallbackServicio!.trim();
    }
    return _nombreProductoEfectivo;
  }

  String get _folioMostrar {
    if (transaccion.id != null) {
      return '#${transaccion.id}';
    }
    return transaccion.folioTransaccion;
  }

  String get _tipoOperacionLegible {
    final raw = transaccion.tipoOperacion.toUpperCase();
    if (raw.contains('RECARGA')) return 'Recarga Telefónica';
    if (raw.contains('SERVICIO')) return 'Pago / Compra de Servicio';
    return transaccion.tipoOperacion;
  }

  String get _etiquetaTipo {
    if (transaccion.idProducto != null) {
      return '$_tipoOperacionLegible • ID ${transaccion.idProducto}';
    }
    return _tipoOperacionLegible;
  }

  String get _fechaLegible {
    final raw = transaccion.fechaOperacion.trim();
    if (raw.isEmpty) return '';
    final sinT = raw.replaceFirst('T', ' ');
    return sinT.contains('.') ? sinT.split('.').first : sinT;
  }

  @override
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;
    final isCompact = screenWidth < 370;
    final horizontalPad = isCompact ? 16.0 : 24.0;

    final String gatewayTexto = transaccion.nombreGateway.trim().isNotEmpty
        ? transaccion.nombreGateway.trim()
        : 'GestoPago Gateway 2.0';

    final String monedaTexto = transaccion.moneda.trim().isNotEmpty
        ? transaccion.moneda.trim().toUpperCase()
        : 'MXN';

    final String montoFormateado = '\$${transaccion.monto.toStringAsFixed(2)} $monedaTexto';

    final String estatusVisual = _esAprobado
        ? 'PAGADO / APROBADO'
        : (_esRechazado ? 'RECHAZADO' : 'PENDIENTE / EN CONCILIACIÓN');

    final Color colorEstado = _esAprobado
        ? AppTheme.successColor
        : (_esRechazado ? AppTheme.errorColor : const Color(0xFFEF6C00));

    final Color bgEstadoSuave = _esAprobado
        ? const Color(0xFFE8F5E9)
        : (_esRechazado ? const Color(0xFFFFEBEE) : const Color(0xFFFFF8E1));

    final Color borderEstadoSuave = _esAprobado
        ? const Color(0xFFA5D6A7)
        : (_esRechazado ? const Color(0xFFEF9A9A) : const Color(0xFFFFE082));

    final IconData iconoEstado = _esAprobado
        ? Icons.check_circle_rounded
        : (_esRechazado ? Icons.cancel_rounded : Icons.schedule_rounded);

    final String subtituloComprobante = _esAprobado
        ? 'Comprobante Oficial de Operación'
        : (_esRechazado ? 'Comprobante de Operación Rechazada' : 'Comprobante en Conciliación');

    final String atencion = transaccion.atencionClientes.trim().isNotEmpty
        ? transaccion.atencionClientes.trim()
        : '800-437-8672';

    final String? numAut = (transaccion.numAutorizacion != null &&
            transaccion.numAutorizacion!.trim().isNotEmpty)
        ? transaccion.numAutorizacion!.trim()
        : null;

    final String? idTxProv = (transaccion.idTxProveedor != null &&
            transaccion.idTxProveedor!.trim().isNotEmpty)
        ? transaccion.idTxProveedor!.trim()
        : null;

    final String destino = transaccion.destinoPago.trim();
    final bool tieneDestino = destino.isNotEmpty && destino != '-';

    return Container(
      decoration: BoxDecoration(
        color: AppTheme.surfaceColor,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.grey.shade200, width: 1),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.14),
            blurRadius: 24,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Encabezado institucional alineado con la paleta de Banca GestoPago (AppTheme.primaryColor -> secondaryColor)
          Container(
            padding: EdgeInsets.symmetric(
              horizontal: horizontalPad,
              vertical: 20,
            ),
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                colors: [AppTheme.primaryColor, AppTheme.secondaryColor],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
            ),
            child: Row(
              children: [
                _buildProviderLogoBadge(),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'BANCA DIGITAL GESTOPAGO',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: isCompact ? 13.5 : 15,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 0.4,
                        ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        '$subtituloComprobante • $gatewayTexto',
                        style: TextStyle(
                          color: Colors.white.withOpacity(0.86),
                          fontSize: 11.5,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),

          // Cuerpo blanco corporativo
          Padding(
            padding: EdgeInsets.fromLTRB(horizontalPad, 20, horizontalPad, 22),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Insignia superior de estado
                Center(
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                    decoration: BoxDecoration(
                      color: bgEstadoSuave,
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(color: borderEstadoSuave),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(iconoEstado, size: 17, color: colorEstado),
                        const SizedBox(width: 6),
                        Text(
                          estatusVisual,
                          style: TextStyle(
                            color: colorEstado,
                            fontSize: 12.5,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 0.3,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 16),

                // Tarjeta central de importe y producto sobre lienzo AppTheme.backgroundColor
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 18),
                  decoration: BoxDecoration(
                    color: AppTheme.backgroundColor,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: Colors.grey.shade200),
                  ),
                  child: Column(
                    children: [
                      const Text(
                        'IMPORTE DE LA OPERACIÓN',
                        style: TextStyle(
                          color: AppTheme.textSecondary,
                          fontSize: 11,
                          fontWeight: FontWeight.w600,
                          letterSpacing: 0.8,
                        ),
                      ),
                      const SizedBox(height: 6),
                      FittedBox(
                        fit: BoxFit.scaleDown,
                        child: Text(
                          montoFormateado,
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            fontSize: 30,
                            fontWeight: FontWeight.w800,
                            color: _esRechazado ? AppTheme.errorColor : AppTheme.primaryColor,
                            decoration: _esRechazado
                                ? TextDecoration.lineThrough
                                : TextDecoration.none,
                            decorationColor: AppTheme.errorColor,
                            decorationThickness: 2.2,
                          ),
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        _nombreProductoEfectivo,
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          color: AppTheme.secondaryColor,
                          fontSize: 15.5,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      if (_nombreServicioEfectivo.toUpperCase() !=
                          _nombreProductoEfectivo.toUpperCase()) ...[
                        const SizedBox(height: 2),
                        Text(
                          'Proveedor: $_nombreServicioEfectivo',
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            color: AppTheme.textSecondary,
                            fontSize: 12,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                      const SizedBox(height: 10),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4.5),
                        decoration: BoxDecoration(
                          color: AppTheme.primaryColor.withOpacity(0.08),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(
                          _etiquetaTipo,
                          style: const TextStyle(
                            color: AppTheme.primaryColor,
                            fontSize: 11.5,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),

                const SizedBox(height: 18),

                // Tabla alineada de detalles de la transacción
                Container(
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: Colors.grey.shade200),
                  ),
                  child: Column(
                    children: [
                      if (_folioMostrar.isNotEmpty)
                        _buildFilaDato(
                          'Folio de Operación',
                          _folioMostrar,
                          valorColor: AppTheme.textPrimary,
                          bold: true,
                        ),
                      if (transaccion.upe.trim().isNotEmpty)
                        _buildFilaDato(
                          'UPC (Llave de Transacción)',
                          transaccion.upe.trim(),
                          valorColor: AppTheme.primaryColor,
                          monospace: true,
                          bold: true,
                        ),
                      if (numAut != null)
                        _buildFilaDato(
                          'No. Autorización GestoPago',
                          numAut,
                          valorColor: colorEstado,
                          bold: true,
                        ),
                      if (idTxProv != null)
                        _buildFilaDato(
                          'ID Tx Proveedor',
                          idTxProv,
                          valorColor: AppTheme.textPrimary,
                          bold: true,
                        ),
                      if (tieneDestino)
                        _buildFilaDato(
                          'Teléfono / Referencia',
                          destino,
                          valorColor: AppTheme.textPrimary,
                          bold: true,
                        ),
                      if (_fechaLegible.isNotEmpty)
                        _buildFilaDato(
                          'Fecha y Hora',
                          _fechaLegible,
                          valorColor: AppTheme.textPrimary,
                        ),
                      _buildFilaDato(
                        'Atención a Clientes',
                        atencion,
                        valorColor: AppTheme.secondaryColor,
                        bold: true,
                      ),
                      _buildFilaDato(
                        'Estado de la Operación',
                        estatusVisual,
                        valorColor: colorEstado,
                        bold: true,
                        mostrarDivisorInferior: false,
                      ),
                    ],
                  ),
                ),

                const SizedBox(height: 16),

                // Mensaje informativo final
                _buildMensajeInferior(atencion),

                const SizedBox(height: 18),

                // Botón principal consistente con AppTheme.elevatedButtonTheme / CustomButton
                SizedBox(
                  width: double.infinity,
                  height: 50,
                  child: ElevatedButton.icon(
                    onPressed: onCerrar,
                    icon: const Icon(Icons.check_circle_outline, size: 19),
                    label: const Text('Cerrar Comprobante'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppTheme.primaryColor,
                      foregroundColor: Colors.white,
                      elevation: 1,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      textStyle: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                        letterSpacing: 0.3,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFilaDato(
    String etiqueta,
    String valor, {
    required Color valorColor,
    bool bold = false,
    bool monospace = false,
    bool mostrarDivisorInferior = true,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        border: mostrarDivisorInferior
            ? Border(bottom: BorderSide(color: Colors.grey.shade100, width: 1))
            : null,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            flex: 5,
            child: Text(
              etiqueta,
              style: const TextStyle(
                color: AppTheme.textSecondary,
                fontSize: 12.5,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            flex: 6,
            child: Text(
              valor,
              textAlign: TextAlign.right,
              style: TextStyle(
                color: valorColor,
                fontSize: 12.8,
                fontFamily: monospace ? 'monospace' : null,
                fontWeight: bold ? FontWeight.w700 : FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMensajeInferior(String atencion) {
    if (_esAprobado) {
      return Container(
        padding: const EdgeInsets.all(13),
        decoration: BoxDecoration(
          color: const Color(0xFFE8F5E9),
          borderRadius: BorderRadius.circular(10),
          border: const Border(
            left: BorderSide(color: AppTheme.successColor, width: 4),
          ),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Icon(
              Icons.verified_user_outlined,
              color: AppTheme.successColor,
              size: 19,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                'Transacción procesada y liquidada exitosamente a través de GestoPago. Conserve este comprobante. Dudas o aclaraciones al $atencion.',
                style: const TextStyle(
                  color: Color(0xFF1B5E20),
                  fontSize: 12,
                  height: 1.42,
                ),
              ),
            ),
          ],
        ),
      );
    }

    if (_esRechazado) {
      final bool sinCargoConfirmado = transaccion.confirmadoSinCargo == true;
      final String textoRechazo = sinCargoConfirmado
          ? 'La operación fue rechazada por el proveedor. No se aplicó ningún cargo a su cuenta. Dudas o aclaraciones al $atencion.'
          : 'La operación fue rechazada. Para cualquier duda o aclaración comuníquese al $atencion.';

      return Container(
        padding: const EdgeInsets.all(13),
        decoration: BoxDecoration(
          color: const Color(0xFFFFEBEE),
          borderRadius: BorderRadius.circular(10),
          border: const Border(
            left: BorderSide(color: AppTheme.errorColor, width: 4),
          ),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Icon(
              Icons.info_outline_rounded,
              color: AppTheme.errorColor,
              size: 19,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                textoRechazo,
                style: const TextStyle(
                  color: Color(0xFFB71C1C),
                  fontSize: 12,
                  height: 1.42,
                ),
              ),
            ),
          ],
        ),
      );
    }

    return Container(
      padding: const EdgeInsets.all(13),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF8E1),
        borderRadius: BorderRadius.circular(10),
        border: const Border(
          left: BorderSide(color: Color(0xFFEF6C00), width: 4),
        ),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(
            Icons.schedule_rounded,
            color: Color(0xFFEF6C00),
            size: 19,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              'Operación en proceso de conciliación ante GestoPago. Conserve este comprobante y consulte su estado antes de reintentar. Atención: $atencion.',
              style: const TextStyle(
                color: Color(0xFFE65100),
                fontSize: 12,
                height: 1.42,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildProviderLogoBadge() {
    final full = '$_nombreServicioEfectivo $_nombreProductoEfectivo'.toUpperCase();

    IconData? brandIcon;
    Color brandColor = const Color(0xFF0F172A);
    String label = '';

    if (full.contains('AT&T') || full.contains('AT-T')) {
      brandIcon = Icons.language;
      brandColor = const Color(0xFF00A8E0);
      label = 'AT&T';
    } else if (full.contains('TELCEL')) {
      brandIcon = Icons.cell_tower;
      brandColor = const Color(0xFF002F6C);
      label = 'TELCEL';
    } else if (full.contains('MOVISTAR')) {
      brandIcon = Icons.signal_cellular_alt;
      brandColor = const Color(0xFF019DF4);
      label = 'MOV';
    } else if (full.contains('CFE') || full.contains('LUZ')) {
      brandIcon = Icons.bolt;
      brandColor = const Color(0xFF007A3D);
      label = 'CFE';
    } else if (full.contains('TELMEX')) {
      brandIcon = Icons.phone_in_talk;
      brandColor = const Color(0xFF005696);
      label = 'TELMEX';
    } else if (full.contains('AMAZON')) {
      brandIcon = Icons.shopping_bag_outlined;
      brandColor = const Color(0xFF232F3E);
      label = 'AMZN';
    } else if (full.contains('NETFLIX')) {
      brandColor = const Color(0xFFE50914);
      label = 'NETFLIX';
    } else if (full.contains('SPOTIFY')) {
      brandIcon = Icons.graphic_eq;
      brandColor = const Color(0xFF1DB954);
      label = 'SPOTIFY';
    } else if (full.contains('TOTALPLAY')) {
      brandIcon = Icons.wifi;
      brandColor = const Color(0xFFE6007E);
      label = 'TPLAY';
    } else if (full.contains('IZZI')) {
      brandColor = const Color(0xFFFF5000);
      label = 'IZZI';
    } else if (full.contains('SKY') || full.contains('DISH')) {
      brandIcon = Icons.satellite_alt;
      brandColor = const Color(0xFF0072C6);
      label = 'SKY';
    } else if (full.contains('AGUA') || full.contains('SACMEX') || full.contains('AGUAKAN') || full.contains('CESPT') || full.contains('SADM')) {
      brandIcon = Icons.water_drop;
      brandColor = const Color(0xFF0091EA);
      label = 'AGUA';
    } else if (full.contains('GAS') || full.contains('NATURGY')) {
      brandIcon = Icons.local_fire_department;
      brandColor = const Color(0xFFFF6D00);
      label = 'GAS';
    } else if (full.contains('XBOX') || full.contains('PLAYSTATION') || full.contains('NINTENDO')) {
      brandIcon = Icons.sports_esports;
      brandColor = const Color(0xFF107C10);
      label = 'GAME';
    } else if (full.contains('ABIB')) {
      brandColor = const Color(0xFF0F172A);
      label = 'ABIB';
    } else {
      final base = _nombreServicioEfectivo.trim().toUpperCase();
      final primeraPalabra = base.split(RegExp(r'\s+')).first;
      label = primeraPalabra.isNotEmpty
          ? (primeraPalabra.length > 5 ? primeraPalabra.substring(0, 5) : primeraPalabra)
          : 'GP';
      brandColor = const Color(0xFF1E3A8A);
    }

    return Container(
      width: 50,
      height: 50,
      padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(13),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.18),
            blurRadius: 6,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Center(
        child: brandIcon != null && label.length <= 4
            ? FittedBox(
                fit: BoxFit.scaleDown,
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(brandIcon, size: 15, color: brandColor),
                    const SizedBox(width: 2),
                    Text(
                      label,
                      style: TextStyle(
                        color: const Color(0xFF0F172A),
                        fontWeight: FontWeight.w900,
                        fontSize: 11.5,
                        letterSpacing: -0.2,
                      ),
                    ),
                  ],
                ),
              )
            : FittedBox(
                fit: BoxFit.scaleDown,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (brandIcon != null)
                      Icon(brandIcon, size: 16, color: brandColor),
                    Text(
                      label,
                      style: TextStyle(
                        color: brandColor,
                        fontWeight: FontWeight.w900,
                        fontSize: 12.5,
                        letterSpacing: 0.3,
                      ),
                    ),
                  ],
                ),
              ),
      ),
    );
  }
}
