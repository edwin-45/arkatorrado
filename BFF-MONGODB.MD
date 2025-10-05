# Levantamiento del BFF con MongoDB

Este documento describe, de forma profesional y detallada, cómo levantar el BFF (Backend for Frontend) del proyecto integrándolo con MongoDB, incluyendo arquitectura, despliegue local, configuración, modelos de datos, rendimiento, seguridad y observabilidad.

Nota importante: durante la revisión de código no se encontró una implementación explícita y nombrada como “BFF”. En este contexto, consideramos el rol de BFF como un servicio de borde que orquesta llamadas a microservicios (p. ej., `arka-gestor-solicitudes`, `arka-cotizador`) y expone un API optimizado para el frontend. El repositorio ya incluye dependencias de MongoDB (sin y con reactividad) en el `build.gradle` raíz, lo cual habilita su uso desde un servicio BFF:
- Ver dependencias MongoDB en [build.gradle](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/build.gradle#L290-L301).

Si el BFF aún no existe como módulo independiente, se recomienda crearlo (p. ej., `bff-gateway`) o incorporar estas pautas en el `api-gateway` si éste asume el rol de BFF.

---

## Objetivo

- Exponer endpoints adaptados a las necesidades del frontend (BFF).
- Persistir y servir datos específicos del frontend (read models, sesiones, preferencias, cachés de composición) en MongoDB, aprovechando su flexibilidad de esquemas y capacidad de consulta.
- Operar de forma reactiva cuando sea apropiado (con `spring-boot-starter-data-mongodb-reactive`), preservando el backpressure extremo a extremo.

---

## Arquitectura de alto nivel

- Frontend → BFF → Microservicios (p. ej., `arka-gestor-solicitudes`, `arka-cotizador`) y DB MongoDB
- Patrones:
    - Backend for Frontend (adaptación de payloads, composición de datos de varios servicios).
    - CQRS “ligero” en el BFF: usar colecciones Mongo para leer con baja latencia y evitar sobrecargar orígenes.
    - Sesiones/estado UI-friendly (p. ej., carritos, preferencias) en Mongo.

Componentes clave:
- BFF (Spring Boot 3.x, Java 21)
- MongoDB 6.x/7.x (local con Docker o gestionado: Atlas/DocumentDB)
- Service discovery y configuración: `eureka-server`, `config-server`

---

## Despliegue local de MongoDB

El repositorio ya incluye `docker-compose.yml` en la raíz. Añade un servicio de MongoDB:

```yaml
# Fragmento sugerido para docker-compose.yml
services:
  mongo:
    image: mongo:7
    container_name: arka-mongo
    restart: unless-stopped
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: arka
      MONGO_INITDB_ROOT_PASSWORD: arkasecret
      MONGO_INITDB_DATABASE: arka_bff
    volumes:
      - mongo_data:/data/db
      - ./mongo/init:/docker-entrypoint-initdb.d:ro

volumes:
  mongo_data:
```

- Directorio `./mongo/init` (opcional) para scripts de inicialización (`.js`) de colecciones e índices.
- Levantar Mongo:
    - `docker compose up -d mongo`

---

## Configuración del BFF

Variables de entorno recomendadas:
- `MONGODB_URI`: cadena de conexión (con credenciales y opciones).
- `MONGODB_DATABASE`: base lógica (p. ej., `arka_bff`).
- Para entornos gestionados (Atlas/DocumentDB): configurar TLS, CA y parámetros del driver.

Ejemplo `application.yml` del BFF:

```yaml
spring:
  application:
    name: bff-gateway
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://arka:arkasecret@localhost:27017/arka_bff?authSource=admin}
      database: ${MONGODB_DATABASE:arka_bff}
      auto-index-creation: true

server:
  port: 8085

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

Notas:
- `auto-index-creation: true` crea índices definidos por anotaciones (`@Indexed`) en el arranque.
- Si se usa reactividad extremo a extremo, añadir WebFlux en el BFF y preferir `ReactiveMongoRepository`.

---

## Dependencias

El proyecto ya incluye:
- `org.springframework.boot:spring-boot-starter-data-mongodb`
- `org.springframework.boot:spring-boot-starter-data-mongodb-reactive`

Verificar en el `build.gradle` del módulo BFF que no haya conflictos y habilitar sólo lo necesario (imperativo vs reactivo).

---

## Modelo de datos sugerido para el BFF

Colecciones típicas:
- `usuarios_view`: proyección ligera de usuarios para UI (nombre, email, rol).
- `carritos`: estado de carrito por usuario.
- `preferencias_ui`: preferencias/UI-state específicos del cliente.
- `notificaciones`: bandeja de notificaciones/alertas para el usuario.
- `calculos_envio_cache`: cacheo de resultados de composición (cuando el BFF sincroniza datos de servicios como `arka-gestor-solicitudes`).

Ejemplos de documentos:

```json
// carritos
{
  "_id": "usr_123",
  "items": [
    {"sku": "SKU-001", "nombre": "Laptop Pro", "cantidad": 1, "precio": 1299.99},
    {"sku": "SKU-010", "nombre": "Mouse", "cantidad": 2, "precio": 19.99}
  ],
  "total": 1339.97,
  "updatedAt": "2025-10-05T13:45:00Z"
}
```

```json
// calculos_envio_cache
{
  "_id": "calc_abc123",
  "usuarioId": "usr_123",
  "origen": "Lima",
  "destino": "Cusco",
  "peso": 2.0,
  "dimensiones": "40x30x20",
  "costo": 45.90,
  "tiempoEstimadoDias": 3,
  "proveedorUtilizado": "SERVICIO_BACKUP",
  "fechaCalculo": "2025-10-05T13:40:16Z",
  "ttl": 86400
}
```

---

## Índices y rendimiento

Definir índices en campos consultados frecuentemente:
- `carritos`: `_id` (userId), `updatedAt`
- `calculos_envio_cache`: `usuarioId`, `fechaCalculo`, TTL en `ttl` o `expireAt`

Ejemplo con anotaciones:

```java
@Document("calculos_envio_cache")
@CompoundIndex(name = "usuario_fecha_idx", def = "{'usuarioId': 1, 'fechaCalculo': -1}")
public class CalculoEnvioCache {
  @Id private String id;
  @Indexed private String usuarioId;
  @Indexed(expireAfterSeconds = 86400) private Instant expireAt;
  // ...
}
```

O crear índices desde scripts en `./mongo/init/*.js`:

```javascript
db.getSiblingDB("arka_bff").calculos_envio_cache.createIndex(
  { usuarioId: 1, fechaCalculo: -1 },
  { name: "usuario_fecha_idx" }
);

db.getSiblingDB("arka_bff").calculos_envio_cache.createIndex(
  { expireAt: 1 },
  { name: "ttl_idx", expireAfterSeconds: 0 }
);
```

---

## Repositorios y acceso a datos

Imperativo (bloqueante):

```java
public interface CarritoRepository extends MongoRepository<Carrito, String> {
  Optional<Carrito> findById(String userId);
}
```

Reactivo:

```java
public interface CalculoEnvioCacheRepository extends ReactiveMongoRepository<CalculoEnvioCache, String> {
  Flux<CalculoEnvioCache> findByUsuarioIdOrderByFechaCalculoDesc(String usuarioId);
}
```

Uso con servicios BFF:

```java
@Service
public class CarritoBffService {
  private final CarritoRepository repo;
  public CarritoBffService(CarritoRepository repo) { this.repo = repo; }

  public Carrito getOrCreate(String userId) {
    return repo.findById(userId).orElseGet(() -> {
      Carrito nuevo = new Carrito(userId);
      return repo.save(nuevo);
    });
  }
}
```

---

## Composición en el BFF

- El BFF consulta microservicios (p. ej., `arka-gestor-solicitudes`) y enriquece/normaliza respuesta.
- Almacena en Mongo para servir consultas rápidas o estados UI.
- Si se usa WebFlux y repositorios reactivos, orquestar con `Mono/Flux` y WebClient.

Ejemplo de patrón reactivo (simplificado):

```java
public Mono<BffCalculoEnvioDto> calcularYCachear(String usuarioId, PeticionCalculo req) {
  return proveedorCalculo.call(origen, destino, peso)
    .flatMap(resultado -> cacheRepo.save(mapToCache(usuarioId, resultado)).thenReturn(resultado))
    .map(this::mapToDto);
}
```

---

## Semillas y datos de prueba

Crear script `./mongo/init/seed.js`:

```javascript
db = db.getSiblingDB("arka_bff");

db.carritos.insertOne({
  _id: "usr_demo",
  items: [{ sku: "SKU-001", nombre: "Laptop Pro", cantidad: 1, precio: 1299.99 }],
  total: 1299.99,
  updatedAt: new Date()
});

db.preferencias_ui.insertOne({
  _id: "usr_demo",
  theme: "dark",
  language: "es",
  updatedAt: new Date()
});
```

Se ejecutará automáticamente si se monta `./mongo/init` como `docker-entrypoint-initdb.d`.

---

## Migraciones (opcional)

Para cambios controlados de esquema/índices en Mongo, se recomienda usar Mongock:

- Dependencia:
    - `io.mongock:mongock-springboot`
- Definir `@ChangeLog` con `@ChangeSet` para índices, colecciones, migración de datos.

Beneficios:
- Reproducibilidad entre entornos.
- Auditoría de cambios en DB.

---

## Seguridad y conformidad

- Credenciales mediante variables de entorno/secret manager (no en repositorio).
- TLS/SSL en conexiones a Mongo gestionado (Atlas/DocumentDB):
    - `?ssl=true&retryWrites=true&w=majority` (Atlas)
    - Certificados CA adecuados.
- Roles mínimos en DB; mínimo privilegio por microservicio/BFF.
- Enmascarar datos sensibles en logs; PII al mínimo.

---

## Observabilidad

- Actuator: `/actuator/health` verifica `mongo` (si el starter está presente).
- Métricas con Micrometer y Prometheus/Grafana.
- Logs estructurados con `calculoId`/`usuarioId` para trazabilidad extremo a extremo.

---

## Estrategia por entorno

- Local: Docker `mongo:7` + credenciales locales.
- Dev/QA: cluster compartido con namespaces/lógicas de DB separadas (`arka_bff_dev`).
- Prod: Atlas/DocumentDB con:
    - Réplicas/nodos adecuados, IOPS provisionadas si aplica.
    - Backups, alertas, políticas de retención.
    - Índices planificados y revisados por carga.

---

## Puesta en marcha paso a paso

1) Prerrequisitos
- Java 21, Docker, Docker Compose.
- `eureka-server` y `config-server` en ejecución (si el BFF los usa).

2) Base de datos
- `docker compose up -d mongo`
- Verificar conexión: `mongosh "mongodb://arka:arkasecret@localhost:27017/arka_bff?authSource=admin"`

3) Configurar BFF
- Exportar variables:
    - `export MONGODB_URI="mongodb://arka:arkasecret@localhost:27017/arka_bff?authSource=admin"`
    - `export MONGODB_DATABASE="arka_bff"`
- Ajustar `application.yml` según el entorno.

4) Ejecutar BFF
- `./gradlew :bff-gateway:bootRun` (o módulo/artefacto equivalente si el BFF está en `api-gateway`).
- Verificar health: `GET http://localhost:8085/actuator/health`

5) Validar flujo
- Probar endpoint(s) del BFF que compongan datos y lean/escriban en Mongo.
- Revisar colecciones y documentos creados:
    - `mongosh` → `use arka_bff` → `db.carritos.find().pretty()`

---

## Buenas prácticas

- Mantener DTOs del BFF desacoplados de los Payloads internos.
- TTL para cachés temporales; políticas de limpieza para colecciones voluminosas.
- Revisar índices trimestralmente según patrones de consulta reales.
- Tests con Testcontainers (MongoDB) para integración reproducible.

---

## Referencias dentro del repositorio

- Dependencias MongoDB:
    - [build.gradle](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/build.gradle#L290-L301)
- Docker compose general:
    - [docker-compose.yml](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/docker-compose.yml)

Nota de alcance: los resultados de búsqueda automatizada pueden ser incompletos. Si el BFF ya existe bajo otro nombre o con configuraciones específicas, se pueden ajustar los pasos y nombres de artefactos sin cambiar las recomendaciones técnicas aquí descritas.