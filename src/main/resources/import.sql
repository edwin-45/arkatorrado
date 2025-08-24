-- Inserción de categorías
INSERT INTO categorias (nombre) VALUES ('Electrónica');
INSERT INTO categorias (nombre) VALUES ('Ropa');
INSERT INTO categorias (nombre) VALUES ('Hogar');
INSERT INTO categorias (nombre) VALUES ('Deportes');
INSERT INTO categorias (nombre) VALUES ('Alimentación');

-- Inserción de clientes
INSERT INTO clientes (nombre, email, telefono, pais, ciudad) VALUES ('Juan Pérez', 'juan@example.com', '655123456', 'España', 'Madrid');
INSERT INTO clientes (nombre, email, telefono, pais, ciudad) VALUES ('María García', 'maria@example.com', '633789012', 'España', 'Barcelona');
INSERT INTO clientes (nombre, email, telefono, pais, ciudad) VALUES ('Carlos Rodríguez', 'carlos@example.com', '644567890', 'México', 'Ciudad de México');
INSERT INTO clientes (nombre, email, telefono, pais, ciudad) VALUES ('Ana Martínez', 'ana@example.com', '622345678', 'Colombia', 'Bogotá');
INSERT INTO clientes (nombre, email, telefono, pais, ciudad) VALUES ('Luis Sánchez', 'luis@example.com', '611234567', 'Argentina', 'Buenos Aires');

-- Inserción de productos
INSERT INTO productos (nombre, descripcion, categoria_id, marca, precio_unitario, stock) VALUES ('Smartphone X', 'Teléfono inteligente de última generación', 1, 'TechBrand', 899.99, 50);
INSERT INTO productos (nombre, descripcion, categoria_id, marca, precio_unitario, stock) VALUES ('Laptop Pro', 'Portátil para profesionales', 1, 'CompuTech', 1299.99, 30);
INSERT INTO productos (nombre, descripcion, categoria_id, marca, precio_unitario, stock) VALUES ('Camiseta básica', 'Camiseta de algodón 100%', 2, 'FashionWear', 19.99, 200);
INSERT INTO productos (nombre, descripcion, categoria_id, marca, precio_unitario, stock) VALUES ('Pantalón vaquero', 'Jeans clásicos', 2, 'DenimStyle', 49.99, 150);
INSERT INTO productos (nombre, descripcion, categoria_id, marca, precio_unitario, stock) VALUES ('Sofá moderno', 'Sofá de 3 plazas', 3, 'HomeDecor', 599.99, 15);
INSERT INTO productos (nombre, descripcion, categoria_id, marca, precio_unitario, stock) VALUES ('Lámpara de pie', 'Lámpara LED regulable', 3, 'LightMaster', 89.99, 40);
INSERT INTO productos (nombre, descripcion, categoria_id, marca, precio_unitario, stock) VALUES ('Balón de fútbol', 'Balón oficial', 4, 'SportPro', 29.99, 100);
INSERT INTO productos (nombre, descripcion, categoria_id, marca, precio_unitario, stock) VALUES ('Raqueta de tenis', 'Raqueta profesional', 4, 'TennisAce', 129.99, 35);
INSERT INTO productos (nombre, descripcion, categoria_id, marca, precio_unitario, stock) VALUES ('Aceite de oliva', 'Aceite de oliva virgen extra', 5, 'OliveGold', 12.99, 80);
INSERT INTO productos (nombre, descripcion, categoria_id, marca, precio_unitario, stock) VALUES ('Café en grano', 'Café arábica premium', 5, 'CoffeeDelight', 15.99, 60);

-- Inserción de carritos
INSERT INTO carritos (cliente_id, fecha_creacion, estado) VALUES (1, '2023-06-10 10:30:00', 'activo');
INSERT INTO carritos (cliente_id, fecha_creacion, estado) VALUES (2, '2023-06-11 14:45:00', 'activo');
INSERT INTO carritos (cliente_id, fecha_creacion, estado) VALUES (3, '2023-06-12 09:15:00', 'completado');
INSERT INTO carritos (cliente_id, fecha_creacion, estado) VALUES (4, '2023-06-13 16:20:00', 'abandonado');
INSERT INTO carritos (cliente_id, fecha_creacion, estado) VALUES (5, '2023-06-14 11:00:00', 'activo');

-- Inserción de pedidos
INSERT INTO pedidos (cliente_id, fecha, total) VALUES (1, '2023-06-15 12:30:00', 919.98);
INSERT INTO pedidos (cliente_id, fecha, total) VALUES (2, '2023-06-16 13:45:00', 1349.98);
INSERT INTO pedidos (cliente_id, fecha, total) VALUES (3, '2023-06-17 15:20:00', 649.98);
INSERT INTO pedidos (cliente_id, fecha, total) VALUES (4, '2023-06-18 10:15:00', 159.98);
INSERT INTO pedidos (cliente_id, fecha, total) VALUES (5, '2023-06-19 14:50:00', 28.98);

-- Inserción de relaciones pedido-producto
INSERT INTO pedido_producto (pedido_id, producto_id) VALUES (1, 1);
INSERT INTO pedido_producto (pedido_id, producto_id) VALUES (1, 3);
INSERT INTO pedido_producto (pedido_id, producto_id) VALUES (2, 2);
INSERT INTO pedido_producto (pedido_id, producto_id) VALUES (2, 4);
INSERT INTO pedido_producto (pedido_id, producto_id) VALUES (3, 5);
INSERT INTO pedido_producto (pedido_id, producto_id) VALUES (3, 6);
INSERT INTO pedido_producto (pedido_id, producto_id) VALUES (4, 7);
INSERT INTO pedido_producto (pedido_id, producto_id) VALUES (4, 8);
INSERT INTO pedido_producto (pedido_id, producto_id) VALUES (5, 9);
INSERT INTO pedido_producto (pedido_id, producto_id) VALUES (5, 10);