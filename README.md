# Proyecto Servicios Empresa

Microservicio backend desarrollado con **Spring Boot 3.3.6** y **Java 17/21**, gestionado mediante **Gradle**.

## Características y Módulos

### 1. Gestión de Personas
Servicio REST para operaciones básicas sobre la entidad `Personas`:
- `POST /personas`: Registrar nueva persona.
- `PUT /personasActualiza`: Actualizar datos de persona.
- `PUT /personasElimina`: Eliminar persona existente.

### 2. Integración GestoPago
- Cliente HTTP declarativo con **Spring Cloud OpenFeign** (`GestoPagoAuthClient`) para autenticación.
- Tarea programada (`@Scheduled`) que ejecuta la renovación periódica de tokens y su almacenamiento persistente en base de datos.
- Mapeo automatizado mediante **MapStruct** y auditoría de timestamps con `@PrePersist` / `@PreUpdate`.

### 3. Persistencia y Migraciones
- **Spring Data JPA** con **Hibernate** y pool de conexiones **HikariCP**.
- Migraciones de esquema automatizadas con **Flyway** (`db/migration`).
- Base de datos relacional objetivo: **PostgreSQL**.

---

## Requisitos Previos

- **Java JDK**: 17 o 21 (si se usa JDK 21+, asegurar versión de Lombok compatible >= 1.18.30)
- **Gradle**: 8.x / 9.x (incluido mediante wrapper `./gradlew`)
- **PostgreSQL**: Instancia accesible

---

## Configuración (`application.properties`)

Antes de ejecutar, asegúrate de configurar las propiedades necesarias en `src/main/resources/application.properties`:

```properties
# Base de datos
spring.datasource.url=jdbc:postgresql://localhost:5432/tu_base_datos
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contraseña

# GestoPago Auth
gestopago.auth.url=https://api.gestopago.com
gestopago.auth.id-distribuidor=12345
gestopago.auth.codigo-dispositivo=DISP001
gestopago.auth.password=tu_password_gestopago
gestopago.auth.refresh-rate-ms=3600000

# Flyway
spring.flyway.locations=classpath:db/migration
spring.flyway.schemas=public
```

---

## Ejecución y Compilación

Para compilar el proyecto:
```bash
./gradlew build -x test
```

Para ejecutar la aplicación localmente:
```bash
./gradlew bootRun
```

Documentación Swagger / OpenAPI disponible en:
```
http://localhost:8081/swagger-ui.html
```
