package com.hackathon.gares;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GaresApplication {
    public static void main(String[] args) {
        SpringApplication.run(GaresApplication.class, args);
    }
}
