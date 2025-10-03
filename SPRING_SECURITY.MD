# Documentación de Seguridad (Spring Security + JWT)
Proyecto: `arka-security-common`  
Integración: `api-gateway` (Spring Cloud Gateway)

> Esta documentación describe la arquitectura de seguridad basada en JWT, el módulo reutilizable `arka-security-common`, su modelo de dominio, ciclo de vida de autenticación (login / refresh / logout), anotaciones de autorización por roles, y cómo debe integrarse correctamente con el API Gateway. Incluye además guías de prueba manual (curl / HTTPie / Postman) y recomendaciones de endurecimiento.

---

## Tabla de Contenido

1. Visión General
2. Componentes Principales
3. Flujo de Autenticación y Autorización
4. Modelo de Datos y Persistencia
5. Tokens JWT y Refresh Tokens
6. Servicios de Seguridad
7. Anotaciones Personalizadas de Autorización
8. Integración del Módulo `arka-security-common` en otros Microservicios
9. API Gateway: Estado Actual y Plan de Endurecimiento
10. Endpoints de Autenticación y Uso Esperado
11. Ejemplos de Pruebas (curl / HTTPie / Postman)
12. Validación y Manejo de Errores
13. Métricas y Observabilidad (Posibles Extensiones)
14. Tareas de Mantenimiento (Cleanup, Revocación)
15. Riesgos y Recomendaciones de Mejora
16. Checklist de Implementación
17. Roadmap Evolutivo
18. Apéndice: Snippets Relevantes

---

## 1. Visión General

El ecosistema de microservicios ARKA utiliza un mecanismo de autenticación basado en:
- JWT (access token) para autenticación stateless y autorización en cada request.
- Refresh Tokens persistidos en base de datos para renovar sesiones de manera controlada.
- Roles jerarquizados y permisos derivados (ADMINISTRADOR, GESTOR, OPERADOR, USUARIO).
- Repositorios reactivos (R2DBC) para `Usuario` y `RefreshToken`.
- Anotaciones personalizadas para declarar autorizaciones en controladores/servicios.

El módulo `arka-security-common` encapsula la lógica core de seguridad para ser reutilizada por otros microservicios (auth, gestor, cotizador, etc.).  
El API Gateway actúa como puerta de entrada, y en una versión endurecida validará los JWT antes de enrutar.

---

## 2. Componentes Principales

| Componente | Función |
|------------|---------|
| `Usuario` | Entidad que implementa `UserDetails`, contiene rol y estado |
| `RefreshToken` | Entidad persistida para manejar renovación de sesiones |
| `UsuarioRepository` | Acceso reactivo a usuarios |
| `RefreshTokenRepository` | Manejo de tokens de refresco (revocación, limpieza) |
| `AuthService` | Registro, autenticación, refresh, logout, validación |
| `JwtService` | Generación, parsing y validación de JWT |
| `SecurityAnnotations` | Anotaciones custom con `@PreAuthorize` |
| `ArkaSecurityAutoConfiguration` | Auto-config (component scan + repos) |
| `schema.sql` | Esquema base (tablas usuarios y refresh_tokens) |
| `application-security.yml` | Config para ambiente local (H2 + propiedades) |
| (Gateway) `GatewaySecurityConfig` | Config inicial (actualmente permisiva) |
| (Gateway) `JwtAuthenticationFilter` | Filtro global JWT (desactivado / TODO) |

---

## 3. Flujo de Autenticación y Autorización

### 3.1 Registro (Register)
1. Cliente envía `username`, `email`, `password`, `nombreCompleto`.
2. Se valida unicidad de `username` y `email`.
3. Se crea `Usuario` con rol `USUARIO`.
4. Se genera `accessToken` y `refreshToken` (almacena refresh en DB).
5. Respuesta incluye info del usuario + tokens.

### 3.2 Login (Authenticate)
1. Identificador puede ser `username` o `email` (`AuthRequest.identifier`).
2. Verifica credenciales con `PasswordEncoder`.
3. Actualiza `fechaUltimoAcceso`.
4. Genera y retorna tokens.

### 3.3 Acceso a Recursos
- Cliente envía header `Authorization: Bearer <accessToken>`.
- Microservicios (o Gateway) validan integridad y expiración.
- Autorización basada en rol/permiso (vía anotaciones o lógica).

### 3.4 Refresh Token
1. Cliente envía `refreshToken`.
2. Se busca y valida que esté activo y no expirado.
3. Se revoca el refresh token usado (patrón "rotate refresh token").
4. Se genera nuevo par (access + refresh) y se persiste nuevo refresh.
5. Respuesta retorna tokens actualizados.

### 3.5 Logout
- Revoca el refresh token específico (desactiva sesión).
- Acceso con access token activo expira naturalmente.

### 3.6 Logout All
- Revoca todos los refresh tokens del usuario → Forzar re-login universal.

### 3.7 Limpieza (Cleanup)
- Tarea programada (no implementada en el snippet) debería llamar `deleteExpiredTokens()`.

---

## 4. Modelo de Datos y Persistencia

### Tabla `usuarios`
Campos clave:
- `rol` (enum lógico en código: ADMINISTRADOR, GESTOR, OPERADOR, USUARIO)
- `fecha_ultimo_acceso` → se actualiza en login
- Índices en username, email, rol, activo para consultas rápidas

### Tabla `refresh_tokens`
Campos clave:
- `token` (string aleatorio persistido para refresh)
- `fecha_expiracion`
- `activo` (revocación lógica)
- `ip_address`, `user_agent` (telemetría / seguridad contextual futura)

### Relaciones y Consistencia
- FK `usuario_id` con ON DELETE CASCADE → limpia tokens al eliminar usuario.
- Revocaciones no borran registros (auditoría implícita) salvo limpieza histórica.

---

## 5. Tokens JWT y Refresh Tokens

| Tipo | Vida Útil (por defecto) | Persistido | Uso |
|------|-------------------------|------------|-----|
| Access Token | `arka.security.jwt.expiration` (default 86400 s = 24h) | NO | Autorización por request |
| Refresh Token (JWT semántico + DB record) | `refresh-expiration` (default 7 días) | SÍ (registro) | Renovación de access token |

### 5.1 Claims Principales (Access Token)
- `sub`: username
- `userId`
- `email`
- `role`
- `nombreCompleto`
- `permissions` (permiso granular derivado del rol)
- `exp`, `iat`

### 5.2 Refresh Token (Claims)
- `sub`: username
- `userId`
- `type`: `"refresh"`
- `exp`

### 5.3 Validación (`JwtService`)
- Firma HMAC (clave en `secretKey`).
- Verifica expiración.
- Confirma `sub` vs usuario encontrado.
- En refresh: check adicional `type == 'refresh'`.

### 5.4 Observaciones de Seguridad
- Clave secreta definida por property: `arka.security.jwt.secret`.
- Debe externalizarse y rotarse (no hardcode en repositorio).
- Usar longitud suficiente (>256 bits).
- Añadir `kid` futuro si se implementa rotación multinodo.

---

## 6. Servicios de Seguridad

### 6.1 `AuthService`
Responsabilidades:
- Registro `register()`
- Login `authenticate()`
- Refresh `refreshToken()`
- Logout simple `logout()`
- Logout global `logoutAll()`
- Validación token vs usuario (`validateToken()`)
- Extracción de usuario desde token (`getUserFromToken()`)
- Limpieza (`cleanupExpiredTokens()`)

Patrones usados:
- Reactor `Mono` en todas las operaciones.
- Encadenamiento de validaciones con `switchIfEmpty` y `flatMap`.
- Rotación de refresh tokens (revoca y crea nuevo).

### 6.2 `JwtService`
Funciones:
- Generación de access / refresh tokens.
- Extracción de claims (`extractUsername`, `extractUserId`, etc.).
- Validación semántica.
- Verificación de permisos (`hasPermission(token, permiso)`).

---

## 7. Anotaciones Personalizadas de Autorización

Archivo: `SecurityAnnotations.java`

| Anotación | Expresión (Spring EL) | Acceso |
|-----------|-----------------------|--------|
| `@AdminOnly` | `hasRole('ADMINISTRADOR')` | Solo administradores |
| `@ManagementAccess` | `hasAnyRole('ADMINISTRADOR','GESTOR')` | Gestión |
| `@OperationalAccess` | `hasAnyRole('ADMINISTRADOR','GESTOR','OPERADOR')` | Operaciones |
| `@AuthenticatedAccess` | `hasAnyRole('ADMINISTRADOR','GESTOR','OPERADOR','USUARIO')` | Cualquier usuario autenticado |
| `@RequirePermission("...")` | (Pendiente – no posee `@PreAuthorize` directo) | Para futura extensión a permisos avanzados |
| `@OwnerOrAdminAccess` | `hasRole('ADMINISTRADOR') or @securityService.isOwner(authentication.name, #id)` | Acceso propietario o admin |

Uso sugerido:
```java
@SecurityAnnotations.AdminOnly
@GetMapping("/admin/config")
public Mono<Config> getConfig() { ... }
```

Para `@RequirePermission`, se recomienda implementar un `Aspect` o un `MethodSecurityExpressionHandler` custom que valide permisos.

---

## 8. Integración del Módulo `arka-security-common`

Pasos en un microservicio consumidor:
1. Incluir dependencia (ejemplo Gradle):
   ```gradle
   implementation project(':arka-security-common')
   ```
2. Asegurarse que `ArkaSecurityAutoConfiguration` se cargue (Spring Boot lo hace vía `META-INF/spring.factories`).
3. Definir un `PasswordEncoder` bean si no lo hace el módulo (BCrypt recomendado).
4. Configurar propiedades externas:
   ```yaml
   arka:
     security:
       jwt:
         secret: ${JWT_SECRET}           # Inyectado desde entorno
         expiration: 3600               # 1h
         refresh-expiration: 604800     # 7d
   ```
5. Exponer endpoints `/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout` (controlador no mostrado en snippet – debe implementarse).
6. Aplicar anotaciones sobre endpoints protegidos.
7. (Opcional) Integrar `SecurityWebFilterChain` en servicios WebFlux:
   ```java
   @Bean
   public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
       return http
           .csrf(ServerHttpSecurity.CsrfSpec::disable)
           .authorizeExchange(ex -> ex
              .pathMatchers("/auth/**","/actuator/**").permitAll()
              .anyExchange().authenticated()
           )
           .build();
   }
   ```

---

## 9. API Gateway: Estado Actual y Plan de Endurecimiento

### 9.1 Estado Actual
- `GatewaySecurityConfig` permite TODO (`.anyExchange().permitAll()`).
- El filtro `JwtAuthenticationFilter` está presente pero comentado (`@Component` faltante) y sin validación real.
- Rutas agregan headers como `X-Required-Roles` pero NO se consumen aún por un filtro de autorización.

### 9.2 Endurecimiento (Recomendado)
1. Activar `JwtAuthenticationFilter` e inyectar `JwtService` remoto o microservicio autenticador (o replicar lógica).
2. Validar header `Authorization`:
    - Extraer token.
    - Validar firma y expiración.
    - Enriquecer `exchange.getRequest().mutate().header("X-User-Id", userId)...`.
3. Autorizar comparando roles del token vs `X-Required-Role(s)` de la ruta.
4. Manejo centralizado de errores (401/403).
5. Implementar caché de validaciones (pequeño TTL) para reducir parsing JWT.

---

## 10. Endpoints de Autenticación y Uso Esperado

Aunque no se listan en el repositorio (controlador faltante), la convención esperada:

| Método | Ruta | Body | Descripción |
|--------|------|------|-------------|
| POST | `/auth/register` | `{username,email,password,nombreCompleto}` | Registro usuario |
| POST | `/auth/login` | `{identifier,password}` | Autenticación |
| POST | `/auth/refresh` | `{refreshToken}` | Renovar access token |
| POST | `/auth/logout` | `{refreshToken}` | Revocar sesión específica |
| POST | `/auth/logout-all` | `{identifier}` o encabezado | Revocar todas las sesiones |
| GET  | `/auth/me` | (Authorization) | Datos del usuario (derivado del token) |

Rutas tras gateway (suponiendo gateway en 8080 y servicio auth detrás):
```
http://localhost:8080/auth/login
http://localhost:8080/auth/refresh
```

---

## 11. Ejemplos de Pruebas

### 11.1 Registro
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"nuevo","email":"nuevo@arka.com","password":"Secreta123","nombreCompleto":"Nuevo Usuario"}'
```

### 11.2 Login
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"admin","password":"admin123"}'
```
Respuesta ejemplo:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "usuario": {
    "id": 1,
    "username": "admin",
    "rol": "ADMINISTRADOR",
    "permisos": ["USER_CREATE","USER_READ"...]
  }
}
```

### 11.3 Llamada a Endpoint Protegido
```bash
ACCESS=eyJhbGciOi...
curl http://localhost:8080/api/admin/usuarios \
  -H "Authorization: Bearer $ACCESS"
```

### 11.4 Refresh Token
```bash
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<REFRESH_TOKEN_VALOR>"}'
```

### 11.5 Logout
```bash
curl -X POST http://localhost:8080/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<REFRESH_TOKEN_VALOR>"}'
```

### 11.6 Logout Global
```bash
curl -X POST http://localhost:8080/auth/logout-all \
  -H "Content-Type: application/json" \
  -d '{"identifier":"admin"}'
```

### 11.7 Verificación Manual de Expiración
Simular manipulación:
```bash
jwt=$(jq -r '.accessToken' login.json)
echo "$jwt" | cut -d. -f2 | base64 -d 2>/dev/null | jq
```

---

## 12. Validación y Manejo de Errores

Errores típicos (propuestos):
| Caso | Mensaje actual (RuntimeException) | Mejora recomendada |
|------|------------------------------------|---------------------|
| Usuario no existe | "Usuario no encontrado" | 404 con código de error |
| Credenciales inválidas | "Credenciales incorrectas" | 401 + `error_code=BAD_CREDENTIALS` |
| Usuario inactivo | "Usuario inactivo" | 403 + `error_code=INACTIVE_USER` |
| Refresh token inválido | "Refresh token no válido" | 401 + `error_code=INVALID_REFRESH` |
| Refresh token expirado | "Refresh token expirado o inválido" | 401 + `error_code=EXPIRED_REFRESH` |

Se recomienda:
- Custom exceptions (e.g. `BusinessException`, `AuthenticationException`).
- Handler global (`@RestControllerAdvice`) para mapear a JSON uniforme:
  ```json
  {
    "timestamp":"...",
    "status":401,
    "error":"Unauthorized",
    "error_code":"INVALID_REFRESH",
    "message":"Refresh token no válido",
    "path":"/auth/refresh"
  }
  ```

---

## 13. Métricas y Observabilidad (Extensiones)

Propuestas:
- Contador de logins exitosos vs fallidos (Micrometer).
- Gauge de tokens activos por usuario.
- Métrica de rotaciones de refresh token.
- Eventos de seguridad (login_failed, password_mismatch, refresh_used).
- Log estructurado (JSON) con `userId`, `ip`, `userAgent`.

---

## 14. Tareas de Mantenimiento

| Tarea | Frecuencia | Método |
|-------|------------|--------|
| Limpieza de refresh tokens expirados | Diario / cada hora | `refreshTokenRepository.deleteExpiredTokens()` |
| Auditoría de tokens por IP sospechosa | Ad hoc | `findActiveTokensByIpAddress` |
| Revocación tras cambio de contraseña | Immediate | `revokeAllTokensByUsuarioId` |
| Reindexación DB (si volumen alto) | Semestral | DDL / planeación |

---

## 15. Riesgos y Recomendaciones

| Riesgo | Descripción | Mitigación |
|--------|-------------|-----------|
| Clave JWT en repo | Expuesta en código | Usar variables entorno / Vault |
| Refresh tokens sin límite por usuario | Posible abuso | Agregar límite (p.ej. 5 activos) |
| Falta de `issuer` / `audience` | Menor robustez semántica | Añadir claims estándar |
| Falta de rotación de clave | Riesgo compromiso prolongado | Implementar `kid` + rotación programada |
| Errores genéricos | Dificulta auditoría | Excepciones tipificadas |
| Gateway sin validación | Bypass de recursos internos | Activar filtro JWT pronto |
| Falta de revocación madre (password reset) | Tokens aún válidos | Ejecutar `logoutAll` post reset |
| Permisos embebidos en token | Desincronización si cambian roles | Incluir `token_version` o invalidar en cambios |

---

## 16. Checklist de Implementación

| Paso | Estado |
|------|--------|
| Externalizar `jwt.secret` | ☐ |
| Implementar controlador `/auth/*` | ☐ |
| Activar filtro JWT en Gateway | ☐ |
| Añadir exception handler global | ☐ |
| Limitar refresh tokens por usuario | ☐ |
| Crear tarea programada cleanup | ☐ |
| Agregar métricas de seguridad | ☐ |
| Validar roles en gateway (interceptor) | ☐ |
| Añadir pruebas integradas (WebTestClient) | ☐ |
| Documentar versionado de tokens | ☐ |

---

## 17. Roadmap Evolutivo

Fase | Objetivo | Detalle
-----|----------|--------
1 | Endurecimiento básico | Activar JWT filter + autorizar roles |
2 | Auditoría | Logs estructurados + métricas |
3 | Permisos granulares dinámicos | Implementar `@RequirePermission` con `MethodSecurity` |
4 | Rotación de claves JWT | Introducir `kid` + JWKS endpoint |
5 | Prevención de abuso | Rate limiting / detección de login anómalo |
6 | MFA (opcional) | Segundo factor para ADMINISTRADOR |
7 | Token introspection service | Para revocación centralizada (si surge necesidad) |
8 | Centralización de roles | Migrar permisos a tabla y carga dinámica |

---

## 18. Apéndice: Snippets Relevantes

### 18.1 Entidad Usuario (extracto)
```java
public class Usuario implements UserDetails {
    @Column("rol")
    private Rol rol;
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }
}
```

### 18.2 Generación de JWT
```java
return Jwts.builder()
  .claims(extraClaims)
  .subject(username)
  .issuedAt(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()))
  .expiration(Date.from(expirationTime.atZone(ZoneId.systemDefault()).toInstant()))
  .signWith(getSigningKey())
  .compact();
```

### 18.3 Refresh Token Rotación
```java
refreshToken.revocar();
RefreshToken newRefreshTokenEntity = RefreshToken.crear(usuario.getId(), 7);
```

### 18.4 Repositorios R2DBC (ejemplos)
```java
Mono<Usuario> findByUsernameOrEmail(String identifier);
Flux<RefreshToken> findActiveTokensByUsuarioId(Long usuarioId);
Mono<Void> deleteExpiredTokens();
```

### 18.5 Anotación Propietario o Admin
```java
@PreAuthorize("hasRole('ADMINISTRADOR') or @securityService.isOwner(authentication.name, #id)")
public @interface OwnerOrAdminAccess {}
```

---

## Conclusión

El módulo `arka-security-common` establece una base sólida para autenticación y autorización sobre JWT con soporte para refresh tokens, roles jerarquizados y extensibilidad futura. Sin embargo, hay pasos críticos pendientes para un entorno productivo:
- Activar validación real en el API Gateway.
- Fortalecer manejo de errores y auditoría.
- Externalizar y rotar secretos.
- Limitar y monitorear el uso de refresh tokens.

Con las recomendaciones y roadmap incluidos, se puede evolucionar progresivamente hacia una plataforma segura, observada y mantenible.

---

## ¿Siguientes pasos sugeridos inmediatos?

1. Implementar y exponer `AuthController`.
2. Activar `JwtAuthenticationFilter` en el Gateway con validación real.
3. Introducir `@RestControllerAdvice` para uniformar errores.
4. Añadir métricas básicas y tarea programada de limpieza.
5. Definir política de rotación de secretos.

Si deseas, puedo generar a continuación:
- Un ejemplo de `AuthController`.
- El filtro JWT completo para el Gateway.  
  Pídelo y lo añado.

---
Fin del documento.