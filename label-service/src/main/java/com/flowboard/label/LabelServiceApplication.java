package com.flowboard.label;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class LabelServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LabelServiceApplication.class, args);
    }

}
