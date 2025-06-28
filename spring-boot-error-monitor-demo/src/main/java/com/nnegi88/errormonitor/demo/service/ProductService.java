package com.nnegi88.errormonitor.demo.service;

import com.nnegi88.errormonitor.demo.exception.CustomExceptions.*;
import com.nnegi88.errormonitor.demo.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductService {
    
    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @PostConstruct
    public void initializeProducts() {
        // Initialize with sample products
        createProduct(new Product(null, "iPhone 15", "Latest iPhone model", 
            new BigDecimal("999.99"), 10, "Electronics", true));
        createProduct(new Product(null, "Samsung Galaxy S24", "Android flagship", 
            new BigDecimal("899.99"), 15, "Electronics", true));
        createProduct(new Product(null, "MacBook Pro", "Professional laptop", 
            new BigDecimal("2499.99"), 5, "Computers", true));
        createProduct(new Product(null, "Dell XPS 15", "High-performance laptop", 
            new BigDecimal("1799.99"), 8, "Computers", true));
        createProduct(new Product(null, "AirPods Pro", "Wireless earbuds", 
            new BigDecimal("249.99"), 20, "Accessories", true));
    }
    
    public List<Product> getAllProducts() {
        log.info("Fetching all products");
        return new ArrayList<>(products.values());
    }
    
    public Product getProductById(Long id) {
        log.info("Fetching product with id: {}", id);
        Product product = products.get(id);
        if (product == null) {
            log.error("Product not found with id: {}", id);
            throw new ProductNotFoundException(id);
        }
        return product;
    }
    
    public Product createProduct(Product product) {
        log.info("Creating new product: {}", product.getName());
        
        // Check for duplicate names
        boolean duplicate = products.values().stream()
            .anyMatch(p -> p.getName().equalsIgnoreCase(product.getName()));
        if (duplicate) {
            log.error("Duplicate product name: {}", product.getName());
            throw new DuplicateProductException(product.getName());
        }
        
        Long id = idGenerator.getAndIncrement();
        product.setId(id);
        products.put(id, product);
        
        log.info("Product created successfully with id: {}", id);
        return product;
    }
    
    public Product updateProduct(Long id, Product updatedProduct) {
        log.info("Updating product with id: {}", id);
        
        Product existingProduct = getProductById(id);
        
        // Check for name conflicts with other products
        boolean nameConflict = products.values().stream()
            .filter(p -> !p.getId().equals(id))
            .anyMatch(p -> p.getName().equalsIgnoreCase(updatedProduct.getName()));
        
        if (nameConflict) {
            log.error("Product name conflict: {}", updatedProduct.getName());
            throw new DuplicateProductException(updatedProduct.getName());
        }
        
        existingProduct.setName(updatedProduct.getName());
        existingProduct.setDescription(updatedProduct.getDescription());
        existingProduct.setPrice(updatedProduct.getPrice());
        existingProduct.setStock(updatedProduct.getStock());
        existingProduct.setCategory(updatedProduct.getCategory());
        existingProduct.setActive(updatedProduct.isActive());
        
        log.info("Product updated successfully: {}", id);
        return existingProduct;
    }
    
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);
        Product removed = products.remove(id);
        if (removed == null) {
            log.error("Cannot delete - product not found with id: {}", id);
            throw new ProductNotFoundException(id);
        }
        log.info("Product deleted successfully: {}", id);
    }
    
    public Product purchaseProduct(Long id, int quantity) {
        log.info("Processing purchase for product: {}, quantity: {}", id, quantity);
        
        Product product = getProductById(id);
        
        if (!product.isActive()) {
            log.error("Product is not active: {}", product.getName());
            throw new IllegalStateException("Product is not available for purchase");
        }
        
        if (product.getStock() < quantity) {
            log.error("Insufficient stock for product: {}", product.getName());
            throw new InsufficientStockException(product.getName(), quantity, product.getStock());
        }
        
        // Simulate external payment service call that might fail
        if (Math.random() < 0.1) { // 10% chance of payment failure
            log.error("Payment service failure for product: {}", product.getName());
            throw new ExternalServiceException("PaymentService", 
                new RuntimeException("Payment gateway timeout"));
        }
        
        product.setStock(product.getStock() - quantity);
        log.info("Purchase successful for product: {}", product.getName());
        return product;
    }
    
    public List<Product> searchProducts(String query) {
        log.info("Searching products with query: {}", query);
        
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
        
        String lowercaseQuery = query.toLowerCase();
        List<Product> results = products.values().stream()
            .filter(p -> p.getName().toLowerCase().contains(lowercaseQuery) ||
                        (p.getDescription() != null && 
                         p.getDescription().toLowerCase().contains(lowercaseQuery)))
            .collect(Collectors.toList());
        
        log.info("Found {} products matching query: {}", results.size(), query);
        return results;
    }
    
    public List<Product> getProductsByCategory(String category) {
        log.info("Fetching products by category: {}", category);
        
        List<Product> results = products.values().stream()
            .filter(p -> p.getCategory().equalsIgnoreCase(category))
            .collect(Collectors.toList());
        
        if (results.isEmpty()) {
            log.warn("No products found in category: {}", category);
        }
        
        return results;
    }
    
    public Map<String, Object> bulkImport(List<Product> productsToImport) {
        log.info("Starting bulk import of {} products", productsToImport.size());
        
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (Product product : productsToImport) {
            try {
                createProduct(product);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                errors.add(String.format("Failed to import '%s': %s", 
                    product.getName(), e.getMessage()));
                log.error("Failed to import product: {}", product.getName(), e);
            }
        }
        
        log.info("Bulk import completed. Success: {}, Failures: {}", successCount, failureCount);
        
        return Map.of(
            "totalProcessed", productsToImport.size(),
            "successCount", successCount,
            "failureCount", failureCount,
            "errors", errors
        );
    }
}