package com.app.arkatorrado.domain.port.out;

import com.app.arkatorrado.domain.model.Customer;
import com.app.arkatorrado.domain.model.Order;
import com.app.arkatorrado.domain.model.Product;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepositoryPort {
    List<Order> findAll();
    Optional<Order> findById(Long id);
    Order save(Order order);
    void deleteById(Long id);
    List<Order> findByProductosContaining(Product product);
    List<Order> findByFechaBetween(LocalDateTime start, LocalDateTime end);
    List<Order> findByCliente(Customer cliente);
    boolean existsById(Long id);
}
