package com.app.arkatorrado.infrastructure.adapter.in.web.mapper;

import com.app.arkatorrado.domain.model.Customer;
import com.app.arkatorrado.domain.model.Order;
import com.app.arkatorrado.domain.model.Product;
import com.app.arkatorrado.infrastructure.adapter.in.web.dto.OrderDto;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OrderWebMapper {
    /**
     * Convierte un objeto Order del dominio a un OrderDto
     */
    public static OrderDto toDto(Order order) {
        if (order == null) {
            return null;
        }

        OrderDto orderDto = new OrderDto();
        orderDto.setId(order.getId());
        orderDto.setClienteId(order.getCliente() != null ? order.getCliente().getId() : null);
        orderDto.setFecha(order.getFecha());
        orderDto.setTotal(order.getTotal());

        // Convertir Set<Product> a Set<Long> con los IDs de los productos
        if (order.getProductos() != null) {
            Set<Long> productosIds = order.getProductos().stream()
                    .map(Product::getId)
                    .collect(Collectors.toSet());
            orderDto.setProductosIds(productosIds);
        }

        return orderDto;
    }

    /**
     * Convierte un OrderDto a un objeto Order del dominio
     * Requiere un objeto Customer y un Map de productos para la conversión
     */
    public static Order toDomain(OrderDto orderDto, Customer cliente, Map<Long, Product> productosMap) {
        if (orderDto == null) {
            return null;
        }

        Order order = new Order();
        order.setId(orderDto.getId());
        order.setCliente(cliente);
        order.setFecha(orderDto.getFecha());
        order.setTotal(orderDto.getTotal());

        // Convertir Set<Long> a Set<Product> usando el mapa de productos
        if (orderDto.getProductosIds() != null) {
            Set<Product> productos = new HashSet<>();
            for (Long productoId : orderDto.getProductosIds()) {
                Product producto = productosMap.get(productoId);
                if (producto != null) {
                    productos.add(producto);
                }
            }
            order.setProductos(productos);
        }

        return order;
    }
}
