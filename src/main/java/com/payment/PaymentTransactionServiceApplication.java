package com.payment;

import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application entry point for Payment Transaction Service.
 */
@SpringBootApplication
public class PaymentTransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentTransactionServiceApplication.class, args);
    }
}
