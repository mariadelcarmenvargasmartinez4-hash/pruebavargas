# Documentacion Tecnica: Arquitectura e Integracion con MongoDB y Cron Task

## 1. Analisis del Diagrama de Pizarron y Pantalla de Clase

A partir del diagrama de arquitectura y la estructura mostrada en clase, se implementaron los dos flujos fundamentales:

### Flujo 1: Sincronizacion Automatizada (Cron Task Diaria 06:00 AM)
1. **Cron Task (`06:00 AM`)**: Tarea programada con `@Scheduled(cron = "${productos.cron:0 0 6 * * ?}")`.
2. **Client (`GestoPagoProductClient`)**: Consume la API de catalogo de GestoPago (`GET /sistema/service/getProductList.do`).
3. **Cat Product XML**: Recibe la respuesta en XML del proveedor externo.
4. **Mongo DB**: Deserializa el XML y persiste/actualiza los registros en la coleccion `cat_productos` de MongoDB.

### Flujo 2: Consulta de Negocio (Servicio hacia MongoDB)
1. **Servicio (`ProductoService`)**: Recibe peticion de consulta (`Consultar`).
2. **Mongo DB**: Lee directamente desde la coleccion local de MongoDB (`ProductoMongoRepository`), evitando saturar el endpoint externo.
3. **Cat Product Json**: Retorna la lista de productos estructurada en JSON (`ProductoResponseDto`).
4. **Manejo respuesta Enums**: Control y estandarizacion de codigos y mensajes mediante `RespuestaCatalogoEnum`.

---

## 2. Componentes Implementados

### 2.1. Configuracion y Dependencias
* **`build.gradle`**:
  * Dependencia: `org.springframework.boot:spring-boot-starter-data-mongodb`.
* **`application.properties`**:
  * `spring.data.mongodb.uri=mongodb://localhost:27017/db_servicios`
  * `productos.cron=0 0 6 * * ?` (Programacion diaria 06:00 AM)
* **`com.proyecto.servicios.config.MongoConfig`**:
  * Configuracion con `@EnableMongoRepositories(basePackages = "com.proyecto.servicios.repositorys.mongo")` para coexistir con JPA.

### 2.2. Cliente de Integracion (Client)
* **`com.proyecto.servicios.client.GestoPagoProductClient`**:
  * Cliente OpenFeign que replica la nomenclatura mostrada en la pantalla de la clase.
  * Inyecta `Authorization: Bearer <token>` dinamicamente.

### 2.3. Persistencia en MongoDB
* **`com.proyecto.servicios.entity.mongo.ProductoDocument`**:
  * Documento MongoDB mapeado a la coleccion `cat_productos` con indice unico sobre `idProducto`.
* **`com.proyecto.servicios.repositorys.mongo.ProductoMongoRepository`**:
  * Interfaz `MongoRepository` con metodos de consulta especificos.

### 2.4. Estandarizacion con Enums
* **`com.proyecto.servicios.enums.RespuestaCatalogoEnum`**:
  * `EXITO (0)`
  * `SINCRONIZACION_EXITOSA (1)`
  * `SIN_DATOS (2)`
  * `ERROR_AUTORIZACION (401)`
  * `ERROR_TOKEN_EXPIRADO (403)`
  * `ERROR_TIMEOUT (408)`
  * `ERROR_PROVEEDOR (502)`
  * `ERROR_FORMATO_XML (500)`
  * `ERROR_INTERNO (500)`

### 2.5. Capa de Servicio y Controlador
* **`com.proyecto.servicios.service.Impl.ProductoServiceImpl`**:
  * Ejecucion del cron y sincronizacion hacia MongoDB (`guardarEnMongo`).
  * Consulta desde MongoDB hacia JSON (`mapearDocumentosADto`).
* **`com.proyecto.servicios.controller.ProductoController`**:
  * `GET /productos`: Consulta el catalogo desde MongoDB.
  * `POST /productos/sincronizar`: Sincronizacion manual bajo demanda.

### 2.6. Pruebas Unitarias
* **`com.proyecto.servicios.service.ProductoServiceImplTest`**:
  * Validacion de persistencia en MongoDB.
  * Validacion de consulta desde MongoDB.
  * Validacion de errores (401, 403, timeout, parseo XML) con Enums.
