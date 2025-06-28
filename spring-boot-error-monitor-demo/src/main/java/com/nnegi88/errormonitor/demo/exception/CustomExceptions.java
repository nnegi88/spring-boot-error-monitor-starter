package com.nnegi88.errormonitor.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class CustomExceptions {
    
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(Long id) {
            super("Product not found with id: " + id);
        }
    }
    
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String productName, int requested, int available) {
            super(String.format("Insufficient stock for product '%s'. Requested: %d, Available: %d", 
                productName, requested, available));
        }
    }
    
    @ResponseStatus(HttpStatus.CONFLICT)
    public static class DuplicateProductException extends RuntimeException {
        public DuplicateProductException(String productName) {
            super("Product already exists with name: " + productName);
        }
    }
    
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public static class ExternalServiceException extends RuntimeException {
        public ExternalServiceException(String service, Throwable cause) {
            super("External service failure: " + service, cause);
        }
    }
    
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public static class DatabaseConnectionException extends RuntimeException {
        public DatabaseConnectionException(String message) {
            super("Database connection error: " + message);
        }
    }
    
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class UnauthorizedAccessException extends RuntimeException {
        public UnauthorizedAccessException(String resource) {
            super("Unauthorized access to resource: " + resource);
        }
    }
}