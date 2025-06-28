package com.nnegi88.errormonitor.benchmark.benchmarks;

import com.nnegi88.errormonitor.config.ErrorMonitorProperties;
import com.nnegi88.errormonitor.filter.RateLimitingErrorFilter;
import com.nnegi88.errormonitor.model.ErrorEvent;
import com.nnegi88.errormonitor.model.ErrorSeverity;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Benchmark to measure the performance of rate limiting mechanisms.
 * 
 * This benchmark measures:
 * 1. Overhead of rate limit checks
 * 2. Performance under burst scenarios
 * 3. Concurrent access performance
 * 4. Memory usage of timestamp tracking
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms1G", "-Xmx1G", "-XX:+UseG1GC"})
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class RateLimitingBenchmark {

    private RateLimitingErrorFilter rateLimitFilter;
    private RateLimitingErrorFilter strictRateLimitFilter;
    private RateLimitingErrorFilter relaxedRateLimitFilter;
    private ErrorEvent testEvent;
    
    @Param({"10", "100", "1000"})
    private int maxErrorsPerMinute;
    
    @Param({"5", "10", "50"})
    private int burstLimit;

    @Setup(Level.Trial)
    public void setup() {
        // Standard rate limiter with parameterized values
        ErrorMonitorProperties.RateLimitingProperties props = 
            new ErrorMonitorProperties.RateLimitingProperties();
        props.setMaxErrorsPerMinute(maxErrorsPerMinute);
        props.setBurstLimit(burstLimit);
        rateLimitFilter = new RateLimitingErrorFilter(props);
        
        // Strict rate limiter (low limits)
        ErrorMonitorProperties.RateLimitingProperties strictProps = 
            new ErrorMonitorProperties.RateLimitingProperties();
        strictProps.setMaxErrorsPerMinute(10);
        strictProps.setBurstLimit(2);
        strictRateLimitFilter = new RateLimitingErrorFilter(strictProps);
        
        // Relaxed rate limiter (high limits)
        ErrorMonitorProperties.RateLimitingProperties relaxedProps = 
            new ErrorMonitorProperties.RateLimitingProperties();
        relaxedProps.setMaxErrorsPerMinute(10000);
        relaxedProps.setBurstLimit(1000);
        relaxedRateLimitFilter = new RateLimitingErrorFilter(relaxedProps);
        
        // Create test event
        testEvent = ErrorEvent.builder()
            .exception(new RuntimeException("Test error"))
            .message("Benchmark test error")
            .severity(ErrorSeverity.ERROR)
            .build();
    }

    @Benchmark
    public boolean measureSingleCheck(Blackhole blackhole) {
        boolean result = rateLimitFilter.shouldReport(testEvent);
        blackhole.consume(result);
        return result;
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measureThroughput_AllowedRequests(Blackhole blackhole) {
        // Use relaxed filter to ensure most requests pass
        boolean result = relaxedRateLimitFilter.shouldReport(testEvent);
        blackhole.consume(result);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measureThroughput_RateLimitedRequests(Blackhole blackhole) {
        // Use strict filter to ensure many requests are rejected
        boolean result = strictRateLimitFilter.shouldReport(testEvent);
        blackhole.consume(result);
    }

    @State(Scope.Thread)
    public static class BurstState {
        AtomicInteger counter = new AtomicInteger(0);
        long lastReset = System.currentTimeMillis();
        
        public boolean shouldGenerateBurst() {
            long now = System.currentTimeMillis();
            if (now - lastReset > 1000) { // Reset every second
                counter.set(0);
                lastReset = now;
            }
            
            // Generate burst for first 20 calls per second
            return counter.incrementAndGet() <= 20;
        }
    }

    @Benchmark
    @Threads(4)
    public void measureBurstScenario(BurstState state, Blackhole blackhole) {
        if (state.shouldGenerateBurst()) {
            // Rapid fire during burst
            for (int i = 0; i < 10; i++) {
                boolean result = rateLimitFilter.shouldReport(testEvent);
                blackhole.consume(result);
            }
        } else {
            // Normal rate
            boolean result = rateLimitFilter.shouldReport(testEvent);
            blackhole.consume(result);
        }
    }

    @Benchmark
    @Threads(8)
    @BenchmarkMode(Mode.AverageTime)
    public void measureConcurrentAccess(Blackhole blackhole) {
        boolean result = rateLimitFilter.shouldReport(testEvent);
        blackhole.consume(result);
    }

    @State(Scope.Benchmark)
    public static class MemoryState {
        RateLimitingErrorFilter memoryTestFilter;
        long baselineMemory;
        
        @Setup(Level.Trial)
        public void setup() {
            ErrorMonitorProperties.RateLimitingProperties props = 
                new ErrorMonitorProperties.RateLimitingProperties();
            props.setMaxErrorsPerMinute(1000);
            props.setBurstLimit(100);
            memoryTestFilter = new RateLimitingErrorFilter(props);
            
            // Get baseline memory
            System.gc();
            baselineMemory = Runtime.getRuntime().totalMemory() - 
                            Runtime.getRuntime().freeMemory();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Measurement(iterations = 1)
    public void measureMemoryGrowth(MemoryState state, Blackhole blackhole) {
        // Fill up the rate limiter's timestamp queue
        ErrorEvent event = ErrorEvent.builder()
            .exception(new RuntimeException("Memory test"))
            .build();
            
        // Generate many events to fill the queue
        for (int i = 0; i < 1000; i++) {
            boolean result = state.memoryTestFilter.shouldReport(event);
            blackhole.consume(result);
            
            // Small delay to spread timestamps
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Measure memory after filling queue
        long currentMemory = Runtime.getRuntime().totalMemory() - 
                           Runtime.getRuntime().freeMemory();
        long memoryGrowth = currentMemory - state.baselineMemory;
        
        System.out.println("Memory growth after 1000 timestamps: " + 
            (memoryGrowth / 1024) + " KB");
        
        blackhole.consume(memoryGrowth);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OperationsPerInvocation(100)
    public void measureCleanupOverhead(Blackhole blackhole) {
        // This measures the overhead of cleaning old timestamps
        // Generate events over time to trigger cleanup
        for (int i = 0; i < 100; i++) {
            boolean result = rateLimitFilter.shouldReport(testEvent);
            blackhole.consume(result);
            
            if (i % 10 == 0) {
                // Simulate time passing
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @State(Scope.Thread)
    public static class RealisticLoadState {
        private final AtomicInteger errorCounter = new AtomicInteger(0);
        private long lastErrorTime = 0;
        
        public boolean shouldGenerateError() {
            long now = System.currentTimeMillis();
            
            // Simulate realistic error patterns:
            // - Occasional single errors
            // - Bursts of errors
            // - Periods of no errors
            
            int count = errorCounter.incrementAndGet();
            
            if (count % 1000 < 10) {
                // Burst: 10 errors out of every 1000 requests
                return true;
            } else if (count % 100 == 0) {
                // Periodic single error
                return true;
            } else if (now - lastErrorTime > 5000 && Math.random() < 0.01) {
                // Random error after quiet period
                lastErrorTime = now;
                return true;
            }
            
            return false;
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
    public void measureRealisticLoad(RealisticLoadState state, Blackhole blackhole) {
        if (state.shouldGenerateError()) {
            boolean result = rateLimitFilter.shouldReport(testEvent);
            blackhole.consume(result);
        } else {
            // Simulate checking if we should process a non-error
            blackhole.consume(false);
        }
    }
}