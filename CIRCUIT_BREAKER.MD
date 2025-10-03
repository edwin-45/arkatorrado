# Circuit Breaker en el Microservicio `arka-gestor-solicitudes`

> Estado actual: En [`arka-gestor-solicitudes/build.gradle`](../arka-gestor-solicitudes/build.gradle) las dependencias de Circuit Breaker (Resilience4j) están **comentadas**, por lo tanto hoy **NO hay un Circuit Breaker activo**. Este documento describe el patrón, cómo activarlo en un entorno Spring Boot 3 + WebFlux, cómo probarlo y cómo operarlo.

---

## Tabla de Contenido
1. [Qué es el Circuit Breaker](#1-qué-es-el-circuit-breaker)
2. [Estados](#2-estados-del-circuit-breaker)
3. [Dependencias necesarias](#3-módulos-recomendados-spring-boot-3--webflux)
4. [Activación en Gradle](#4-activación-en-buildgradle)
5. [Configuración YAML](#5-configuración-básica-applicationyml)
6. [Uso con WebFlux (anotaciones)](#6-uso-con-webflux-webclient--anotación)
7. [Uso programático (Decorators)](#7-uso-programático-decorators)
8. [Clasificación de errores](#8-manejo-de-errores-y-clasificación)
9. [Métricas y monitoreo](#9-métricas-y-monitoreo)
10. [Estrategias de fallback](#10-estrategias-de-fallback)
11. [Pruebas manuales](#11-pruebas-manuales-del-circuit-breaker)
12. [Pruebas automatizadas](#12-pruebas-automatizadas-ejemplo-junit--stepverifier)
13. [Chaos / resiliencia](#13-testing-de-resiliencia--chaos)
14. [Tuning de parámetros](#14-ajuste-de-parámetros-heurística)
15. [Buenas prácticas](#15-buenas-prácticas)
16. [Checklist implementación](#16-checklist-de-implementación)
17. [Roadmap incremental](#17-roadmap-incremental)
18. [Errores comunes](#18-errores-comunes-a-evitar)
19. [Eventos y observabilidad](#19-ejemplo-de-observación-de-eventos)
20. [Conclusión y siguientes pasos](#20-conclusión)

---

## 1. ¿Qué es el Circuit Breaker?

El patrón Circuit Breaker evita invocar repetidamente un recurso remoto inestable (lento, caído o degradado). Detecta fallos sostenidos y “abre” el circuito para:
- Fallar rápido (fail fast).
- Proteger recursos locales (threads, conexiones).
- Evitar efectos cascada.
- Proporcionar señales de salud para monitoreo y tuning.

---

## 2. Estados del Circuit Breaker

```
+---------+  (tasa de fallos excedida)   +-------+
| CLOSED  | ---------------------------> | OPEN  |
+---------+                              +-------+
     ^                                        |
     | (timeout de apertura transcurrido)     |
     |                                        v
  +-----------<-------------------------+ +----------+
  |  HALF_OPEN (llamadas de prueba)     | | fallback |
  +-------------------------------------+ +----------+
```

- CLOSED: Tráfico normal; se cuentan éxitos/fallos.
- OPEN: Se rechazan llamadas inmediatamente (CallNotPermitted).
- HALF_OPEN: Se permiten pocas llamadas de prueba para decidir si volver a CLOSED o reabrir.

---

## 3. Módulos recomendados (Spring Boot 3 + WebFlux)

| Módulo | Propósito |
|--------|-----------|
| `spring-cloud-starter-circuitbreaker-resilience4j` | Integración Spring Cloud |
| `resilience4j-spring-boot3` | Auto-config, métricas, propiedades |
| `resilience4j-reactor` | Operadores reactivos para Mono/Flux |
| `resilience4j-timelimiter` (opcional) | Timeouts controlados |

---

## 4. Activación en `build.gradle`

Archivo original muestra dependencias comentadas:

```gradle
// Circuit Breaker dependencies
//implementation 'org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j'
//implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.2.0'
//implementation 'io.github.resilience4j:resilience4j-reactor:2.2.0'
```

Descomentar / agregar:

```gradle
implementation 'org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j'
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.2.0'
implementation 'io.github.resilience4j:resilience4j-reactor:2.2.0'
```

Reconstruir:
```bash
./gradlew clean build
```

---

## 5. Configuración básica (`application.yml`)

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        registerHealthIndicator: true
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 20
        minimumNumberOfCalls: 10
        failureRateThreshold: 50
        slowCallDurationThreshold: 2s
        slowCallRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        recordExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignoreExceptions:
          - com.skillnest.arka.exception.DominioIgnorableException
    instances:
      proveedorRemote:
        baseConfig: default
        slidingWindowSize: 30
        failureRateThreshold: 40
        waitDurationInOpenState: 5s
  timelimiter:
    instances:
      proveedorRemote:
        timeoutDuration: 3s

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,resilience4j.circuitbreaker.events
```

---

## 6. Uso con WebFlux (`WebClient` + anotación)

```java
package com.skillnest.arka.proveedor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
@RequiredArgsConstructor
public class ProveedorRemoteService {

    private final WebClient webClient; // Bean configurado

    @CircuitBreaker(name = "proveedorRemote", fallbackMethod = "fallbackObtenerProveedor")
    public Mono<ProveedorDTO> obtenerProveedor(String id) {
        return webClient.get()
                .uri("http://arka-proveedor/api/v1/proveedores/{id}", id)
                .retrieve()
                .bodyToMono(ProveedorDTO.class);
    }

    private Mono<ProveedorDTO> fallbackObtenerProveedor(String id, Throwable ex) {
        return Mono.just(ProveedorDTO.builder()
                .id(id)
                .nombre("DESCONOCIDO")
                .estado("DEGRADADO")
                .origen("FALLBACK")
                .build());
    }
}
```

---

## 7. Uso programático (Decorators)

```java
import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import reactor.core.publisher.Mono;
import java.time.Duration;

public Mono<ProveedorDTO> obtenerProveedorDecorado(String id) {
    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("proveedorRemote");
    return Mono.defer(() -> webClient.get()
            .uri("/api/v1/proveedores/{id}", id)
            .retrieve()
            .bodyToMono(ProveedorDTO.class))
        .transformDeferred(CircuitBreakerOperator.of(cb))
        .timeout(Duration.ofSeconds(3))
        .onErrorResume(throwable -> fallbackObtenerProveedor(id, throwable));
}
```

---

## 8. Manejo de errores y clasificación

- `recordExceptions`: Se consideran fallo y suman a tasa de apertura.
- `ignoreExceptions`: No afectan al circuito (errores de dominio esperados).
- Errores HTTP 5xx o timeouts (con TimeLimiter) → fallos.
- Ajustar cuidadosamente para evitar “ruido” de negocio.

---

## 9. Métricas y monitoreo

Actuator (si expuesto):
- `GET /actuator/health` (incluye estado CB).
- `GET /actuator/metrics/resilience4j.circuitbreaker.calls`
- `GET /actuator/resilience4j/circuitbreakerevents` (eventos de transición)
- Métricas Prometheus típicas:
    - `resilience4j_circuitbreaker_calls_total`
    - `resilience4j_circuitbreaker_state`

Observa:
- Tasa de fallos
- Porcentaje slow calls
- Estados (OPEN > umbral temporal)

---

## 10. Estrategias de Fallback

1. Respuesta degradada mínima.
2. Cache local / Redis (último valor válido).
3. Composición parcial (datos incompletos marcados).
4. Fallback → cola asincrónica (en flujos write).
5. Orden recomendado (si se combinan): TimeLimiter → Retry (1) → CircuitBreaker → Fallback.

---

## 11. Pruebas Manuales del Circuit Breaker

Simular dependencia caída:

```bash
# Llamadas repetidas
for i in {1..25}; do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/proveedores/123
done

# Ver estado
curl http://localhost:8080/actuator/health | jq '.components.circuitBreakers'
```

Pasos:
1. Levantar microservicio.
2. Apagar servicio remoto / cortar red.
3. Generar suficientes fallos → circuito OPEN.
4. Verificar respuestas rápidas (fallback).
5. Esperar `waitDurationInOpenState`.
6. Restaurar servicio remoto.
7. Observar HALF_OPEN → CLOSED tras éxitos.

---

## 12. Pruebas Automatizadas (JUnit + StepVerifier)

```java
@SpringBootTest
class ProveedorRemoteServiceTest {

    @Autowired
    ProveedorRemoteService service;

    @MockBean
    WebClient webClient;

    @Test
    void abreCircuitoTrasFallos() {
        // Simular fallos (ejemplo simplificado)
        for (int i = 0; i < 15; i++) {
            reactor.test.StepVerifier.create(service.obtenerProveedor("X"))
                .expectNextMatches(p -> p.getOrigen().equals("FALLBACK"))
                .verifyComplete();
        }
        // Extender: Asserts sobre métricas (Micrometer TestRegistry)
    }
}
```

---

## 13. Testing de Resiliencia / Chaos

Herramientas:
- `toxiproxy` / `chaos-mesh` (latencia / error).
- `tc` Linux:
  ```bash
  sudo tc qdisc add dev lo root netem delay 2500ms
  sudo tc qdisc del dev lo root
  ```
- k6:
  ```javascript
  import http from 'k6/http';
  import { check, sleep } from 'k6';
  export default function() {
    let r = http.get('http://localhost:8080/api/v1/proveedores/123');
    check(r, { 'status ok or fallback': res => [200,503].includes(res.status) });
    sleep(0.2);
  }
  ```

---

## 14. Ajuste de Parámetros (Heurística)

| Síntoma | Ajuste sugerido |
|---------|-----------------|
| Apertura demasiado agresiva | ↑ `minimumNumberOfCalls` o `slidingWindowSize` |
| No se abre pese a fallos | ↓ `failureRateThreshold` o ventana más pequeña |
| Latencias no consideradas | ↓ `slowCallDurationThreshold` |
| Flapping (abre/cierra rápido) | ↑ `waitDurationInOpenState` |
| Exceso fallbacks | Revisar `recordExceptions` / validación negocio |
| Recuperación lenta | Ajustar `permittedNumberOfCallsInHalfOpenState` |

---

## 15. Buenas Prácticas

- No envolver lógica local (CPU) → sólo IO remota.
- Fallbacks simples y trazables (log causa original).
- Evitar múltiples Retries antes del Circuit Breaker.
- Documentar semántica de degradación para negocio.
- Alertar estado OPEN prolongado.
- Medir antes de tunear (data-driven).

---

## 16. Checklist de Implementación

| Paso | Estado |
|------|--------|
| Descomentar dependencias Gradle | ☐ |
| Crear/actualizar `application.yml` | ☐ |
| Anotar métodos remotos `@CircuitBreaker` | ☐ |
| Implementar fallback claro | ☐ |
| Exponer y probar Actuator | ☐ |
| Añadir dashboards / alertas | ☐ |
| Pruebas de falla (latencia/caída) | ☐ |
| Ajuste inicial parámetros | ☐ |
| Documentar en README | ☐ |

---

## 17. Roadmap Incremental

1. Fase 1: CircuitBreaker + fallback básico.
2. Fase 2: Añadir TimeLimiter (timeouts explícitos).
3. Fase 3: Retry (1 intento) antes del CB (si aplica).
4. Fase 4: Bulkhead (aislar recursos) si saturación.
5. Fase 5: Métricas avanzadas + alertas.
6. Fase 6: Tuning continuo basado en tráfico real.

---

## 18. Errores Comunes a Evitar

| Error | Consecuencia |
|-------|--------------|
| Retry alto + CB | Tormenta de llamadas / degradación mayor |
| Fallback pesado | Latencia oculta / saturación |
| Ignorar excepciones críticas | Circuito nunca abre |
| Umbrales por defecto sin análisis | Falsos positivos/negativos |
| No monitorear | Tuning a ciegas |
| Fallback sin semántica negocio | Decisiones incorrectas downstream |

---

## 19. Ejemplo de Observación de Eventos

Si se expone:
```
GET /actuator/resilience4j/circuitbreakerevents?name=proveedorRemote
```

Ejemplo (simplificado):
```json
{
  "circuitBreakerEvents": [
    {"stateTransition": "CLOSED_TO_OPEN", "failureRate": 55.0, "creationTime": "..."},
    {"stateTransition": "OPEN_TO_HALF_OPEN", "creationTime": "..."},
    {"stateTransition": "HALF_OPEN_TO_CLOSED", "creationTime": "..."}
  ]
}
```

---

## 20. Conclusión

El Circuit Breaker aún no está activo en el microservicio. Este documento brinda:
- Guía de activación técnica.
- Configuración inicial razonable.
- Estrategia de pruebas manuales y automatizadas.
- Lineamientos de operación y tuning.

El éxito depende de:
- Visibilidad (métricas y eventos).
- Ajustes iterativos.
- Alineación con requisitos de negocio (SLO/SLA).

---

## Siguientes Pasos Recomendados

1. Descomentar dependencias y reconstruir.
2. Añadir configuración YAML propuesta.
3. Anotar métodos remotos críticos.
4. Ejecutar pruebas de degradación controladas.
5. Configurar métricas y alertas.
6. Ajustar umbrales basado en datos reales.

---

## Extensiones Futuras

- Combinación con Bulkhead (aislar pools).
- Circuit Breaker por tipo de operación (lectura vs escritura).
- Integración con tracing distribuido (OpenTelemetry) para correlación.
- Escenarios canary: parámetros diferentes por entorno.

---

## Referencias Breves

- Resilience4j Docs: https://resilience4j.readme.io
- Spring Cloud Circuit Breaker: https://spring.io/projects/spring-cloud-circuitbreaker

---

¿Necesitas un dashboard Prometheus/Grafana de ejemplo o un PR listo? Pídelo y lo generamos.
