package com.nnegi88.errormonitor.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ErrorMonitorDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErrorMonitorDemoApplication.class, args);
    }
}