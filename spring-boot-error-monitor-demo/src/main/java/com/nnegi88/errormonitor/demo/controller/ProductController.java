package com.nnegi88.errormonitor.demo.controller;

import com.nnegi88.errormonitor.demo.model.Product;
import com.nnegi88.errormonitor.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {
    
    private final ProductService productService;
    
    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }
    
    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productService.getProductById(id);
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(@Valid @RequestBody Product product) {
        return productService.createProduct(product);
    }
    
    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @Valid @RequestBody Product product) {
        return productService.updateProduct(id, product);
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }
    
    @PostMapping("/{id}/purchase")
    public ResponseEntity<Map<String, Object>> purchaseProduct(
            @PathVariable Long id,
            @RequestParam int quantity) {
        Product product = productService.purchaseProduct(id, quantity);
        return ResponseEntity.ok(Map.of(
            "message", "Purchase successful",
            "product", product,
            "quantity", quantity,
            "remainingStock", product.getStock()
        ));
    }
    
    @GetMapping("/search")
    public List<Product> searchProducts(@RequestParam String query) {
        return productService.searchProducts(query);
    }
    
    @GetMapping("/category/{category}")
    public List<Product> getProductsByCategory(@PathVariable String category) {
        return productService.getProductsByCategory(category);
    }
    
    @PostMapping("/bulk-import")
    public ResponseEntity<Map<String, Object>> bulkImport(@RequestBody List<Product> products) {
        Map<String, Object> result = productService.bulkImport(products);
        return ResponseEntity.ok(result);
    }
}