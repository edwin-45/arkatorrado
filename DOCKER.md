# Documentación de contenedores y orquestación

Este documento explica, de forma clara y detallada, qué hace cada archivo de contenedor y orquestación en el proyecto:
- Dockerfile (raíz del repo)
- arka-cotizador/Dockerfile
- arka-gestor-solicitudes/Dockerfile
- docker-compose.yml

Incluye además notas de operación, salud, redes y recomendaciones.

---

## 1) Dockerfile (aplicación principal arkatorrado)

Archivo: `./Dockerfile`

```dockerfile
# Usa una imagen base ligera de Java
FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY build/libs/arkatorrado-0.0.1-SNAPSHOT.war app.war
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]
```

Explicación línea por línea:
- `FROM openjdk:17-jdk-alpine`
    - Imagen base de OpenJDK 17 sobre Alpine Linux (ligera y rápida).
    - Provee el runtime Java necesario.
- `WORKDIR /app`
    - Define `/app` como directorio de trabajo dentro del contenedor.
- `COPY build/libs/arkatorrado-0.0.1-SNAPSHOT.war app.war`
    - Copia el artefacto WAR ya compilado desde tu máquina/CI al contenedor y lo renombra a `app.war`.
    - Requisito: el WAR debe existir (por ejemplo tras `./gradlew build` o `mvn package`).
- `EXPOSE 8080`
    - Documenta que la app escucha en el puerto interno 8080 (no publica el puerto por sí mismo).
- `ENTRYPOINT ["java", "-jar", "app.war"]`
    - Proceso principal del contenedor: ejecutar la app Spring Boot empaquetada como WAR.

Notas:
- Asegúrate de compilar antes de construir la imagen (la instrucción `COPY` espera el WAR en `build/libs/`).
- Para builds más reproducibles y livianos, podrías usar multistage builds (fase de build + fase de runtime).

---

## 2) arka-cotizador/Dockerfile

Archivo: `./arka-cotizador/Dockerfile`

```dockerfile
FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Explicación:
- `FROM openjdk:17-jdk-alpine`
    - Runtime Java 17 sobre Alpine.
- `WORKDIR /app`
    - Directorio de trabajo.
- `COPY build/libs/*.jar app.jar`
    - Copia el JAR ya compilado del subproyecto `arka-cotizador` y lo renombra a `app.jar`.
    - Requisito: ejecutar el build dentro de `arka-cotizador` (p. ej. `./gradlew :arka-cotizador:build`) antes de construir la imagen.
- `EXPOSE 8081`
    - Documenta puerto interno 8081 para este servicio.
- `ENTRYPOINT ["java", "-jar", "app.jar"]`
    - Ejecuta la app como un JAR Spring Boot.

---

## 3) arka-gestor-solicitudes/Dockerfile

Archivo: `./arka-gestor-solicitudes/Dockerfile`

```dockerfile
FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Explicación:
- Estructura análoga a la de `arka-cotizador`.
- `COPY build/libs/*.jar app.jar`: requiere el JAR compilado del subproyecto `arka-gestor-solicitudes`.
- `EXPOSE 8080`: documenta puerto interno 8080.

Importante:
- En `docker-compose.yml`, este servicio se mapea con `8082:8082` y sus healthchecks apuntan a `http://localhost:8082/actuator/health`, lo cual sugiere que la app debe escuchar en 8082 dentro del contenedor. Si la app realmente escucha en 8082, convendría ajustar este Dockerfile a `EXPOSE 8082` o mapear correctamente el puerto en Compose (ver “Notas y recomendaciones” más abajo).

---

## 4) docker-compose.yml

Archivo: `./docker-compose.yml`

```yaml
version: '3.8'

services:
  mysql-db:
    image: mysql:8.0
    container_name: mysql-db
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: arkabd
      MYSQL_ROOT_HOST: '%'
    ports:
      - "3306:3306"
    volumes:
      - db_data:/var/lib/mysql
    networks:
      - arkatorrado-net
    command: --default-authentication-plugin=mysql_native_password --bind-address=0.0.0.0
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-proot"]
      timeout: 20s
      retries: 10

  arkatorrado-app:
    build: .
    container_name: arkatorrado-app
    ports:
      - "8080:8080"
    depends_on:
      mysql-db:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql-db:3306/arkabd?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root
    networks:
      - arkatorrado-net

  arka-cotizador:
    build:
      context: ./arka-cotizador
      dockerfile: Dockerfile
    container_name: arka-cotizador
    ports:
      - "8081:8081"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_R2DBC_URL: r2dbc:h2:mem:///cotizador_db?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
      SPRING_R2DBC_USERNAME: sa
      SPRING_R2DBC_PASSWORD: ""
    networks:
      - arkatorrado-net
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  arka-gestor-solicitudes:
    build:
      context: ./arka-gestor-solicitudes
      dockerfile: Dockerfile
    container_name: arka-gestor-solicitudes
    ports:
      - "8082:8082"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_R2DBC_URL: r2dbc:h2:mem:///gestor_db?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
      SPRING_R2DBC_USERNAME: sa
      SPRING_R2DBC_PASSWORD: ""
      PROVEEDORES_COTIZADOR_URL: http://arka-cotizador:8081
    depends_on:
      arka-cotizador:
        condition: service_healthy
    networks:
      - arkatorrado-net
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8082/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

volumes:
  db_data:

networks:
  arkatorrado-net:
    driver: bridge
```

Explicación por secciones:

- `version: '3.8'`
    - Versión del esquema de Compose.

- `services:` define cuatro servicios: `mysql-db`, `arkatorrado-app`, `arka-cotizador`, `arka-gestor-solicitudes`.

### Servicio: mysql-db
- `image: mysql:8.0`: usa MySQL 8 oficial.
- Variables de entorno:
    - `MYSQL_ROOT_PASSWORD: root`: contraseña de root.
    - `MYSQL_DATABASE: arkabd`: crea la DB inicial.
    - `MYSQL_ROOT_HOST: '%'`: permite accesos remotos al usuario root (útil en dev, riesgoso en prod).
- `ports: "3306:3306"`: expone MySQL al host.
- `volumes: db_data:/var/lib/mysql`: persistencia de datos.
- `command`: fuerza `mysql_native_password` y `bind-address=0.0.0.0` para permitir conexiones.
- `healthcheck`:
    - Usa `mysqladmin ping` contra `localhost` con credenciales root para determinar cuándo MySQL está listo.

### Servicio: arkatorrado-app
- `build: .`: construye con el Dockerfile de la raíz.
- `ports: "8080:8080"`: publica la API en el host.
- `depends_on: mysql-db: condition: service_healthy`:
    - La app espera a que MySQL pase el healthcheck antes de iniciar (control de readiness).
- Variables de entorno:
    - `SPRING_DATASOURCE_URL`: apunta al host `mysql-db` (nombre del servicio) en la red de Compose.
    - `allowPublicKeyRetrieval=true`: facilita la conexión desde clientes MySQL modernos en entornos de dev.
    - Usuario/contraseña: `root`/`root` (útil para dev; endurecer en prod).
- Conectado a la red `arkatorrado-net`.

### Servicio: arka-cotizador
- `build.context: ./arka-cotizador`: build desde subcarpeta del microservicio.
- `ports: "8081:8081"`: la app escucha en 8081 y se publica en el mismo puerto.
- Variables:
    - `SPRING_PROFILES_ACTIVE: docker`: activa el perfil docker.
    - `SPRING_R2DBC_URL`: H2 in-memory vía R2DBC (`cotizador_db`), con flags para no cerrar al salir.
    - Credenciales R2DBC: `sa` / vacío.
- `healthcheck`:
    - Usa `wget ... /actuator/health` en puerto 8081 con `start_period` 40s (da tiempo a la app para levantar).

### Servicio: arka-gestor-solicitudes
- `build.context: ./arka-gestor-solicitudes`
- `ports: "8082:8082"`: expone la app en 8082.
- Variables:
    - `SPRING_PROFILES_ACTIVE: docker`
    - `SPRING_R2DBC_URL`: H2 in-memory (`gestor_db`).
    - `PROVEEDORES_COTIZADOR_URL: http://arka-cotizador:8081`:
        - URL interna para llamar al servicio cotizador usando el nombre de servicio como hostname dentro de la red de Docker.
- `depends_on: arka-cotizador: condition: service_healthy`:
    - Espera a que `arka-cotizador` esté saludable antes de iniciar.
- `healthcheck`: igual enfoque que `arka-cotizador`, pero en 8082.

### Recursos compartidos
- `volumes: db_data`: volumen nombrado para persistir la data de MySQL.
- `networks: arkatorrado-net (driver: bridge)`:
    - Red de usuario que permite:
        - DNS interno entre servicios (p.ej., `arkatorrado-app` -> `mysql-db`).
        - Aislamiento del tráfico.

---

## 5) Flujo de arranque y comunicación entre servicios

- Orden y readiness:
    - `mysql-db` arranca y, una vez saludable, permite que `arkatorrado-app` inicie.
    - `arka-cotizador` arranca y, al estar saludable, permite que `arka-gestor-solicitudes` arranque.
- Comunicación interna:
    - Cada servicio se resuelve por su `container_name`/nombre de servicio dentro de `arkatorrado-net`.
    - `arkatorrado-app` se conecta a `mysql-db` vía JDBC.
    - `arka-gestor-solicitudes` consume `arka-cotizador` vía HTTP usando `PROVEEDORES_COTIZADOR_URL`.

---

## 6) Comandos útiles (local)

- Construir e iniciar todo (reconstruyendo imágenes):
    - `docker compose up -d --build`
- Ver logs en streaming:
    - `docker compose logs -f mysql-db`
    - `docker compose logs -f arkatorrado-app`
    - `docker compose logs -f arka-cotizador`
    - `docker compose logs -f arka-gestor-solicitudes`
- Detener y eliminar contenedores:
    - `docker compose down`
- Eliminar también volúmenes (borra datos de MySQL):
    - `docker compose down -v`
- Reconstruir un servicio específico:
    - `docker compose build arka-cotizador && docker compose up -d arka-cotizador`

Requisitos previos:
- Compilar los artefactos:
    - Para la app principal (WAR): `./gradlew build` (en la raíz).
    - Para cada microservicio (JAR):
        - `./gradlew :arka-cotizador:build`
        - `./gradlew :arka-gestor-solicitudes:build`

---

## 7) Salud y observabilidad

- Healthchecks:
    - MySQL: `mysqladmin ping` con timeout y reintentos.
    - Servicios Spring Boot: endpoint `/actuator/health` vía `wget`.
- Ajustes:
    - `start_period` permite a la app “calentar” antes de evaluar salud.
    - `retries` e `interval` se pueden adaptar según tiempos de arranque reales.

---

## 8) Seguridad y configuración

- Credenciales:
    - Se usan credenciales por defecto (root/root) y `MYSQL_ROOT_HOST: '%'` para desarrollo. No recomendado en producción.
- Recomendaciones:
    - Externalizar secretos con `.env` o gestores (Docker Secrets, Vault).
    - Limitar puertos expuestos públicamente si no son necesarios.
    - En producción, usar usuarios no-root y redes restringidas.

Ejemplo `.env` (solo ejemplo):
```
MYSQL_ROOT_PASSWORD=changeme
MYSQL_DATABASE=arkabd
SPRING_DATASOURCE_USERNAME=arkadbuser
SPRING_DATASOURCE_PASSWORD=changeme
```
Y en Compose:
```yaml
  mysql-db:
    env_file:
      - .env
```

---

## 9) Notas y recomendaciones

- Consistencia de puertos en arka-gestor-solicitudes:
    - El Dockerfile expone `8080`, pero Compose y healthcheck usan `8082`.
    - Opciones para alinear:
        1) Si la app escucha en 8082, actualizar el Dockerfile:
            - `EXPOSE 8082`
        2) Si la app escucha en 8080, ajustar Compose:
            - `ports: ["8082:8080"]` y healthcheck a `http://localhost:8080/actuator/health`
        3) Definir explícitamente `server.port=8082` en el perfil `docker` de Spring Boot y mantener `EXPOSE 8082`.

- Persistencia de microservicios:
    - `arka-cotizador` y `arka-gestor-solicitudes` usan H2 en memoria vía R2DBC. Los datos se perderán al reiniciar; perfecto para desarrollo y pruebas, pero no para producción. Para persistencia, considerar Postgres/MySQL con R2DBC.

- Construcción de artefactos:
    - Los Dockerfiles de microservicios esperan `build/libs/*.jar`. Asegúrate de ejecutar el build en cada submódulo antes de `docker compose build` o usa multistage builds para compilar dentro de la imagen.

---

## 10) Resumen rápido

- `Dockerfile` (raíz): empaqueta y ejecuta el WAR de arkatorrado en 8080.
- `arka-cotizador/Dockerfile`: ejecuta el microservicio cotizador en 8081 (JAR).
- `arka-gestor-solicitudes/Dockerfile`: ejecuta el microservicio gestor; revisar puerto expuesto (Dockerfile 8080 vs Compose 8082).
- `docker-compose.yml`: orquesta MySQL, la app principal y los dos microservicios, con healthchecks y redes internas; cotizador y gestor usan H2 in-memory por R2DBC.
