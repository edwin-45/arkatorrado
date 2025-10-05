package com.app.arkatorrado.infrastructure.config;

import com.app.arkatorrado.domain.port.in.*;
import com.app.arkatorrado.domain.port.out.*;
import com.app.arkatorrado.application.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bean Configuration for Hexagonal Architecture
 * This class configures the dependency injection for the application layer
 * Following Hexagonal Architecture - Configuration Layer
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public ProductUseCase productUseCase(ProductRepositoryPort productRepository, 
                                       CategoryRepositoryPort categoryRepository) {
        return new ProductApplicationService(productRepository, categoryRepository);
    }

    @Bean
    public CategoryUseCase categoryUseCase(CategoryRepositoryPort categoryRepository) {
        return new CategoryApplicationService(categoryRepository);
    }

    @Bean
    public CustomerUseCase customerUseCase(CustomerRepositoryPort customerRepository) {
        return new CustomerApplicationService(customerRepository);
    }

    @Bean
    public OrderUseCase orderUseCase(OrderRepositoryPort orderRepository) {
        return new OrderApplicationService(orderRepository);
    }

    @Bean
    public CartUseCase cartUseCase(CartRepositoryPort cartRepository) {
        return new CartApplicationService(cartRepository);
    }
}
