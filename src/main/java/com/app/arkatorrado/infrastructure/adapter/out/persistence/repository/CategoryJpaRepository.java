package com.app.arkatorrado.infrastructure.adapter.out.persistence.repository;

import com.app.arkatorrado.infrastructure.adapter.out.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long> {
}
