package com.nnegi88.errormonitor.demo.controller;

import com.nnegi88.errormonitor.demo.exception.CustomExceptions.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/demo/errors")
public class DemoErrorController {
    
    private final Random random = new Random();
    
    @GetMapping("/null-pointer")
    public String triggerNullPointer() {
        String str = null;
        return str.toUpperCase(); // Will throw NullPointerException
    }
    
    @GetMapping("/array-index")
    public String triggerArrayIndexOutOfBounds() {
        int[] array = {1, 2, 3};
        return "Value: " + array[10]; // Will throw ArrayIndexOutOfBoundsException
    }
    
    @GetMapping("/arithmetic")
    public String triggerArithmeticException() {
        int result = 10 / 0; // Will throw ArithmeticException
        return "Result: " + result;
    }
    
    @GetMapping("/illegal-argument")
    public String triggerIllegalArgument(@RequestParam(required = false) String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Value parameter cannot be null or empty");
        }
        return "Value: " + HtmlUtils.htmlEscape(value);
    }
    
    @GetMapping("/product-not-found/{id}")
    public String triggerProductNotFound(@PathVariable Long id) {
        throw new ProductNotFoundException(id);
    }
    
    @GetMapping("/insufficient-stock")
    public String triggerInsufficientStock() {
        throw new InsufficientStockException("iPhone 15", 10, 3);
    }
    
    @GetMapping("/external-service")
    public String triggerExternalServiceError() {
        try {
            // Simulate external service call failure
            throw new RuntimeException("Connection timeout");
        } catch (Exception e) {
            throw new ExternalServiceException("PaymentGateway", e);
        }
    }
    
    @GetMapping("/database-error")
    public String triggerDatabaseError() {
        throw new DatabaseConnectionException("Connection pool exhausted");
    }
    
    @GetMapping("/unauthorized")
    public String triggerUnauthorized() {
        throw new UnauthorizedAccessException("/api/admin/users");
    }
    
    @GetMapping("/random")
    public Map<String, Object> triggerRandomError() {
        String[] errorTypes = {
            "null-pointer", "array-index", "arithmetic", 
            "illegal-argument", "product-not-found", "insufficient-stock"
        };
        
        String selectedError = errorTypes[random.nextInt(errorTypes.length)];
        
        switch (selectedError) {
            case "null-pointer":
                triggerNullPointer();
                break;
            case "array-index":
                triggerArrayIndexOutOfBounds();
                break;
            case "arithmetic":
                triggerArithmeticException();
                break;
            case "illegal-argument":
                triggerIllegalArgument(null);
                break;
            case "product-not-found":
                triggerProductNotFound(999L);
                break;
            case "insufficient-stock":
                triggerInsufficientStock();
                break;
        }
        
        return Map.of("error", "Should not reach here");
    }
    
    @GetMapping("/async-error")
    public CompletableFuture<String> triggerAsyncError() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Async operation failed");
        });
    }
    
    @PostMapping("/validation-error")
    public String triggerValidationError(@RequestBody Map<String, Object> payload) {
        if (!payload.containsKey("requiredField")) {
            throw new IllegalArgumentException("Missing required field: requiredField");
        }
        if (payload.get("requiredField") == null) {
            throw new IllegalArgumentException("Required field cannot be null");
        }
        return "Valid payload received";
    }
    
    @GetMapping("/slow-operation")
    public String triggerSlowOperation(@RequestParam(defaultValue = "5000") long delayMs) {
        try {
            Thread.sleep(delayMs);
            if (delayMs > 3000) {
                throw new RuntimeException("Operation timeout after " + delayMs + "ms");
            }
            return "Operation completed";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operation interrupted", e);
        }
    }
    
    @GetMapping("/memory-leak")
    public String simulateMemoryLeak() {
        // Simulate memory issues
        try {
            byte[] data = new byte[100 * 1024 * 1024]; // 100MB
            return "Allocated 100MB";
        } catch (OutOfMemoryError e) {
            throw new RuntimeException("Out of memory error", e);
        }
    }
}