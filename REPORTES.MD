# Generación de Reportes en el Proyecto

Este documento describe el estado actual y la guía de uso para la generación de reportes en el proyecto, así como una propuesta técnica para evolucionar hacia reportes formales (PDF/CSV/Excel) con almacenamiento y distribución.

Nota: Según la revisión del repositorio, hoy los “reportes” implementados son salidas de texto plano generadas por utilidades CLI expuestas vía endpoints REST en el servicio arka-gestor-solicitudes. No se encontraron generadores PDF/CSV/Excel en el código actual. Las rutas y clases referenciadas son verificables en los siguientes archivos:
- Controlador REST (endpoints de reporte): [CalculoEnvioController.java](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/controller/CalculoEnvioController.java#L77-L88)
- Utilidades de reporte (texto):
    - [CircuitBreakerCLIUtils.java (reporte estado)](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/cli/CircuitBreakerCLIUtils.java#L100-L106)
    - [CircuitBreakerCLIUtils.java (config, parámetros y recomendaciones)](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/cli/CircuitBreakerCLIUtils.java#L107-L151)
- CLI interactiva (para pruebas manuales):
    - [CircuitBreakerCLI.java (pruebas de carga, sumarios)](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/cli/CircuitBreakerCLI.java#L182-L205)

Los resultados de búsqueda pueden ser incompletos; si existe otro módulo de reportes no indexado o con otro nombre, ajústese este documento en consecuencia.

---

## 1. Objetivo

- Exponer y consumir reportes operativos de estado y pruebas orientados a resiliencia (Circuit Breaker) en formato texto plano.
- Sentar las bases para evolucionar a reportes formales (PDF/CSV/Excel), con almacenamiento (S3), trazabilidad y distribución (email/descarga).

---

## 2. Reportes disponibles hoy (texto plano)

Actualmente el servicio arka-gestor-solicitudes expone utilidades CLI como endpoints REST con contenido text/plain.

### 2.1 Reporte de Estado (Circuit Breaker)

- Endpoint: GET /api/calculos/cli/reporte-estado
- Código fuente controlador: [CalculoEnvioController.java](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/controller/CalculoEnvioController.java#L77-L88)
- Generación del reporte: [CircuitBreakerCLIUtils.generarReporteEstado()](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/cli/CircuitBreakerCLIUtils.java#L100-L151)

Ejemplo de invocación:
```bash
curl -s http://localhost:8082/api/calculos/cli/reporte-estado
```

Salida típica (ejemplo aproximado a partir del código):
```
📊 REPORTE DE ESTADO - CIRCUIT BREAKER
════════════════════════════════════════════════════════════════════════════════
🕐 Fecha: 05/10/2025 12:34:56
🏢 Servicio: Arca Gestor Solicitudes

🔒 Componente: Circuit Breaker para Cálculo de Envío

✅ Estado del Servicio: Servicio de cálculo de envíos operativo

🛠️  CONFIGURACIÓN ACTUAL:
────────────────────────────────────────────────────────────────────────
• Proveedor Externo Service:
  - Sliding Window: 8 llamadas
  - Failure Rate Threshold: 60%
  - Wait Duration: 15 segundos

• Calculo Envio Service:
  - Sliding Window: 10 llamadas
  - Failure Rate Threshold: 50%
  - Wait Duration: 10 segundos
  - Retry: 3 intentos
  - Timeout: 5 segundos

💡 RECOMENDACIONES:
────────────────────────────────────────────────────────────────────────
• Monitoree las métricas regularmente
• Ejecute pruebas de carga periódicamente
• Verifique los logs para detectar patrones
• Ajuste los umbrales según el comportamiento observado
```

Notas:
- El contenido exacto puede variar según configuración y estado del servicio.
- El endpoint retorna Content-Type: text/plain.

### 2.2 Reporte de Pruebas de Carga (resumen)

- Endpoint: POST /api/calculos/cli/prueba-carga
- Body (JSON):
    - llamadas: número de ejecuciones (default: 10)
    - escenario: “externo” | “interno” | “completo” (default: “externo”)
- Código fuente:
    - Controlador: [CalculoEnvioController.java (prueba de carga)](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/controller/CalculoEnvioController.java#L43-L81)
    - Utilidad: [CircuitBreakerCLIUtils.ejecutarPruebaDeCarga(...)](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/cli/CircuitBreakerCLIUtils.java#L29-L78)

Ejemplo de invocación:
```bash
curl -s -X POST http://localhost:8082/api/calculos/cli/prueba-carga \
  -H "Content-Type: application/json" \
  -d '{"llamadas": 12, "escenario": "completo"}'
```

Salida típica (ejemplo aproximado):
```
🔄 PRUEBA DE CARGA - Circuit Breaker
════════════════════════════════════════════════════════════════════════════════
📊 Llamadas: 12 | Escenario: completo
🕐 Inicio: 05/10/2025 12:30:00

# ... listado de resultados por llamada (✅/🔄/❌) ...

📈 RESUMEN DE RESULTADOS:
════════════════════════════════════════════════════════════════════════════════
✅ Exitosos: 9 (75.0%)
🔄 Fallbacks: 2 (16.7%)
❌ Errores: 1 (8.3%)
⏱️ Tiempo total: 4.2 s
```

### 2.3 Demostración Completa (guion de prueba)

- Endpoint: POST /api/calculos/cli/demostracion
- Código fuente:
    - Controlador: [CalculoEnvioController.java (demostración)](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/controller/CalculoEnvioController.java#L90-L103)
    - Utilidad: [CircuitBreakerCLIUtils.ejecutarDemostracion()](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/cli/CircuitBreakerCLIUtils.java#L143-L151)

Ejemplo de invocación:
```bash
curl -s -X POST http://localhost:8082/api/calculos/cli/demostracion
```

Salida:
- Texto guiado que recorre fases de comportamiento “normal”, “degradación”, “fallback”, etc., útil para validación manual.

---

## 3. Ejecución por CLI (interactiva)

Las mismas capacidades existen en una CLI interactiva (opcional) si se habilita la propiedad:
- Activación: `arka.cli.enabled=true`
- Clase: [CircuitBreakerCLI.java](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/cli/CircuitBreakerCLI.java#L182-L205)

Funciones:
- Pruebas de carga “guiadas” (solicita nº de llamadas, escenario).
- Reportes con sumarios y tasas de éxito/fallback/error.

---

## 4. Limitaciones del estado actual

- Formato: solo text/plain (no PDF/CSV/Excel).
- Persistencia: no se almacena histórico de reportes; se generan bajo demanda.
- Descarga/Distribución: no se suben a S3 ni se envían por correo automáticamente.
- Seguridad: los endpoints de “CLI/reportes” deben revisarse en conjunto con la configuración de seguridad del servicio o del API Gateway (hoy con filtros permisivos en dev).

---

## 5. Propuesta de evolución a reportes formales

Para cubrir necesidades de auditoría, BI ligero, soporte y distribución, se recomienda:

### 5.1 Arquitectura

- Módulo/Componente Reportes en arka-gestor-solicitudes (o microservicio dedicado “arka-reportes”).
- Fuentes de datos:
    - Servicios de negocio (p. ej., cálculo de envío).
    - Métricas/estados (Circuit Breaker, tiempos, tasas).
    - Eventos y logs (opcional).
- Formatos:
    - PDF: OpenPDF o iText/OpenHTMLtoPDF (HTML → PDF).
    - CSV: Apache Commons CSV.
    - Excel (XLSX): Apache POI.
- Almacenamiento y distribución:
    - S3: guardar binarios de reportes y metadatos.
    - Email (opcional): adjuntar reportes a destinatarios definidos.
    - API de descarga: URLs firmadas de S3 o streaming desde servicio.

### 5.2 Flujo de generación

1) Solicitud de reporte:
- POST /api/reportes/circuit-breaker?formato=pdf|csv|xlsx&desde=...&hasta=...
- Respuesta async con “reporteId” (202 Accepted) o sync (200 OK) si tamaño menor.

2) Orquestación:
- Consolidar datos desde utilidades existentes (CLIUtils/servicios) y/o métricas.
- Renderizar plantilla (HTML/Thymeleaf) y convertir a PDF o generar CSV/XLSX.

3) Persistencia:
- Subir a S3: s3://arka-reportes/{tipo}/{yyyy}/{MM}/{dd}/{reporteId}.{ext}
- Guardar metadatos (calculoId, fechas, parámetros, hash, size, contentType).

4) Entrega:
- Retornar URL firmada temporal (opción) o permitir descarga vía GET /api/reportes/{reporteId}
- Envío por email (opcional), referenciando el archivo en S3.

5) Monitoreo:
- Métricas: tiempo de generación, tamaño, éxito/error, recuento por tipo.
- Trazabilidad: correlacionar requestId/usuario.

### 5.3 Diseño de componentes

- ReportTemplateService:
    - Plantillas HTML (para PDF) y layouts (cabecera, pie).
    - Versionado por entorno y por tipo de reporte.
- ReportRenderService:
    - HTML → PDF (OpenHTMLtoPDF/LibrePDF).
    - CSV/Excel (Apache Commons CSV/POI).
- ReportStorageService:
    - S3 (SDK v2). Metadatos: tipo, período, usuario, checksum.
- ReportController:
    - Endpoints REST para solicitar y descargar reportes.
- ReportJob/Worker:
    - Procesamiento async (por tamaño/demora). Podría integrarse con SQS para “cola de trabajos”.

### 5.4 Ejemplo de endpoints

- POST /api/reportes/circuit-breaker?formato=pdf&desde=2025-10-01&hasta=2025-10-05
    - Body (opcional): filtros adicionales.
- GET /api/reportes/{reporteId}/descarga
    - Retorna stream del binario o redirige a URL firmada S3.
- GET /api/reportes/{reporteId}/metadata
    - JSON con estado, tamaño, checksums, timestamps.

### 5.5 Consideraciones de seguridad

- Autenticación y autorización por rol (ej. ADMINISTRADOR/OPERADOR).
- Sanitización de parámetros y límites (períodos, tamaño máximo).
- Cifrado en reposo (S3 SSE) y en tránsito (TLS).
- Evitar exponer rutas internas S3 (usar URLs firmadas o proxy de descarga).

### 5.6 Observabilidad

- Métricas (Micrometer/Prometheus):
    - report_generation_duration_seconds
    - report_generated_total (labels: type, format, status)
    - report_size_bytes (histogram)
- Logs con correlación (reporteId, usuarioId).
- Trazas distribuidas si se compone de múltiples servicios.

---

## 6. Ejemplos de salidas formales (propuestos)

- PDF “Estado Circuit Breaker (período)”:
    - Portada: rango de fechas, versión de servicio.
    - Secciones: configuración, métricas, eventos relevantes, recomendaciones.
    - Anexos: logs resumidos/tables.

- CSV/Excel “Resultados de prueba de carga”:
    - Columnas: timestamp, escenario, resultado (COMPLETADO/FALLBACK/ERROR), proveedor, latencia, etc.
    - Hoja resumen con KPI (tasa éxito/fallback/error, p95/p99 si aplica).

---

## 7. Integración con S3 y Email (opcional, alineado con el proyecto)

El proyecto ya contempla integración AWS (comentada/temporal) para S3/SQS en arka-gestor-solicitudes:
- Ejemplo de servicio AWS (borrador): [AWSIntegrationService.java.bak](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/aws/AWSIntegrationService.java.bak)

Sugerencias:
- Reutilizar ese patrón para subir reportes generados a S3, con metadatos (tipo, período, usuario).
- Si se requiere distribución por correo: integrar un EmailSender (p. ej., Amazon SES) y adjuntar o incluir enlaces firmados S3.

---

## 8. Puesta en marcha (resumen)

- Endpoints actuales (texto):
    - GET /api/calculos/cli/reporte-estado → reporte de estado (text/plain)
    - POST /api/calculos/cli/prueba-carga → resumen de carga (text/plain)
    - POST /api/calculos/cli/demostracion → guion demostrativo (text/plain)

- Requisitos:
    - Ejecutar arka-gestor-solicitudes (puerto dev típico: 8082).
    - Revisar seguridad del gateway para exposición en dev ([GatewaySecurityConfig.java](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/api-gateway/src/main/java/com/skillnest/apigateway/config/GatewaySecurityConfig.java#L1-L46)).

- Próximos pasos:
    - Definir alcance inicial de reportes formales (tipos/formatos).
    - Añadir dependencias (OpenPDF/Apache POI) en el módulo elegido.
    - Implementar servicios de renderizado, almacenamiento y endpoints.
    - Añadir tests (unitarios e integración) y métricas.

---

## 9. Referencias

- Controlador (reportes REST): [CalculoEnvioController.java](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/controller/CalculoEnvioController.java#L77-L88)
- Utilidades (texto de reportes):
    - [CircuitBreakerCLIUtils.java — encabezado, fecha y contenido del reporte](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/cli/CircuitBreakerCLIUtils.java#L100-L151)
    - [Resumen de pruebas de carga y métricas](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/cli/CircuitBreakerCLIUtils.java#L63-L78)
- CLI interactiva:
    - [CircuitBreakerCLI.java — flujo de pruebas y sumarios](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/cli/CircuitBreakerCLI.java#L182-L205)
- Seguridad (gateway dev): [GatewaySecurityConfig.java](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/api-gateway/src/main/java/com/skillnest/apigateway/config/GatewaySecurityConfig.java#L1-L46)
