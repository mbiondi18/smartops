package com.smartops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartOpsApplication.class, args);
    }
}
