package com.app.arkatorrado.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

/**
 * Configuración MongoDB para DocumentDB
 */
@Configuration
@EnableMongoRepositories(
    basePackages = "com.app.arkatorrado.infrastructure.adapter.out.mongodb.repository"
)
@EnableReactiveMongoRepositories(
    basePackages = "com.app.arkatorrado.infrastructure.adapter.out.mongodb.repository"
)
public class MongoConfiguration {
    // La configuración se maneja automáticamente a través de spring.data.mongodb.uri
}
