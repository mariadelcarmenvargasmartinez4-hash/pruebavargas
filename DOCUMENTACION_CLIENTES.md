# Clientes, Cuentas, Catálogos, Métricas y Autenticación

## Persistencia y Arquitectura de Catálogos

Flyway administra las tablas al iniciar la aplicación mediante las migraciones:
- `V1__create_gestopago_tokens.sql`
- `V2__create_cliente_cuenta_tables.sql`
- `V3__add_usuario_roles.sql`
- `V4__create_catalogos_tables.sql` (Catálogos desacoplados para Sexo, Nacionalidad y Estado Civil)

```mermaid
erDiagram
    CLIENTES ||--|| DOMICILIOS : tiene
    CLIENTES ||--o{ CUENTAS : posee
    CLIENTES ||--|| USUARIOS_ACCESO : autentica
    CATALOGO_SEXOS ||--o{ CLIENTES : clasifica
    CATALOGO_NACIONALIDADES ||--o{ CLIENTES : identifica
    CATALOGO_ESTADOS_CIVILES ||--o{ CLIENTES : asigna

    CLIENTES {
        BIGINT id PK
        VARCHAR curp UK
        VARCHAR rfc UK
        VARCHAR correo_electronico UK
        VARCHAR sexo
        VARCHAR nacionalidad
        VARCHAR estado_civil
        BOOLEAN activo
        DECIMAL ingreso_mensual
    }
    DOMICILIOS {
        BIGINT id PK
        BIGINT cliente_id FK_UK
        VARCHAR calle
        VARCHAR codigo_postal
    }
    CUENTAS {
        BIGINT id PK
        BIGINT cliente_id FK
        VARCHAR numero_cuenta UK
        VARCHAR clabe UK
        DECIMAL saldo
        VARCHAR estatus
    }
    USUARIOS_ACCESO {
        BIGINT id PK
        BIGINT cliente_id FK_UK
        VARCHAR username UK
        VARCHAR password_hash
        VARCHAR rol
        BOOLEAN activo
    }
    CATALOGO_SEXOS {
        BIGINT id PK
        VARCHAR codigo UK
        VARCHAR descripcion UK
        BOOLEAN activo
    }
    CATALOGO_NACIONALIDADES {
        BIGINT id PK
        VARCHAR codigo UK
        VARCHAR descripcion UK
        BOOLEAN activo
    }
    CATALOGO_ESTADOS_CIVILES {
        BIGINT id PK
        VARCHAR codigo UK
        VARCHAR descripcion UK
        BOOLEAN activo
    }
```

Se usan claves `BIGSERIAL`, importes `DECIMAL`, restricciones `UNIQUE` e índices en campos de búsqueda frecuente. Las bajas son lógicas: al desactivar un cliente se inactivan en cascada sus cuentas bancarias y usuario de acceso.

---

## Robustecimiento de Tipos de Datos y Reglas Estrictas

1. **Tipificación y Campos Alfanuméricos Estrictos:**
   - CURP (18 caracteres alfanuméricos cerrados con expresión regular oficial).
   - RFC (12 o 13 caracteres alfanuméricos cerrados con expresión regular oficial).
   - Teléfono móvil (estrictamente 10 dígitos numéricos).
   - Código Postal (estrictamente 5 dígitos numéricos).
   - Ingresos y Saldos en `BigDecimal` mayor a cero y no negativos.

2. **Validación de Nombres y Apellidos:**
   - Longitud mínima de 2 caracteres reales.
   - Prohibido iniciar o terminar con espacios en blanco.
   - Prohibidos múltiples espacios consecutivos internos: `^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+( [a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+)*$`.

3. **Seguridad de Contraseñas:**
   - Mínimo 8 caracteres, al menos una mayúscula, una minúscula, un número y un carácter especial sin espacios.
   - Almacenamiento seguro mediante hash BCrypt (factor 10).

4. **Eliminación de Estructuras Condicionales Anidadas:**
   - Cero bloques `try-catch` anidados y cero sentencias `if` anidadas.
   - Sustitución de validaciones manuales por validación declarativa (`@NotBlank`, `@Size`, `@Pattern`, `@DecimalMin`, `@Past`).
   - Métodos atómicos y programación funcional con `Optional.ofNullable(...).filter(...)`.
   - Manejo centralizado de errores mediante `@RestControllerAdvice` en `GlobalExceptionHandler`.

5. **Métricas de Texto y Alfanuméricos:**
   - Servicio utilitario `MetricasTextoUtil` que desglosa con exactitud el total de caracteres, letras, dígitos, espacios y caracteres especiales.
   - Endpoint dedicado: `GET /api/utilidades/metricas-texto?campo=curp&valor=...` y `POST /api/utilidades/metricas-texto`.

6. **Arquitectura Basada en Catálogos y APIs Dedicadas:**
   - Desacoplamiento total de valores fijos en duro para Sexo, Nacionalidad y Estado Civil.
   - Limpieza automática de espacios en blanco mediante `StringTrimConfig` en Jackson.
   - APIs REST dedicadas:
     - `GET /api/catalogos/sexos` (público para lectura; creación/edición con rol `ADMIN`)
     - `GET /api/catalogos/nacionalidades` (público para lectura; creación/edición con rol `ADMIN`)
     - `GET /api/catalogos/estados-civiles` (público para lectura; creación/edición con rol `ADMIN`)

---

## Endpoints Principales

| Método | Endpoint | Descripción | Seguridad |
| :--- | :--- | :--- | :--- |
| `POST` | `/clientes` | Registro transaccional de cliente, domicilio, cuenta y usuario | Público |
| `GET` | `/clientes` | Consulta de clientes con filtros | JWT |
| `GET` | `/clientes/{id}` | Consulta de cliente por ID | JWT |
| `PATCH` | `/clientes/{id}` | Actualización parcial protegida | JWT |
| `DELETE` | `/clientes/{id}` | Baja lógica en cascada | JWT |
| `POST` | `/auth/login` | Autenticación y emisión de token JWT | Público |
| `GET` | `/cuentas/{numero}` | Consulta de cuenta bancaria | JWT |
| `GET` | `/cuentas/{numero}/saldo` | Consulta de saldo disponible | JWT |
| `GET` | `/api/catalogos/sexos` | Consulta catálogo de sexos | Público |
| `GET` | `/api/catalogos/nacionalidades` | Consulta catálogo de nacionalidades | Público |
| `GET` | `/api/catalogos/estados-civiles` | Consulta catálogo de estados civiles | Público |
| `GET` | `/api/utilidades/metricas-texto` | Métricas de caracteres (letras, dígitos) | Público |
| `GET` | `/usuarios/filtro` | Filtro administrativo de usuarios | `ROLE_ADMIN` |
| `PUT` | `/usuarios/agregar` | Creación manual de usuario | `ROLE_ADMIN` |

---

## Ejecución y Pruebas

```powershell
.\gradlew.bat test
```
