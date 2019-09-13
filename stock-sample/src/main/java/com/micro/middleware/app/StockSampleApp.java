package com.micro.middleware.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock Sample
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.micro.middleware")
public class StockSampleApp {

    public static void main(String[] args) {
        SpringApplication.run(StockSampleApp.class, args);
    }

}
