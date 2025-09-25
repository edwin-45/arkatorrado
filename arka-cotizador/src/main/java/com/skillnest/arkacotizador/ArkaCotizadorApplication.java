package com.skillnest.arkacotizador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ArkaCotizadorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArkaCotizadorApplication.class, args);
    }

}
