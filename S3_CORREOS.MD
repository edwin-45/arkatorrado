# Documentación de Envío de Correos y Plantillas S3

Esta documentación explica el diseño, los puntos de integración y el flujo de notificaciones por correo en el proyecto, incluyendo el uso de plantillas alojadas en S3. Se basa en el código y la configuración encontrados en el repositorio en esta rama. Nota: los fragmentos encontrados pueden ser incompletos y/o parciales.

## Resumen

- El servicio que orquesta el “cálculo de envío” es `arka-gestor-solicitudes`.
- Existe integración con AWS (S3 y SQS) para:
    - Guardar resultados del cálculo en S3.
    - Publicar notificaciones del evento “cálculo completado” en SQS.
- No se encontró una implementación concreta de “envío de emails” (por ejemplo, con SES/SMTP) en el código actual. La configuración sugiere un “toggle” de notificaciones por email, pero el envío no está implementado.
- La documentación propone el flujo y componentes recomendados para:
    - Consumir plantillas de email desde S3.
    - Renderizar plantillas con datos del cálculo.
    - Enviar correos (p. ej., vía Amazon SES).

## Artefactos clave en el repositorio

- Integración AWS (S3 + SQS) en `arka-gestor-solicitudes`:
    - [AWSIntegrationService.java.bak](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/aws/AWSIntegrationService.java.bak)
        - Guarda resultados en S3 y envía mensajes a SQS.
- Orquestación del cálculo:
    - [CalculoEnvioService.java](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/application/service/CalculoEnvioService.java)
        - Publica evento de “cálculo completado”.
        - Tiene integración con AWS comentada para pruebas sin dependencias:
            - Guardar en S3 (comentado).
            - Enviar notificación SQS (referenciado).
- API del cálculo:
    - [CalculoEnvioController.java](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/controller/CalculoEnvioController.java)
- Modelo de dominio del resultado:
    - [CalculoEnvio.java](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/domain/model/CalculoEnvio.java)
- Configuración del servicio:
    - [arca-gestor-solicitudes-dev.yml](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/config-server/config-repository/arca-gestor-solicitudes-dev.yml)
        - Tiene toggles como `gestor.notification.email.enabled: true`.

## Estado actual del envío de correos

- No hay clases ni configuraciones específicas de cliente de correo (p. ej., `JavaMailSender`, `SES`, `smtp`, etc.) en el código encontrado.
- La configuración incluye:
    - `gestor.notification.email.enabled: true`, lo que sugiere intención de enviar emails, pero no se halló el componente que lo ejecute.
- Conclusión: hoy el sistema notifica “cálculo completado” vía SQS y guarda resultados en S3; el envío de correo debe implementarse como un consumidor/servicio adicional.

## Plantillas en S3

- Uso de S3 encontrado:
    - Guardado de resultados de cálculo en la ruta `calculos-envio/{calculoId}-{timestamp}.json` con metadatos (ver `AWSIntegrationService.java.bak`).
- No se encontró código que:
    - Lea plantillas desde S3.
    - Renderice HTML/texto usando plantillas.
- Recomendación de convención para plantillas:
    - Bucket: `arka-envios-bucket` (configurable).
    - Prefijo de plantillas: `templates/emails/`.
    - Estructura sugerida:
        - `templates/emails/calculo-completado.html` (plantilla HTML)
        - `templates/emails/calculo-completado.txt` (versión texto plano opcional)
        - `templates/partials/*` (fragmentos reutilizables)
    - Versionado de plantillas por entorno:
        - `templates/{env}/emails/...` o buckets por entorno.

## Flujo recomendado de “cálculo de envío” → “email”

1. Cliente invoca `POST /api/calculos/envio` con datos (origen, destino, peso, dimensiones).
    - Ver [CalculoEnvioController.java](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/controller/CalculoEnvioController.java).
2. `CalculoEnvioService` calcula y publica evento “cálculo completado”.
    - Ver [CalculoEnvioService.java](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/application/service/CalculoEnvioService.java).
3. Persistencia del resultado:
    - (Opcional) se guarda el resultado como JSON en S3.
    - Ver `guardarCalculoEnS3(...)` en [AWSIntegrationService.java.bak](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/aws/AWSIntegrationService.java.bak).
4. Notificación asíncrona:
    - Se publica un mensaje en SQS con el evento “CALCULO_ENVIO_COMPLETADO” y `calculoId`.
    - Ver `enviarNotificacionSQS(...)` en [AWSIntegrationService.java.bak](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/aws/AWSIntegrationService.java.bak).
5. Servicio de Email (a implementar):
    - Consume mensajes SQS.
    - Recupera los datos del cálculo:
        - Del propio mensaje y/o
        - Del JSON guardado en S3 (`calculos-envio/{calculoId}-*.json`).
    - Descarga la plantilla desde S3 (p. ej., `templates/emails/calculo-completado.html`).
    - Renderiza la plantilla con variables del cálculo.
    - Envía el correo al destinatario vía el proveedor elegido (p. ej., Amazon SES).

## Especificación de plantillas

- Variables sugeridas para “cálculo completado”:
    - `{{calculoId}}`, `{{origen}}`, `{{destino}}`, `{{peso}}`, `{{dimensiones}}`
    - `{{costo}}`, `{{tiempoEstimadoDias}}`, `{{proveedorUtilizado}}`, `{{fechaCalculo}}`
    - Datos del destinatario (si aplica): `{{destinatarioNombre}}`, `{{destinatarioEmail}}`
- Ejemplo de sujeto:
    - `Tu cálculo de envío {{calculoId}} está listo`
- Contenidos:
    - HTML y texto plano (multipart/alternative).
- Metadatos S3 útiles (ya se usan en guardado de resultados):
    - `calculoId`, `timestamp`, `servicio`.

## Diseño recomendado de componentes

- `EmailTemplateService`:
    - Descarga la plantilla desde S3 usando `GetObject` (SDK v2).
    - Permite caché local y versionado por entorno.
- `EmailRenderer`:
    - Interpreta variables y renderiza (Thymeleaf, Handlebars, Mustache, etc.).
- `EmailSender`:
    - Abstracción del proveedor (Amazon SES o SMTP).
    - Implementación `SesEmailSender` con SDK v2 (`software.amazon.awssdk.services.ses`).
- `NotificacionEmailService`:
    - Orquesta: leer SQS → construir modelo → renderizar → enviar.
    - Respeta toggle `gestor.notification.email.enabled`.

## Configuración sugerida

En el `config-server` o `application.yml` del servicio de email:

```yaml
aws:
  region: us-east-1
  s3:
    bucket-name: arka-envios-bucket
    templates-prefix: templates/emails/
  ses:
    enabled: true
    sender: "no-reply@tudominio.com"

gestor:
  notification:
    email:
      enabled: true
```

- Permisos IAM mínimos:
    - S3: `s3:GetObject` (plantillas), `s3:PutObject` (si guarda evidencias), `s3:ListBucket` (opcional).
    - SQS: `sqs:ReceiveMessage`, `sqs:DeleteMessage`, `sqs:GetQueueAttributes`.
    - SES (si aplica): `ses:SendEmail`/`ses:SendRawEmail`.
- Seguridad:
    - HTTPS/TLS.
    - Cifrado en reposo (S3 SSE).
    - No exponer secretos; usar variables de entorno/secret manager.

## Ejemplo del mensaje SQS (del código existente)

Estructura aproximada enviada por `AWSIntegrationService`:

```json
{
  "tipo": "CALCULO_ENVIO_COMPLETADO",
  "calculoId": "abc123",
  "estado": "EXITOSO",
  "mensaje": "Cálculo de envío completado exitosamente",
  "timestamp": "2025-10-05T13:40:16",
  "servicio": "arca-gestor-solicitudes"
}
```

El consumidor de correo deberá mapear `calculoId` para recuperar los datos y alimentar la plantilla.

## Endpoints de referencia

- `POST /api/calculos/envio` — Calcula envío (roles: ADMINISTRADOR, GESTOR, OPERADOR, USUARIO).
- `GET /api/calculos/prueba-rapida` — Prueba rápida con valores por defecto.
- `GET /api/calculos/estado` — Estado del servicio.
- Endpoints CLI (carga, demo) para pruebas.

Ver controlador: [CalculoEnvioController.java](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/controller/CalculoEnvioController.java)

## Operación y observabilidad

- Logs informativos en guardado S3 y publicación SQS (según `.bak`).
- Recomendado:
    - Métricas: conteo de correos enviados, errores, latencias de S3/SES.
    - Trazas distribuidas: correlacionar `calculoId` desde API → S3/SQS → SES.

## Limitaciones observadas

- Integración AWS parcialmente comentada en `CalculoEnvioService` para pruebas sin dependencias.
- Falta la pieza de “envío de correo”:
    - Cliente SES/SMTP.
    - Descarga/gestión de plantillas desde S3.
    - Servicio consumidor de SQS para disparar emails.

## Pasos siguientes sugeridos

1. Añadir `EmailTemplateService` (S3) y `EmailSender` (SES/SMTP).
2. Crear consumidor SQS que procese `CALCULO_ENVIO_COMPLETADO`.
3. Definir y subir plantillas a `s3://{bucket}/templates/emails/`.
4. Activar el toggle `gestor.notification.email.enabled` en el entorno deseado.
5. Endurecer IAM y cifrado (SSE, KMS si aplica).

---
Referencias en código:
- [AWSIntegrationService.java.bak](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/aws/AWSIntegrationService.java.bak)
- [CalculoEnvioService.java](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/application/service/CalculoEnvioService.java)
- [CalculoEnvioController.java](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/controller/CalculoEnvioController.java)
- [CalculoEnvio.java](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/arka-gestor-solicitudes/src/main/java/com/skillnest/arkagestorsolicitudes/domain/model/CalculoEnvio.java)
- [arca-gestor-solicitudes-dev.yml](https://github.com/edwin-45/arkatorrado/blob/7e595c926b4bdc00931fd7e68d1b8403675fb78b/config-server/config-repository/arca-gestor-solicitudes-dev.yml)