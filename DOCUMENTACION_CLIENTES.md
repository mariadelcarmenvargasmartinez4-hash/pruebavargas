# Clientes, cuentas y autenticacion

## Persistencia

Flyway crea las tablas al iniciar la aplicacion. La migracion principal del modulo es `src/main/resources/db/migration/V2__create_cliente_cuenta_tables.sql`.

```mermaid
erDiagram
    CLIENTES ||--|| DOMICILIOS : tiene
    CLIENTES ||--o{ CUENTAS : posee
    CLIENTES ||--|| USUARIOS_ACCESO : autentica

    CLIENTES {
        BIGINT id PK
        VARCHAR curp UK
        VARCHAR rfc UK
        VARCHAR correo_electronico UK
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
```

Se usan claves `BIGSERIAL`, importes `DECIMAL`, restricciones `UNIQUE` para identificadores/correos/cuentas y llaves foraneas para las relaciones. Las bajas son logicas; al desactivar un cliente se inactivan sus cuentas y usuario.

## API

`POST /clientes` registra al cliente, domicilio, cuenta y usuario en una transaccion. La cuenta queda activa y su saldo se toma de `banco.cuenta.saldo-inicial-default` (500.00 por defecto). La contrasena no se devuelve y se persiste con BCrypt.

`GET /clientes` consulta o filtra por nombre, apellidos, CURP, RFC, correo, clientes activos o rango de fechas. `GET /clientes/{id}` consulta por ID. `PATCH` y `PUT /clientes/{id}` actualizan sin modificar CURP/RFC; `DELETE /clientes/{id}` realiza la baja logica.

`POST /auth/login` recibe correo y contrasena y entrega un JWT. Las rutas restantes requieren `Authorization: Bearer <token>`. Cuentas permiten creacion, consulta por numero/cliente/estatus, consulta de saldo y actualizacion parcial mediante `/cuentas`.

`GET /usuarios/filtro?correo={correo}&activo={true|false}` filtra usuarios y `PUT /usuarios/agregar` crea credenciales para un cliente existente sin usuario. Ambos endpoints requieren rol `ADMIN`; los usuarios normales tienen rol `CLIENTE`. La migracion `V3__add_usuario_roles.sql` agrega el rol y su restriccion.

Para preparar el primer administrador, registra primero un cliente por `POST /clientes`, entra a PostgreSQL y asigna el rol a su correo:

```sql
UPDATE usuarios_acceso SET rol = 'ADMIN' WHERE username = '<correo registrado>';
```

Despues inicia sesion nuevamente para emitir/usar el JWT con el rol vigente.

## Preparar PostgreSQL en Windows

Con PostgreSQL instalado y `createdb` disponible en PATH, crear la base; el cliente pedira la contrasena de PostgreSQL de forma interactiva:

```powershell
createdb -U postgres -h localhost db_prueba
```

Desde la carpeta `prueba`, configurar la contrasena sin escribirla en el historial, generar una llave aleatoria para JWT y arrancar. Flyway creara las tablas:

```powershell
$securePassword = Read-Host "Contrasena de PostgreSQL" -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
}
$randomBytes = New-Object byte[] 32
$randomGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
$randomGenerator.GetBytes($randomBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($randomBytes)
$randomGenerator.Dispose()
.\gradlew.bat bootRun
```

La API escucha en `http://localhost:8088`; Swagger queda disponible en `http://localhost:8088/swagger-ui/index.html`. Las variables de entorno solo duran en esa ventana de PowerShell.

## Pruebas

```powershell
.\gradlew.bat test
```
