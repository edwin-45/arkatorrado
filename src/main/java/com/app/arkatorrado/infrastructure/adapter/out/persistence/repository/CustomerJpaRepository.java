package com.app.arkatorrado.infrastructure.adapter.out.persistence.repository;

import com.app.arkatorrado.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerJpaRepository  extends JpaRepository<CustomerEntity, Long> {
    List<CustomerEntity> findByNombreStartingWith(String letra);
}
