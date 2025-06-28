package com.nnegi88.errormonitor.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledTaskService {
    
    private final Random random = new Random();
    private final AtomicInteger taskExecutionCount = new AtomicInteger(0);
    
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void performDataSync() {
        int count = taskExecutionCount.incrementAndGet();
        log.info("Starting data sync task #{}", count);
        
        // Simulate random failures
        if (random.nextDouble() < 0.2) { // 20% failure rate
            log.error("Data sync task #{} failed", count);
            throw new RuntimeException("Data sync failed: Unable to connect to remote server");
        }
        
        // Simulate processing time
        try {
            Thread.sleep(random.nextInt(2000) + 1000); // 1-3 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Data sync interrupted", e);
        }
        
        log.info("Data sync task #{} completed successfully", count);
    }
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void healthCheck() {
        log.info("Performing system health check at {}", LocalDateTime.now());
        
        // Simulate various health check scenarios
        double randomValue = random.nextDouble();
        
        if (randomValue < 0.1) { // 10% critical failure
            log.error("Critical health check failure");
            throw new RuntimeException("Health check failed: Database connection lost");
        } else if (randomValue < 0.2) { // Additional 10% warning
            log.warn("Health check warning: High memory usage detected");
            // This won't trigger error monitoring (only WARN level)
        } else {
            log.info("Health check passed");
        }
    }
    
    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    public void generateReport() {
        log.info("Generating periodic report");
        
        try {
            // Simulate report generation that might fail
            if (random.nextDouble() < 0.15) { // 15% failure rate
                throw new IllegalStateException("Report generation failed: Insufficient data");
            }
            
            // Simulate external API call
            if (random.nextDouble() < 0.1) { // 10% API failure
                throw new RuntimeException("External API error: Analytics service unavailable");
            }
            
            log.info("Report generated successfully");
        } catch (Exception e) {
            log.error("Report generation failed", e);
            throw e; // Re-throw to trigger error monitoring
        }
    }
    
    @Scheduled(fixedDelay = 45000) // Every 45 seconds
    public void cleanupExpiredData() {
        log.info("Starting cleanup of expired data");
        
        // Simulate different types of errors
        double errorType = random.nextDouble();
        
        if (errorType < 0.05) { // 5% null pointer
            String data = null;
            data.toString(); // NullPointerException
        } else if (errorType < 0.1) { // 5% array index
            int[] array = new int[5];
            int value = array[10]; // ArrayIndexOutOfBoundsException
        } else if (errorType < 0.15) { // 5% arithmetic
            int result = 10 / 0; // ArithmeticException
        }
        
        log.info("Cleanup completed successfully");
    }
    
    @Scheduled(fixedRate = 120000) // Every 2 minutes
    public void performMetricsCollection() {
        log.info("Collecting application metrics");
        
        // Simulate metrics collection that might timeout
        try {
            long sleepTime = random.nextInt(3000) + 1000; // 1-4 seconds
            Thread.sleep(sleepTime);
            
            if (sleepTime > 3000) {
                throw new RuntimeException("Metrics collection timeout after " + sleepTime + "ms");
            }
            
            log.info("Metrics collected successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Metrics collection interrupted", e);
            throw new RuntimeException("Metrics collection interrupted", e);
        }
    }
    
    public int getTaskExecutionCount() {
        return taskExecutionCount.get();
    }
}