# 📂 Estructura del Proyecto `arkatorrado`

Esta arquitectura separa la **lógica de negocio (dominio)** de los **detalles de infraestructura** (frameworks, bases de datos, UI), lo que hace que la aplicación sea más **mantenible, adaptable y fácil de probar**.

---

## 📦 Paquete Raíz (`com.app.arkatorrado`)

- **`ArkatorradoApplication.java`**  
  Punto de entrada principal de la aplicación **Spring Boot**.  
  Contiene el método `main` que inicia el servidor y la aplicación.

- **`ServletInitializer.java`**  
  Permite que la aplicación se empaquete como un archivo **WAR** y se despliegue en un contenedor de servlets tradicional como **Tomcat**.

---

## 🧩 Dominio (`domain`)

Es el **corazón de la aplicación**.  
Contiene toda la lógica y las reglas de negocio, **sin depender de ninguna tecnología externa**.

- **`model`**  
  Contiene las **entidades de negocio principales**.  
  Representan los conceptos clave del problema que se está resolviendo.  

  Ejemplos:  
  - `Product.java`  
  - `Order.java`  
  - `Customer.java`  

  > Definen los datos y el comportamiento de los objetos de negocio.

- **`port.in` (Puertos de Entrada)**  
  Definen **cómo se interactúa con el dominio desde el exterior**.  
  Son **interfaces** que exponen los casos de uso del sistema.

  Ejemplos:  
  - `ProductUseCase.java`  
  - `OrderUseCase.java`  

  > Declaran las operaciones de negocio que se pueden realizar (ej: `crearProducto`, `añadirAlCarrito`).

---

## ⚙️ Aplicación (`application`)

Esta capa **orquesta la lógica de negocio**.  
Actúa como un **intermediario entre la infraestructura y el dominio**.

- **`usecase`**  
  Contiene las **implementaciones de los puertos de entrada (UseCase)**.  

  Ejemplos:  
  - `ProductApplicationService.java`  
  - `OrderApplicationService.java`  

  > Implementan las interfaces `UseCase`.  
  > Coordinan las entidades del dominio para ejecutar un caso de uso específico.  
  > **No contienen lógica de negocio**, solo la orquestan.

---

## 🏗️ Infraestructura (`infrastructure`)

Contiene todo lo relacionado con **tecnologías externas**:  
frameworks web, acceso a bases de datos, servicios de mensajería, etc.

- **`adapter.in.web` (Adaptador de Entrada Web)**  
  Adaptador que conecta la aplicación con **peticiones HTTP**.

  Ejemplos:  
  - `ProductController.java`  
  - `OrderController.java`  

  > Controladores REST de Spring que reciben las peticiones HTTP.  
  > Su responsabilidad es traducir las peticiones en llamadas a los **ApplicationService**.

- **`dto` (Data Transfer Objects)**  
  Objetos para **transferir datos** entre el exterior y la aplicación.  
  Se usan para **no exponer los modelos de dominio directamente** en la API.

- **`mapper`**  
  Clases responsables de **convertir DTO ↔ Modelos de Dominio**.

- **`config`**  
  Clases de configuración de Spring.  

  Ejemplo:  
  - `BeanConfiguration.java` → Declara y configura los **beans de Spring**,  
    como la inyección de los **UseCase (ApplicationService)** en los **Controller**.

---


- 📂 arkatorrado
  - 📂 src
    - 📂 main
      - 📂 java
        - 📂 com
          - 📂 app
            - 📂 arkatorrado
              - 📄 ArkatorradoApplication.java
              - 📄 ServletInitializer.java
              - 📂 application
                - 📂 usecase
                  - 📄 CartApplicationService.java
                  - 📄 CategoryApplicationService.java
                  - 📄 CustomerApplicationService.java
                  - 📄 OrderApplicationService.java
                  - 📄 ProductApplicationService.java
              - 📂 domain
                - 📂 model
                  - 📄 Cart.java
                  - 📄 Category.java
                  - 📄 Customer.java
                  - 📄 Order.java
                  - 📄 Product.java
                - 📂 port
                  - 📂 in
                    - 📄 CartUseCase.java
                    - 📄 CategoryUseCase.java
                    - 📄 CustomerUseCase.java
                    - 📄 OrderUseCase.java
                    - 📄 ProductUseCase.java
              - 📂 infrastructure
                - 📂 adapter
                  - 📂 in
                    - 📂 web
                      - 📄 CartController.java
                      - 📄 CategoryController.java
                      - 📄 CustomerController.java
                      - 📄 OrderController.java
                      - 📄 ProductController.java
                      - 📂 dto
                        - 📄 CartDto.java
                        - 📄 CategoryDto.java
                        - 📄 CustomerDto.java
                        - 📄 OrderDto.java
                        - 📄 ProductDto.java
                      - 📂 mapper
                        - 📄 CartWebMapper.java
                        - 📄 CategoryWebMapper.java
                        - 📄 CustomerWebMapper.java
                        - 📄 OrderWebMapper.java
                        - 📄 ProductWebMapper.java
                - 📂 config
                  - 📄 BeanConfiguration.java


