package com.app.arkatorrado.infrastructure.adapter.in.web;

import com.app.arkatorrado.domain.model.Customer;
import com.app.arkatorrado.domain.model.Order;
import com.app.arkatorrado.domain.model.Product;
import com.app.arkatorrado.domain.port.in.CustomerUseCase;
import com.app.arkatorrado.domain.port.in.OrderUseCase;
import com.app.arkatorrado.domain.port.in.ProductUseCase;
import com.app.arkatorrado.infrastructure.adapter.in.web.dto.OrderDto;
import com.app.arkatorrado.infrastructure.adapter.in.web.mapper.OrderWebMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ordenes")
public class OrderController {


    private final OrderUseCase orderService;
    private final CustomerUseCase customerService;
    private final ProductUseCase productService;

    public OrderController(OrderUseCase orderService, CustomerUseCase customerService, ProductUseCase productService) {
        this.orderService = orderService;
        this.customerService = customerService;
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> obtenerTodas() {
        List<OrderDto> ordenes = orderService.getAllOrders().stream()
                .map(OrderWebMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ordenes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> obtenerPorId(@PathVariable Long id) {
        try {
            Order order = orderService.getOrderById(id);
            return ResponseEntity.ok(OrderWebMapper.toDto(order));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<OrderDto> crear(@RequestBody OrderDto orderDto) {
        try {
            Customer cliente = customerService.getCustomerById(orderDto.getClienteId());

            // Obtener los productos desde sus IDs
            Map<Long, Product> productosMap = new HashMap<>();
            for (Long productoId : orderDto.getProductosIds()) {
                Product producto = productService.getProductById(productoId);
                productosMap.put(productoId, producto);
            }

            Order order = OrderWebMapper.toDomain(orderDto, cliente, productosMap);
            Order savedOrder = orderService.createOrder(order);
            return new ResponseEntity<>(OrderWebMapper.toDto(savedOrder), HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> actualizar(@PathVariable Long id, @RequestBody OrderDto orderDto) {
        try {
            Customer cliente = customerService.getCustomerById(orderDto.getClienteId());

            // Obtener los productos desde sus IDs
            Map<Long, Product> productosMap = new HashMap<>();
            for (Long productoId : orderDto.getProductosIds()) {
                Product producto = productService.getProductById(productoId);
                productosMap.put(productoId, producto);
            }

            Order order = OrderWebMapper.toDomain(orderDto, cliente, productosMap);
            Order updatedOrder = orderService.updateOrder(id, order);
            return ResponseEntity.ok(OrderWebMapper.toDto(updatedOrder));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        try {
            orderService.deleteOrder(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<OrderDto>> obtenerPorCliente(@PathVariable Long clienteId) {
        try {
            Customer cliente = customerService.getCustomerById(clienteId);
            List<OrderDto> ordenes = orderService.getOrdersByCustomer(cliente).stream()
                    .map(OrderWebMapper::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ordenes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<OrderDto>> obtenerPorProducto(@PathVariable Long productoId) {
        try {
            Product producto = productService.getProductById(productoId);
            List<OrderDto> ordenes = orderService.getOrdersByProduct(producto).stream()
                    .map(OrderWebMapper::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ordenes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/fecha")
    public ResponseEntity<List<OrderDto>> obtenerPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        try {
            List<OrderDto> ordenes = orderService.getOrdersByDateRange(inicio, fin).stream()
                    .map(OrderWebMapper::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ordenes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
