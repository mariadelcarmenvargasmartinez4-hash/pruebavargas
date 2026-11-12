import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/responsive_util.dart';
import '../state/auth_provider.dart';
import '../state/banco_provider.dart';
import '../widgets/loading_spinner.dart';
import 'historial_screen.dart';
import 'login_screen.dart';
import 'pagos_screen.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<BancoProvider>().cargarDatosDashboard();
    });
  }

  Future<void> _logout() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cerrar Sesión'),
        content: const Text('¿Desea cerrar su sesión en el servidor bancario?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.errorColor),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Cerrar Sesión'),
          ),
        ],
      ),
    );

    if (confirm == true && mounted) {
      await context.read<AuthProvider>().logout();
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => const LoginScreen()),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();
    final bancoProvider = context.watch<BancoProvider>();
    final session = authProvider.session;
    final isDesktop = ResponsiveUtil.isDesktop(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Banca GestoPago'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Actualizar saldos',
            onPressed: () => bancoProvider.cargarDatosDashboard(),
          ),
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: 'Cerrar sesión',
            onPressed: _logout,
          ),
        ],
      ),
      body: bancoProvider.isLoading && bancoProvider.cuentas.isEmpty
          ? const LoadingSpinner(message: 'Cargando información financiera...')
          : RefreshIndicator(
              onRefresh: () => bancoProvider.cargarDatosDashboard(),
              child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: ResponsiveUtil.pagePadding(context),
                child: Center(
                  child: ConstrainedBox(
                    constraints: BoxConstraints(maxWidth: ResponsiveUtil.contentWidth(context)),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // Tarjeta de Bienvenida y Sesión Única
                        Card(
                          color: AppTheme.primaryColor,
                          child: Padding(
                            padding: const EdgeInsets.all(20),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                  children: [
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          const Text(
                                            'Bienvenido(a),',
                                            style: TextStyle(color: Colors.white70, fontSize: 13),
                                          ),
                                          Text(
                                            session?.nombreCompleto.isNotEmpty == true
                                                ? session!.nombreCompleto
                                                : session?.username ?? 'Usuario',
                                            style: const TextStyle(
                                              color: Colors.white,
                                              fontSize: 20,
                                              fontWeight: FontWeight.bold,
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                    Container(
                                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                                      decoration: BoxDecoration(
                                        color: Colors.white.withOpacity(0.18),
                                        borderRadius: BorderRadius.circular(20),
                                      ),
                                      child: Row(
                                        mainAxisSize: MainAxisSize.min,
                                        children: [
                                          const Icon(Icons.shield_outlined, color: Colors.white, size: 16),
                                          const SizedBox(width: 4),
                                          Text(
                                            session?.dispositivo ?? 'MOVIL',
                                            style: const TextStyle(
                                              color: Colors.white,
                                              fontSize: 12,
                                              fontWeight: FontWeight.bold,
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 14),
                                Row(
                                  children: [
                                    const Icon(Icons.lock_clock_outlined, color: Colors.white60, size: 14),
                                    const SizedBox(width: 6),
                                    Expanded(
                                      child: Text(
                                        'Sesión respaldada única: ${session?.sessionId.isNotEmpty == true ? session!.sessionId.substring(0, 13) : "Activa"}...',
                                        style: const TextStyle(color: Colors.white60, fontSize: 11),
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        ),
                        const SizedBox(height: 24),

                        // Sección: Mis Cuentas Bancarias
                        const Text(
                          'Mis Cuentas Bancarias',
                          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 12),

                        if (bancoProvider.cuentas.isEmpty)
                          Card(
                            child: Padding(
                              padding: const EdgeInsets.all(24),
                              child: Center(
                                child: Text(
                                  bancoProvider.errorMessage ?? 'No se encontraron cuentas activas.',
                                  style: const TextStyle(color: AppTheme.textSecondary),
                                ),
                              ),
                            ),
                          )
                        else
                          SizedBox(
                            height: 160,
                            child: ListView.builder(
                              scrollDirection: Axis.horizontal,
                              itemCount: bancoProvider.cuentas.length,
                              itemBuilder: (ctx, index) {
                                final cuenta = bancoProvider.cuentas[index];
                                final isSelected = bancoProvider.cuentaSeleccionada?.id == cuenta.id;

                                return InkWell(
                                  onTap: () => bancoProvider.seleccionarCuenta(cuenta),
                                  borderRadius: BorderRadius.circular(16),
                                  child: Container(
                                    width: isDesktop ? 320 : 270,
                                    margin: const EdgeInsets.only(right: 14),
                                    padding: const EdgeInsets.all(18),
                                    decoration: BoxDecoration(
                                      gradient: isSelected
                                          ? const LinearGradient(
                                              colors: [AppTheme.primaryColor, AppTheme.secondaryColor],
                                              begin: Alignment.topLeft,
                                              end: Alignment.bottomRight,
                                            )
                                          : null,
                                      color: isSelected ? null : Colors.white,
                                      borderRadius: BorderRadius.circular(16),
                                      border: Border.all(
                                        color: isSelected ? Colors.transparent : Colors.grey.shade300,
                                      ),
                                      boxShadow: [
                                        BoxShadow(
                                          color: Colors.black.withOpacity(0.04),
                                          blurRadius: 8,
                                          offset: const Offset(0, 4),
                                        ),
                                      ],
                                    ),
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                      children: [
                                        Row(
                                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                          children: [
                                            Text(
                                              cuenta.tipoCuenta,
                                              style: TextStyle(
                                                color: isSelected ? Colors.white70 : AppTheme.textSecondary,
                                                fontSize: 12,
                                                fontWeight: FontWeight.bold,
                                              ),
                                            ),
                                            Text(
                                              cuenta.estatus,
                                              style: TextStyle(
                                                color: isSelected ? Colors.lightGreenAccent : AppTheme.successColor,
                                                fontSize: 11,
                                                fontWeight: FontWeight.bold,
                                              ),
                                            ),
                                          ],
                                        ),
                                        Column(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              '\$${cuenta.saldo.toStringAsFixed(2)} ${cuenta.moneda}',
                                              style: TextStyle(
                                                color: isSelected ? Colors.white : AppTheme.primaryColor,
                                                fontSize: 22,
                                                fontWeight: FontWeight.bold,
                                              ),
                                            ),
                                            const SizedBox(height: 4),
                                            Text(
                                              'Cuenta: ${cuenta.numeroCuenta}',
                                              style: TextStyle(
                                                color: isSelected ? Colors.white70 : AppTheme.textSecondary,
                                                fontSize: 12,
                                              ),
                                            ),
                                          ],
                                        ),
                                      ],
                                    ),
                                  ),
                                );
                              },
                            ),
                          ),

                        const SizedBox(height: 28),

                        // Sección: Accesos Rápidos Comerciales
                        const Text(
                          'Operaciones Comerciales',
                          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 12),

                        Row(
                          children: [
                            Expanded(
                              child: _buildActionCard(
                                title: 'Pagar Servicios',
                                subtitle: 'Luz, Agua, Telefonía',
                                icon: Icons.receipt_long_outlined,
                                color: const Color(0xFF0077B6),
                                onTap: () {
                                  Navigator.of(context).push(
                                    MaterialPageRoute(builder: (_) => const PagosScreen()),
                                  );
                                },
                              ),
                            ),
                            const SizedBox(width: 14),
                            Expanded(
                              child: _buildActionCard(
                                title: 'Historial y Cuentas',
                                subtitle: 'Detalle de Movimientos',
                                icon: Icons.history_rounded,
                                color: const Color(0xFF2E7D32),
                                onTap: () {
                                  Navigator.of(context).push(
                                    MaterialPageRoute(builder: (_) => const HistorialScreen()),
                                  );
                                },
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
    );
  }

  Widget _buildActionCard({
    required String title,
    required String subtitle,
    required IconData icon,
    required Color color,
    required VoidCallback onTap,
  }) {
    return Card(
      elevation: 2,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              CircleAvatar(
                radius: 22,
                backgroundColor: color.withOpacity(0.12),
                child: Icon(icon, color: color, size: 24),
              ),
              const SizedBox(height: 12),
              Text(
                title,
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
              ),
              const SizedBox(height: 4),
              Text(
                subtitle,
                style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
