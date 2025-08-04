package com.app.arkatorrado;

import com.app.arkatorrado.infrastructure.adapter.in.web.CategoryController;
import com.app.arkatorrado.infrastructure.adapter.in.web.CustomerController;
import com.app.arkatorrado.infrastructure.adapter.in.web.ProductController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PruebaControlador {

    @Autowired
    private ProductController productController;

    @Autowired
    private CustomerController customerController;

    @Autowired
    private CategoryController categoryController;

    @Test
    void contextLoads() {
        assertThat(productController).isNotNull();
        assertThat(customerController).isNotNull();
        assertThat(categoryController).isNotNull();
    }
}
