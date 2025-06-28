package com.nnegi88.errormonitor.benchmark.benchmarks;

import com.nnegi88.errormonitor.benchmark.utils.BenchmarkApplication;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jol.vm.VM;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark to measure memory usage and allocation patterns of the error monitoring library.
 * 
 * This benchmark measures:
 * 1. Initial memory footprint
 * 2. Memory growth under load
 * 3. Object allocation rates
 * 4. GC pressure
 */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {
    "-Xms512M", "-Xmx512M", 
    "-XX:+UseG1GC",
    "-XX:+UnlockDiagnosticVMOptions",
    "-XX:+PrintCompilation",
    "-verbose:gc"
})
public class MemoryUsageBenchmark {

    private ConfigurableApplicationContext context;
    private RestTemplate restTemplate;
    private String baseUrl = "http://localhost:8083";
    private MemoryMXBean memoryBean;
    
    @Param({"baseline", "basic", "metrics", "analytics", "full"})
    private String profile;

    @Setup(Level.Trial)
    public void setup() {
        memoryBean = ManagementFactory.getMemoryMXBean();
        
        // Force GC before starting
        System.gc();
        Thread.yield();
        System.gc();
        
        // Record initial memory
        recordMemoryUsage("Before application start");
        
        // Start application with specified profile
        context = SpringApplication.run(
            BenchmarkApplication.class,
            "--server.port=8083",
            "--spring.profiles.active=benchmark-" + profile
        );
        
        restTemplate = new RestTemplate();
        waitForApplicationReady();
        
        // Record memory after startup
        recordMemoryUsage("After application start");
        
        // Warm up
        for (int i = 0; i < 1000; i++) {
            try {
                restTemplate.getForObject(baseUrl + "/benchmark/normal", String.class);
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Force GC and record baseline
        System.gc();
        Thread.yield();
        System.gc();
        recordMemoryUsage("After warmup");
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Benchmark
    public void measureInitialFootprint(Blackhole blackhole) {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        blackhole.consume(heapUsage.getUsed());
        blackhole.consume(nonHeapUsage.getUsed());
        
        System.out.println("Initial Memory Footprint:");
        System.out.println("  Heap Used: " + formatBytes(heapUsage.getUsed()));
        System.out.println("  Non-Heap Used: " + formatBytes(nonHeapUsage.getUsed()));
        System.out.println("  Total: " + formatBytes(heapUsage.getUsed() + nonHeapUsage.getUsed()));
    }

    @Benchmark
    @Measurement(iterations = 1, time = 60, timeUnit = TimeUnit.SECONDS)
    public void measureMemoryGrowthUnderLoad(Blackhole blackhole) {
        long startHeap = memoryBean.getHeapMemoryUsage().getUsed();
        long errorCount = 0;
        long requestCount = 0;
        
        // Run for 60 seconds with mixed load
        long endTime = System.currentTimeMillis() + 60_000;
        
        while (System.currentTimeMillis() < endTime) {
            requestCount++;
            
            // Mix of normal and error requests
            if (requestCount % 10 == 0) {
                try {
                    restTemplate.getForObject(baseUrl + "/benchmark/error", String.class);
                } catch (HttpServerErrorException e) {
                    errorCount++;
                    blackhole.consume(e);
                }
            } else {
                String result = restTemplate.getForObject(baseUrl + "/benchmark/normal", String.class);
                blackhole.consume(result);
            }
            
            // Record memory every 1000 requests
            if (requestCount % 1000 == 0) {
                long currentHeap = memoryBean.getHeapMemoryUsage().getUsed();
                System.out.println("After " + requestCount + " requests: " + 
                    formatBytes(currentHeap - startHeap) + " growth");
            }
        }
        
        long endHeap = memoryBean.getHeapMemoryUsage().getUsed();
        System.out.println("\nMemory Growth Summary:");
        System.out.println("  Total Requests: " + requestCount);
        System.out.println("  Total Errors: " + errorCount);
        System.out.println("  Heap Growth: " + formatBytes(endHeap - startHeap));
        System.out.println("  Growth per 1K requests: " + 
            formatBytes((endHeap - startHeap) * 1000 / requestCount));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void measureObjectAllocationPerError(Blackhole blackhole) {
        // Measure allocation for processing an error
        long before = getAllocatedBytes();
        
        try {
            restTemplate.getForObject(baseUrl + "/benchmark/error", String.class);
        } catch (HttpServerErrorException e) {
            blackhole.consume(e);
        }
        
        long after = getAllocatedBytes();
        blackhole.consume(after - before);
    }

    @Benchmark
    @Measurement(iterations = 1, time = 120, timeUnit = TimeUnit.SECONDS)
    public void measureAnalyticsRetentionMemory(Blackhole blackhole) {
        if (!"analytics".equals(profile) && !"full".equals(profile)) {
            blackhole.consume(0);
            return;
        }
        
        long startHeap = memoryBean.getHeapMemoryUsage().getUsed();
        
        // Generate errors for 2 minutes to build up analytics data
        long endTime = System.currentTimeMillis() + 120_000;
        long errorCount = 0;
        
        while (System.currentTimeMillis() < endTime) {
            for (int i = 0; i < 10; i++) {
                try {
                    restTemplate.getForObject(baseUrl + "/benchmark/error", String.class);
                } catch (HttpServerErrorException e) {
                    errorCount++;
                    blackhole.consume(e);
                }
            }
            
            // Check memory every 10 seconds
            if (errorCount % 100 == 0) {
                long currentHeap = memoryBean.getHeapMemoryUsage().getUsed();
                System.out.println("Analytics retention after " + errorCount + " errors: " + 
                    formatBytes(currentHeap - startHeap));
            }
            
            try {
                Thread.sleep(100); // Pace the errors
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        long endHeap = memoryBean.getHeapMemoryUsage().getUsed();
        System.out.println("\nAnalytics Memory Usage:");
        System.out.println("  Total Errors: " + errorCount);
        System.out.println("  Memory Used: " + formatBytes(endHeap - startHeap));
        System.out.println("  Per Error: " + formatBytes((endHeap - startHeap) / errorCount));
    }

    @State(Scope.Thread)
    public static class GCState {
        long gcCountBefore;
        long gcTimeBefore;
        
        @Setup(Level.Invocation)
        public void setup() {
            gcCountBefore = getGCCount();
            gcTimeBefore = getGCTime();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Measurement(iterations = 5, time = 30, timeUnit = TimeUnit.SECONDS)
    public void measureGCPressure(GCState gcState, Blackhole blackhole) {
        // Generate high error rate to stress GC
        for (int i = 0; i < 10000; i++) {
            try {
                restTemplate.getForObject(baseUrl + "/benchmark/error", String.class);
            } catch (HttpServerErrorException e) {
                blackhole.consume(e);
            }
        }
        
        long gcCountAfter = getGCCount();
        long gcTimeAfter = getGCTime();
        
        System.out.println("GC Activity:");
        System.out.println("  GC Count: " + (gcCountAfter - gcState.gcCountBefore));
        System.out.println("  GC Time: " + (gcTimeAfter - gcState.gcTimeBefore) + " ms");
        
        blackhole.consume(gcCountAfter);
    }

    private void recordMemoryUsage(String label) {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        
        System.out.println("\n" + label + ":");
        System.out.println("  Heap: " + formatMemoryUsage(heap));
        System.out.println("  Non-Heap: " + formatMemoryUsage(nonHeap));
    }

    private String formatMemoryUsage(MemoryUsage usage) {
        return String.format("Used: %s, Committed: %s, Max: %s",
            formatBytes(usage.getUsed()),
            formatBytes(usage.getCommitted()),
            formatBytes(usage.getMax()));
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private long getAllocatedBytes() {
        // This is an approximation
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    private static long getGCCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
            .mapToLong(gc -> gc.getCollectionCount())
            .sum();
    }

    private static long getGCTime() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
            .mapToLong(gc -> gc.getCollectionTime())
            .sum();
    }

    private void waitForApplicationReady() {
        int maxRetries = 30;
        int retries = 0;
        
        while (retries < maxRetries) {
            try {
                restTemplate.getForObject(baseUrl + "/actuator/health", String.class);
                break;
            } catch (Exception e) {
                retries++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        if (retries >= maxRetries) {
            throw new RuntimeException("Application failed to start within timeout");
        }
    }
}