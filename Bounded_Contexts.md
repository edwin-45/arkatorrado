# ARKA - Análisis de Bounded Contexts

## Introducción
Este documento analiza los Bounded Contexts identificados en el sistema ARKA, un proyecto multi-módulo basado en Spring Boot que maneja cotizaciones y gestión de solicitudes.

## Bounded Contexts Identificados

### 1. **Cotización Context** (`arka-cotizador`)
**Responsabilidad**: Gestión completa del proceso de cotizaciones
- Creación y cálculo de cotizaciones
- Validación de precios y descuentos
- Gestión de productos cotizables
- Histórico de cotizaciones

**Entidades Principales**:
- `Cotizacion`
- `ItemCotizacion`
- `Producto`
- `Cliente` (en contexto de cotización)

### 2. **Gestión de Solicitudes Context** (`arka-gestor-solicitudes`)
**Responsabilidad**: Manejo del ciclo de vida de solicitudes
- Recepción y procesamiento de solicitudes
- Workflow de aprobaciones
- Seguimiento de estados
- Notificaciones

**Entidades Principales**:
- `Solicitud`
- `EstadoSolicitud`
- `TipoSolicitud`
- `Usuario` (en contexto de gestión)
- `Cliente` (en contexto de solicitudes)

### 3. **Core/Shared Context** (módulo principal)
**Responsabilidad**: Funcionalidades compartidas y configuraciones globales
- Autenticación y autorización
- Configuraciones generales
- Entidades base compartidas
- Servicios de infraestructura

## Entidades con Nombres Duplicados

### `Cliente`
- **En Cotización Context**: Enfocado en datos necesarios para cotizar (presupuesto, historial de compras, descuentos aplicables)
- **En Gestión de Solicitudes**: Enfocado en datos de contacto y seguimiento (datos de contacto, preferencias de comunicación, historial de solicitudes)

### `Usuario`
- **En Core Context**: Usuario del sistema con roles y permisos
- **En Gestión de Solicitudes**: Usuario como solicitante o aprobador en workflows

### `Producto`
- **En Cotización Context**: Producto con precios, descuentos y configuraciones de cotización
- **En Core Context**: Información básica del producto (catálogo general)

## Análisis del Modelado Actual

### ✅ **Fortalezas**
- **Separación física**: Los módulos están claramente separados en diferentes directorios
- **Responsabilidades definidas**: Cada módulo tiene un propósito específico
- **Independencia de construcción**: Cada módulo tiene su propio `build.gradle`

### ⚠️ **Áreas de Mejora**
- **Modelos compartidos**: Algunas entidades están acopladas entre contextos
- **Dependencias cruzadas**: Posible dependencia directa entre módulos
- **Consistencia eventual**: Falta de estrategia clara para sincronización de datos

## Estrategia de División en Microservicios

### 1. **Preparación para Microservicios**

#### Separar Entidades Duplicadas:
```java
// Cotización Context
public class ClienteCotizacion {
    private String clienteId;
    private BigDecimal presupuestoMaximo;
    private BigDecimal descuentoAplicable;
    // ... campos específicos para cotización
}

// Gestión de Solicitudes Context  
public class ClienteSolicitud {
    private String clienteId;
    private String email;
    private String telefono;
    private EstadoCliente estado;
    // ... campos específicos para solicitudes
}