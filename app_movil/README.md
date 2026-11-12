# Aplicación Móvil y Web GestoPago (Flutter & Dart)

Proyecto integrador bancario y de pagos de servicios conectado al backend Spring Boot.

## 1. Arquitectura del Proyecto (Clean Architecture)

```
lib/
├── core/                       # Utilidades transversales y configuración
│   ├── constants/              # URLs base, endpoints y claves de almacenamiento
│   ├── network/                # Cliente Dio con interceptores de JWT y 401
│   ├── storage/                # FlutterSecureStorage (Keystore / Keychain)
│   ├── theme/                  # Paleta de colores bancaria y Material 3
│   └── utils/                  # Responsividad para móvil, tablet y web
├── data/                       # Capa de datos e integración externa
│   ├── datasources/            # Llamadas HTTP remotas a endpoints REST
│   ├── models/                 # Modelos JSON y DTOs de transporte
│   └── repositories/           # Implementación de los repositorios de dominio
├── domain/                     # Lógica y entidades de negocio puras
│   ├── entities/               # Entidades de dominio (Cuenta, Producto, Sesión)
│   └── repositories/           # Contratos / Interfaces de repositorios
└── presentation/               # Capa de presentación visual
    ├── screens/                # Pantallas (Login, 2FA, Dashboard, Pagos, Historial)
    ├── state/                  # Gestión de estado reactiva (Provider)
    └── widgets/                # Componentes reutilizables (Botones, Spinners, Modales)
```

## 2. Requerimientos Implementados

1. **Autenticación Compartida con JWT:**
   - Consume `/auth/login` y `/auth/2fa/verificar`.
   - Inyección automática del header `Authorization: Bearer <token>` en todas las peticiones protegidas.
2. **Control de Sesión Única en Servidor:**
   - Detecta si se inició sesión desde otro dispositivo (ej. Web mientras se usaba Móvil) mediante la respuesta 401 del servidor.
   - Despliega de inmediato el modal `SessionConflictDialog`, invalida el almacenamiento seguro y redirige al usuario al inicio de sesión.
3. **Autenticación Multifactor (2FA / OTP):**
   - Pantalla `TwoFactorScreen` con ingreso de 6 dígitos numéricos, contador regresivo de expiración (5 min) y botón de reenvío.
4. **Almacenamiento Seguro:**
   - `flutter_secure_storage` para guardar de forma cifrada el JWT, `sessionId` y usuario.
5. **Experiencia de Usuario (UX/UI):**
   - Diálogo de confirmación comercial con PIN antes de debitar transacciones.
   - Retroalimentación con indicadores de carga y mensajes claros de error (sin pantallas en blanco).
   - Scroll horizontal y vertical adaptativo.
