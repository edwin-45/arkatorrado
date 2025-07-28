package com.app.arkatorrado.infrastructure.adapter.out.persistence.repository;

import com.app.arkatorrado.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import com.app.arkatorrado.infrastructure.adapter.out.persistence.entity.OrderEntity;
import com.app.arkatorrado.infrastructure.adapter.out.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByProductosContaining(ProductEntity product);

    List<OrderEntity> findByFechaBetween(LocalDateTime start, LocalDateTime end);

    List<OrderEntity> findByCliente(CustomerEntity cliente);
}
