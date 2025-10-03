package com.skillnest.arkagestorsolicitudes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
        "com.skillnest.arkagestorsolicitudes",
        "com.skillnest.security"
})
public class ArkaGestorSolicitudesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArkaGestorSolicitudesApplication.class, args);
    }

}
