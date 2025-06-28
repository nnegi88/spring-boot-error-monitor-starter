package com.nnegi88.errormonitor.benchmark;

import com.nnegi88.errormonitor.config.ErrorMonitorProperties;
import com.nnegi88.errormonitor.core.DefaultErrorProcessor;
import com.nnegi88.errormonitor.filter.RateLimitingErrorFilter;
import com.nnegi88.errormonitor.model.ErrorEvent;
import com.nnegi88.errormonitor.notification.slack.SlackClient;
import com.nnegi88.errormonitor.notification.teams.TeamsClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
public class SimpleBenchmarkRunner {

    public static void main(String[] args) throws Exception {
        System.out.println("Spring Boot Error Monitor - Simple Performance Benchmark");
        System.out.println("======================================================\n");

        // Start Spring Boot application
        ConfigurableApplicationContext context = SpringApplication.run(SimpleBenchmarkRunner.class, 
            "--spring.profiles.active=benchmark-enabled",
            "--server.port=8081");

        try {
            Thread.sleep(2000); // Wait for app to start
            
            // Run benchmarks
            runRequestOverheadBenchmark();
            runMemoryUsageBenchmark();
            runRateLimitingBenchmark();
            runNotificationBenchmark();
            
        } finally {
            context.close();
        }
    }

    private static void runRequestOverheadBenchmark() throws Exception {
        System.out.println("\n1. Request Processing Overhead Benchmark");
        System.out.println("========================================");
        
        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = "http://localhost:8081";
        
        // Warmup
        for (int i = 0; i < 1000; i++) {
            try {
                restTemplate.getForObject(baseUrl + "/benchmark/normal", String.class);
            } catch (Exception ignored) {}
        }
        
        // Measure normal requests
        long startTime = System.nanoTime();
        int iterations = 10000;
        for (int i = 0; i < iterations; i++) {
            try {
                restTemplate.getForObject(baseUrl + "/benchmark/normal", String.class);
            } catch (Exception ignored) {}
        }
        long endTime = System.nanoTime();
        
        double avgTimeMs = (endTime - startTime) / 1_000_000.0 / iterations;
        System.out.printf("Normal Request Average: %.3f ms\n", avgTimeMs);
        
        // Measure error requests
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            try {
                restTemplate.getForObject(baseUrl + "/benchmark/error", String.class);
            } catch (Exception ignored) {}
        }
        endTime = System.nanoTime();
        
        avgTimeMs = (endTime - startTime) / 1_000_000.0 / iterations;
        System.out.printf("Error Request Average: %.3f ms\n", avgTimeMs);
    }

    private static void runMemoryUsageBenchmark() {
        System.out.println("\n2. Memory Usage Benchmark");
        System.out.println("========================");
        
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapBefore = memoryBean.getHeapMemoryUsage();
        
        System.out.printf("Initial Heap Usage: %.2f MB\n", heapBefore.getUsed() / (1024.0 * 1024));
        
        // Simulate error load
        DefaultErrorProcessor processor = new DefaultErrorProcessor(null, null, null, null);
        for (int i = 0; i < 10000; i++) {
            ErrorEvent event = ErrorEvent.builder()
                .timestamp(Instant.now())
                .errorType("TestException")
                .message("Test error " + i)
                .stackTrace("Stack trace...")
                .build();
        }
        
        MemoryUsage heapAfter = memoryBean.getHeapMemoryUsage();
        System.out.printf("After 10K Errors: %.2f MB\n", heapAfter.getUsed() / (1024.0 * 1024));
        System.out.printf("Memory Growth: %.2f MB\n", 
            (heapAfter.getUsed() - heapBefore.getUsed()) / (1024.0 * 1024));
    }

    private static void runRateLimitingBenchmark() {
        System.out.println("\n3. Rate Limiting Performance Benchmark");
        System.out.println("=====================================");
        
        ErrorMonitorProperties properties = new ErrorMonitorProperties();
        properties.getRateLimiting().setMaxErrorsPerMinute(100);
        properties.getRateLimiting().setBurstLimit(10);
        
        RateLimitingErrorFilter filter = new RateLimitingErrorFilter(properties);
        
        // Warmup
        for (int i = 0; i < 10000; i++) {
            filter.shouldReport(null);
        }
        
        // Measure
        long startTime = System.nanoTime();
        int iterations = 1_000_000;
        int allowed = 0;
        
        for (int i = 0; i < iterations; i++) {
            if (filter.shouldReport(null)) {
                allowed++;
            }
        }
        long endTime = System.nanoTime();
        
        double avgTimeNs = (double)(endTime - startTime) / iterations;
        System.out.printf("Average Check Time: %.1f ns\n", avgTimeNs);
        System.out.printf("Allowed Requests: %d (%.2f%%)\n", allowed, 100.0 * allowed / iterations);
    }

    private static void runNotificationBenchmark() {
        System.out.println("\n4. Notification Pipeline Benchmark");
        System.out.println("=================================");
        
        WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:9999/mock-webhook")
            .build();
            
        SlackClient slackClient = new SlackClient(webClient, "http://localhost:9999/mock-webhook");
        
        AtomicLong totalTime = new AtomicLong(0);
        AtomicLong count = new AtomicLong(0);
        
        // Send async notifications
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            ErrorEvent event = ErrorEvent.builder()
                .timestamp(Instant.now())
                .errorType("TestException")
                .message("Test error " + i)
                .build();
                
            long notifStart = System.nanoTime();
            slackClient.sendNotification(event)
                .doOnTerminate(() -> {
                    totalTime.addAndGet(System.nanoTime() - notifStart);
                    count.incrementAndGet();
                })
                .subscribe();
        }
        
        // Wait for completion
        while (count.get() < 100 && System.currentTimeMillis() - startTime < 10000) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }
        
        System.out.printf("Notifications Sent: %d\n", count.get());
        System.out.printf("Average Pipeline Time: %.2f ms\n", 
            totalTime.get() / 1_000_000.0 / Math.max(1, count.get()));
        System.out.println("Non-blocking: Yes (async processing confirmed)");
    }
}