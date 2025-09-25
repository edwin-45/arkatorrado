
| ID                        | Path                  | Destino (URI)                     | Filtros            | Notas |
|--------------------------|-----------------------|-----------------------------------|--------------------|-------|
| auth-service             | /auth/**              | lb://arka-gestor-solicitudes      | –                  | Sin restricciones de roles |
| admin-service            | /api/admin/**         | lb://arka-gestor-solicitudes      | –                  | – |
| gestion-service          | /api/gestion/**       | lb://arka-gestor-solicitudes      | –                  | – |
| operaciones-service      | /api/operaciones/**   | lb://arka-gestor-solicitudes      | –                  | – |
| calculos-service         | /api/calculos/**      | lb://arka-gestor-solicitudes      | –                  | – |
| cotizador-service        | /api/cotizador/**     | lb://arka-cotizador               | StripPrefix=2      | Reenvía sin /api/cotizador |
| gestor-solicitudes       | /api/gestor/**        | lb://arka-gestor-solicitudes      | StripPrefix=2      | Reenvía sin /api/gestor |
| hello-world-service      | /api/hello/**         | lb://hello-world-service          | StripPrefix=2      | – |
| eureka-server            | /eureka/**            | http://localhost:8761             | –                  | Proxy directo al dashboard de Eureka |

Rutas en GatewayConfig.java (Java):
- /api/hello/** -> lb://hello-world-service (StripPrefix=2)
- /api/cotizador/** -> lb://arca-cotizador (StripPrefix=2)
- /api/gestor/** -> lb://arca-gestor-solicitudes (StripPrefix=2)
- /eureka/** -> http://localhost:8761

Duplicidad:
- Estas rutas existen también en el YAML. Se recomienda unificar para evitar mantenimiento doble.

StripPrefix=2:
- Un request a /api/cotizador/precios/lista se reenvía como /precios/lista al servicio arca-cotizador.

Ruta hacia Eureka:
- En local funciona con http://localhost:8761
- En contenedores, localhost apunta al propio contenedor del Gateway; usa la URL del servicio de Eureka (por ejemplo, http://eureka:8761). Ajusta el YAML en despliegues en Docker/K8s.

### Endpoints del propio Gateway

Controlador GatewayController:
- GET / 
  - Devuelve JSON con información del gateway, versión, servicios disponibles y ejemplos.
- GET /health
  - Estado “UP” con timestamp.
- GET /routes
  - Mapa descriptivo de rutas previstas (informativo).

Manejador global de errores (GlobalErrorHandler):
- Cualquier ruta no encontrada devuelve 404 con un JSON autoexplicativo y sugerencias.

Ejemplo de respuesta 404:
{
  "error": true,
  "status": 404,
  "message": "Route not found in API Gateway",
  "service": "api-gateway",
  "timestamp": 172xxxxxxx,
  "suggestion": "Visit / for available routes and services",
  "availableEndpoints": {
    "home": "/",
    "health": "/health",
    "routes": "/routes",
    "authentication": "/auth/login",
    "mainApp": "http://localhost:8888"
  }
}

### Descubrimiento de servicios

Activado por configuración:
- spring.cloud.gateway.discovery.locator.enabled: true
- lower-case-service-id: true

Esto permite que el Gateway descubra dinámicamente servicios registrados en Eureka usando lb://<service-id>.

### Ejecución local

1) Ejecutar con Gradle:
- Linux/Mac: ./gradlew :api-gateway:bootRun
- Windows:   gradlew.bat :api-gateway:bootRun

2) Ejecutar JAR (tras build):
- Build: ./gradlew :api-gateway:build
- Ejecutar: java -jar api-gateway/build/libs/api-gateway-<version>.jar

3) Requisitos:
- Asegura que Eureka Server está corriendo (por ejemplo, http://localhost:8761)
- Asegura que los microservicios objetivo estén registrados en Eureka con los service-id usados en las rutas (p. ej., arka-gestor-solicitudes, arca-cotizador, hello-world-service)

4) Acceso:
- Home: http://localhost:8080/
- Salud: http://localhost:8080/health
- Rutas: http://localhost:8080/routes
- Rutas proxied: según tabla de rutas

### Consideraciones de seguridad

- No hay configuración de seguridad específica en el módulo del Gateway (abierto por defecto).
- Si necesitas proteger rutas (auth JWT, API keys, RBAC), añade dependencias de Spring Security/Spring Authorization Server o filtros personalizados del Gateway.

Ejemplos:
- Filtro de autenticación por token JWT
- Validación de roles en headers/claims antes de enrutar

### Observabilidad y logging

Configurado en application.yml:
- management.endpoints.web.exposure.include: "*"
- management.endpoint.health.show-details: always
- logging.level.org.springframework.cloud.gateway: DEBUG
- logging.level.org.springframework.cloud.loadbalancer: DEBUG

Puedes bajar a INFO en producción para reducir ruido:
logging.level.org.springframework.cloud.gateway: INFO
logging.level.org.springframework.cloud.loadbalancer: INFO

### Problemas comunes (Gateway)

- Rutas duplicadas (YAML + Java):
  - Puede causar mantenimiento confuso. Elige una única fuente.

- Ruta /eureka/** en Docker:
  - localhost dentro del contenedor no apunta al host. Cambia a http://eureka:8761 o la URL interna correspondiente.

- Servicios no resuelven (404 o 503):
  - Asegura que están registrados en Eureka con el service-id correcto.
  - Verifica que spring.cloud.gateway.discovery.locator.enabled sea true si usas lb://

---

## Ejemplos de prueba (curl)

- Home del Gateway:
  - curl http://localhost:8080/

- Salud del Gateway:
  - curl http://localhost:8080/health

- Rutas informativas:
  - curl http://localhost:8080/routes

- Probar autenticación (proxied a arka-gestor-solicitudes):
  - curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"identifier":"admin","password":"admin123"}'

- Probar hello-world (requiere servicio registrado):
  - curl http://localhost:8080/api/hello/world

- Acceso al dashboard de Eureka a través del Gateway:
  - curl -I http://localhost:8080/eureka/

- Actuator de Eureka (si habilitas y/o con credenciales):
  - curl -u admin:admin123 http://localhost:8761/actuator/health
