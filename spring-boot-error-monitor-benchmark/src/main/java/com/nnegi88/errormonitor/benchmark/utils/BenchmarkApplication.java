package com.nnegi88.errormonitor.benchmark.utils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@ComponentScan(basePackages = {"com.springboot.errormonitor"})
@RestController
public class BenchmarkApplication {

    private static volatile boolean shouldThrowError = false;
    private static volatile int errorCounter = 0;
    
    public static void main(String[] args) {
        SpringApplication.run(BenchmarkApplication.class, args);
    }
    
    @GetMapping("/benchmark/normal")
    public String normalRequest() {
        // Simulate some business logic
        doSomeWork();
        return "OK";
    }
    
    @GetMapping("/benchmark/error")
    public String errorRequest() {
        doSomeWork();
        throw new RuntimeException("Benchmark test error #" + errorCounter++);
    }
    
    @GetMapping("/benchmark/conditional-error")
    public String conditionalError(@RequestParam(defaultValue = "10") int errorRate) {
        doSomeWork();
        
        // Throw error based on configured rate
        if (Math.random() * 100 < errorRate) {
            throw new RuntimeException("Random benchmark error");
        }
        
        return "OK";
    }
    
    @PostMapping("/benchmark/data")
    public String processData(@RequestBody String data) {
        // Simulate data processing
        doSomeWork();
        
        if (data.contains("error")) {
            throw new IllegalArgumentException("Invalid data: " + data);
        }
        
        return "Processed: " + data.length() + " bytes";
    }
    
    @GetMapping("/benchmark/heavy")
    public String heavyOperation() {
        // Simulate heavy operation
        doHeavyWork();
        
        if (shouldThrowError) {
            throw new RuntimeException("Heavy operation failed");
        }
        
        return "Heavy operation completed";
    }
    
    @PutMapping("/benchmark/config/error-mode")
    public void setErrorMode(@RequestParam boolean enabled) {
        shouldThrowError = enabled;
    }
    
    private void doSomeWork() {
        // Simulate 1-2ms of work
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void doHeavyWork() {
        // Simulate 10-20ms of work
        try {
            Thread.sleep(10 + (int)(Math.random() * 10));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleRuntimeException(RuntimeException e) {
        return "Error: " + e.getMessage();
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public String handleIllegalArgumentException(IllegalArgumentException e) {
        return "Bad Request: " + e.getMessage();
    }
}