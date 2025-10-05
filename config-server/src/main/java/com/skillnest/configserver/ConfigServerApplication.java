package com.skillnest.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableConfigServer
@EnableDiscoveryClient
public class ConfigServerApplication {

    public static void main(String[] args) {

        System.out.println("🏗️ Starting ARKA Config Server...");
        System.out.println("📁 Loading configurations from: file:../config-repository");
        System.out.println("🔗 Eureka Discovery: Enabled");
        System.out.println("🔐 Security: Basic Auth Enabled");

        SpringApplication.run(ConfigServerApplication.class, args);

        System.out.println("✅ ARKA Config Server started successfully!");
        System.out.println("🌐 Access: http://localhost:8888");
        System.out.println("📊 Health: http://localhost:8888/actuator/health");
    }

}
