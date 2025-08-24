## Aplicación de Principios SOLID

### 1. Principio de Responsabilidad Única (S)
En nuestro proyecto, cada clase tiene una única responsabilidad:

Modelos de dominio (Cart, Customer, Order, etc.): Representan las entidades de negocio y sus reglas.
Casos de uso (CartUseCase, CustomerUseCase, etc.): Definen las operaciones de negocio disponibles.
Servicios de aplicación (CartApplicationService, etc.): Implementan la lógica de negocio.
Controladores (CartController, etc.): Gestionan las peticiones HTTP y la transformación de datos.
DTOs y Mappers: Manejan la conversión entre objetos de dominio y representaciones externas.

### 2. Principio Abierto/Cerrado (O)
El diseño permite extender funcionalidades sin modificar el código existente:

Los puertos (interfaces como CartUseCase) están cerrados para modificación pero abiertos para extensión.
Nuevas implementaciones pueden añadirse sin cambiar el código que las utiliza.
La estructura de adaptadores permite incorporar nuevas integraciones sin alterar el núcleo.

### 3. Principio de Sustitución de Liskov (L)
Las implementaciones respetan los contratos definidos por sus interfaces:

CartApplicationService puede sustituir a CartUseCase sin afectar el comportamiento del sistema.
Los controladores operan con interfaces (CartUseCase) en lugar de implementaciones concretas.
### 4. Principio de Segregación de Interfaces (I)
Las interfaces están divididas según su propósito específico:

Cada caso de uso (CartUseCase, OrderUseCase, etc.) define operaciones específicas para su dominio.
Los clientes solo dependen de los métodos que realmente utilizan.
Evitamos interfaces "obesas" con métodos innecesarios para algunos clientes.
### 5. Principio de Inversión de Dependencias (D)
El proyecto invierte las dependencias tradicionales:

Los módulos de alto nivel (CartController) no dependen de módulos de bajo nivel, sino de abstracciones.
Las dependencias son inyectadas a través de constructores.
La configuración de dependencias se centraliza en BeanConfiguration.java.
El flujo de control va desde el exterior (controladores) hacia el interior (dominio) a través de adaptadores.
Estructura Arquitectónica
El proyecto sigue una arquitectura hexagonal (puertos y adaptadores):

Domain: El núcleo con las reglas de negocio y entidades.
Application: Casos de uso que orquestan la lógica de aplicación.
Infrastructure: Adaptadores que conectan el mundo exterior con nuestra aplicación.
Esta arquitectura refuerza los principios SOLID y facilita el mantenimiento, las pruebas y la evolución del sistema.
