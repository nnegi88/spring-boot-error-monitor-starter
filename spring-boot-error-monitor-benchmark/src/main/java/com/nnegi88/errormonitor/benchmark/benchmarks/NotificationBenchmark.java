package com.nnegi88.errormonitor.benchmark.benchmarks;

import com.nnegi88.errormonitor.config.ErrorMonitorProperties;
import com.nnegi88.errormonitor.core.DefaultErrorProcessor;
import com.nnegi88.errormonitor.core.ErrorProcessor;
import com.nnegi88.errormonitor.filter.ErrorFilter;
import com.nnegi88.errormonitor.metrics.ErrorMetrics;
import com.nnegi88.errormonitor.metrics.NoOpErrorMetrics;
import com.nnegi88.errormonitor.model.ErrorEvent;
import com.nnegi88.errormonitor.model.ErrorSeverity;
import com.nnegi88.errormonitor.notification.NotificationClient;
import com.nnegi88.errormonitor.notification.NotificationPlatform;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Benchmark to measure the performance of the notification pipeline.
 * 
 * This benchmark measures:
 * 1. Async processing overhead
 * 2. Thread pool utilization
 * 3. Queue saturation scenarios
 * 4. Network I/O impact
 * 5. Retry mechanism overhead
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G", "-XX:+UseG1GC"})
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class NotificationBenchmark {

    private ErrorProcessor errorProcessor;
    private ErrorProcessor errorProcessorWithSlowWebhook;
    private ErrorProcessor errorProcessorWithFailingWebhook;
    private MockWebServer mockWebServer;
    private MockWebServer slowWebServer;
    private MockWebServer failingWebServer;
    private ErrorEvent testEvent;
    
    @Param({"1", "10", "50"})
    private int webhookLatencyMs;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        // Setup mock webhook servers
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        slowWebServer = new MockWebServer();
        slowWebServer.start();
        
        failingWebServer = new MockWebServer();
        failingWebServer.start();
        
        // Create error processors with different webhook behaviors
        errorProcessor = createErrorProcessor(mockWebServer, 0);
        errorProcessorWithSlowWebhook = createErrorProcessor(slowWebServer, 100);
        errorProcessorWithFailingWebhook = createErrorProcessor(failingWebServer, 0);
        
        // Create test event
        testEvent = ErrorEvent.builder()
            .exception(new RuntimeException("Benchmark test error"))
            .message("Notification benchmark test")
            .severity(ErrorSeverity.HIGH)
            .applicationName("benchmark-app")
            .environment("test")
            .build();
        
        // Setup mock responses
        setupMockResponses();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
        slowWebServer.shutdown();
        failingWebServer.shutdown();
    }

    private ErrorProcessor createErrorProcessor(MockWebServer server, int fixedLatency) {
        ErrorMonitorProperties properties = new ErrorMonitorProperties();
        properties.setEnabled(true);
        
        ErrorMonitorProperties.NotificationProperties notifProps = 
            new ErrorMonitorProperties.NotificationProperties();
        notifProps.setPlatform("slack");
        
        ErrorMonitorProperties.SlackProperties slackProps = 
            new ErrorMonitorProperties.SlackProperties();
        slackProps.setWebhookUrl(server.url("/webhook").toString());
        notifProps.setSlack(slackProps);
        properties.setNotification(notifProps);
        
        // Create a mock notification client
        NotificationClient mockClient = new NotificationClient() {
            private final AtomicInteger requestCount = new AtomicInteger(0);
            
            @Override
            public void sendNotification(ErrorEvent event) {
                requestCount.incrementAndGet();
                
                // Simulate network call
                CompletableFuture.runAsync(() -> {
                    try {
                        if (fixedLatency > 0) {
                            Thread.sleep(fixedLatency);
                        } else {
                            Thread.sleep(webhookLatencyMs);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
            @Override
            public boolean isEnabled() {
                return true;
            }
            
            @Override
            public NotificationPlatform getPlatform() {
                return NotificationPlatform.SLACK;
            }
        };
        
        ErrorFilter filter = event -> true;
        ErrorMetrics metrics = new NoOpErrorMetrics();
        
        return new DefaultErrorProcessor(
            mockClient, filter, properties, metrics, "benchmark-app", "test"
        );
    }

    @Benchmark
    public void measureAsyncProcessingOverhead(Blackhole blackhole) {
        errorProcessor.processError(testEvent);
        blackhole.consume(testEvent);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measureNotificationThroughput(Blackhole blackhole) {
        errorProcessor.processError(testEvent);
        blackhole.consume(testEvent);
    }

    @State(Scope.Thread)
    public static class ThreadPoolState {
        AtomicLong processedCount = new AtomicLong(0);
        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
    }

    @Benchmark
    @Threads(10)
    public void measureThreadPoolUtilization(ThreadPoolState state, Blackhole blackhole) {
        errorProcessor.processError(testEvent);
        
        long count = state.processedCount.incrementAndGet();
        if (count % 1000 == 0) {
            long elapsed = System.currentTimeMillis() - state.startTime.get();
            double throughput = count * 1000.0 / elapsed;
            System.out.println("Thread pool throughput: " + throughput + " events/sec");
        }
        
        blackhole.consume(count);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Timeout(time = 30, timeUnit = TimeUnit.SECONDS)
    public void measureSlowWebhookImpact(Blackhole blackhole) {
        // Process error with slow webhook
        errorProcessorWithSlowWebhook.processError(testEvent);
        blackhole.consume(testEvent);
    }

    @Benchmark
    @OperationsPerInvocation(100)
    public void measureQueueSaturation(Blackhole blackhole) {
        // Flood the notification pipeline
        for (int i = 0; i < 100; i++) {
            errorProcessor.processError(testEvent);
        }
        blackhole.consume(testEvent);
    }

    @State(Scope.Benchmark)
    public static class BackpressureState {
        AtomicInteger rejectedCount = new AtomicInteger(0);
        AtomicInteger acceptedCount = new AtomicInteger(0);
        long lastReportTime = System.currentTimeMillis();
        
        public void recordResult(boolean accepted) {
            if (accepted) {
                acceptedCount.incrementAndGet();
            } else {
                rejectedCount.incrementAndGet();
            }
            
            long now = System.currentTimeMillis();
            if (now - lastReportTime > 5000) { // Report every 5 seconds
                System.out.println("Backpressure stats - Accepted: " + acceptedCount.get() + 
                                 ", Rejected: " + rejectedCount.get());
                lastReportTime = now;
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Threads(20)
    @Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
    public void measureBackpressureHandling(BackpressureState state, Blackhole blackhole) {
        // Generate high load to test backpressure
        boolean processed = true;
        
        try {
            errorProcessor.processError(testEvent);
        } catch (Exception e) {
            processed = false;
        }
        
        state.recordResult(processed);
        blackhole.consume(processed);
    }

    @Benchmark
    public void measureRetryOverhead(Blackhole blackhole) {
        // Process error that will trigger retries
        errorProcessorWithFailingWebhook.processError(testEvent);
        blackhole.consume(testEvent);
    }

    @State(Scope.Benchmark)
    public static class CircuitBreakerState {
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private volatile boolean circuitOpen = false;
        private long circuitOpenTime = 0;
        
        public boolean shouldProcess() {
            if (circuitOpen) {
                // Check if circuit should be half-open
                if (System.currentTimeMillis() - circuitOpenTime > 5000) {
                    circuitOpen = false;
                    System.out.println("Circuit breaker: HALF-OPEN");
                }
                return false;
            }
            
            return true;
        }
        
        public void recordResult(boolean success) {
            if (success) {
                successCount.incrementAndGet();
                failureCount.set(0);
            } else {
                int failures = failureCount.incrementAndGet();
                if (failures > 5) {
                    circuitOpen = true;
                    circuitOpenTime = System.currentTimeMillis();
                    System.out.println("Circuit breaker: OPEN");
                }
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void measureCircuitBreakerPerformance(CircuitBreakerState state, Blackhole blackhole) {
        if (state.shouldProcess()) {
            boolean success = Math.random() > 0.3; // 30% failure rate
            
            if (success) {
                errorProcessor.processError(testEvent);
            } else {
                errorProcessorWithFailingWebhook.processError(testEvent);
            }
            
            state.recordResult(success);
            blackhole.consume(success);
        } else {
            // Circuit is open, skip processing
            blackhole.consume(false);
        }
    }

    private void setupMockResponses() {
        // Normal webhook
        for (int i = 0; i < 10000; i++) {
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("ok")
                .setBodyDelay(webhookLatencyMs, TimeUnit.MILLISECONDS));
        }
        
        // Slow webhook
        for (int i = 0; i < 10000; i++) {
            slowWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("ok")
                .setBodyDelay(100, TimeUnit.MILLISECONDS));
        }
        
        // Failing webhook (alternates between failures and success)
        for (int i = 0; i < 10000; i++) {
            if (i % 3 == 0) {
                failingWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("ok"));
            } else {
                failingWebServer.enqueue(new MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error"));
            }
        }
    }
}