package com.nnegi88.errormonitor.benchmark.scenarios;

import com.nnegi88.errormonitor.benchmark.utils.BenchmarkApplication;
import com.nnegi88.errormonitor.benchmark.utils.MetricsCollector;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Load testing scenarios for comprehensive performance evaluation.
 */
public class LoadTestScenarios {

    private final ConfigurableApplicationContext context;
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final MetricsCollector metricsCollector;
    private final ExecutorService executor;

    public LoadTestScenarios(String profile, int port) {
        // Start application with specified profile
        this.context = SpringApplication.run(
            BenchmarkApplication.class,
            "--server.port=" + port,
            "--spring.profiles.active=" + profile
        );
        
        this.baseUrl = "http://localhost:" + port;
        this.restTemplate = new RestTemplate();
        this.metricsCollector = new MetricsCollector(baseUrl);
        this.executor = Executors.newFixedThreadPool(100);
        
        waitForApplicationReady();
    }

    /**
     * Sustained Load Test - 1 hour continuous load at specified RPS
     */
    public void runSustainedLoadTest(int targetRPS, int durationMinutes) {
        System.out.println("\n=== SUSTAINED LOAD TEST ===");
        System.out.println("Target RPS: " + targetRPS);
        System.out.println("Duration: " + durationMinutes + " minutes");
        System.out.println("========================\n");

        metricsCollector.startCollection();
        
        AtomicLong totalRequests = new AtomicLong(0);
        AtomicLong totalErrors = new AtomicLong(0);
        AtomicLong totalLatency = new AtomicLong(0);
        
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        CountDownLatch latch = new CountDownLatch(1);
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationMinutes * 60 * 1000);
        
        // Schedule requests at target RPS
        scheduler.scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis() > endTime) {
                latch.countDown();
                return;
            }
            
            // Submit requests for this interval
            for (int i = 0; i < targetRPS / 10; i++) { // Divide by 10 for 100ms intervals
                executor.submit(() -> {
                    long requestStart = System.currentTimeMillis();
                    boolean isError = Math.random() < 0.05; // 5% error rate
                    
                    try {
                        if (isError) {
                            restTemplate.getForObject(baseUrl + "/benchmark/error", String.class);
                        } else {
                            restTemplate.getForObject(baseUrl + "/benchmark/normal", String.class);
                        }
                    } catch (HttpServerErrorException e) {
                        totalErrors.incrementAndGet();
                    } finally {
                        long latency = System.currentTimeMillis() - requestStart;
                        totalRequests.incrementAndGet();
                        totalLatency.addAndGet(latency);
                    }
                });
            }
            
            // Print progress every 10 seconds
            if (totalRequests.get() % (targetRPS * 10) == 0) {
                printProgress(totalRequests.get(), totalErrors.get(), 
                    totalLatency.get() / Math.max(1, totalRequests.get()));
            }
            
        }, 0, 100, TimeUnit.MILLISECONDS);
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        scheduler.shutdown();
        
        // Final results
        long duration = System.currentTimeMillis() - startTime;
        printTestResults("Sustained Load Test", totalRequests.get(), totalErrors.get(), 
            totalLatency.get(), duration);
        
        metricsCollector.stopCollection();
        metricsCollector.printReport();
    }

    /**
     * Spike Load Test - Sudden increase from low to high RPS
     */
    public void runSpikeLoadTest(int baseRPS, int spikeRPS, int spikeDurationSeconds) {
        System.out.println("\n=== SPIKE LOAD TEST ===");
        System.out.println("Base RPS: " + baseRPS);
        System.out.println("Spike RPS: " + spikeRPS);
        System.out.println("Spike Duration: " + spikeDurationSeconds + " seconds");
        System.out.println("=====================\n");

        metricsCollector.startCollection();
        
        AtomicLong totalRequests = new AtomicLong(0);
        AtomicLong totalErrors = new AtomicLong(0);
        AtomicLong totalLatency = new AtomicLong(0);
        AtomicInteger currentRPS = new AtomicInteger(baseRPS);
        
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        
        // Run for 5 minutes total
        long testDuration = 5 * 60 * 1000;
        long startTime = System.currentTimeMillis();
        
        // Schedule spike after 1 minute
        scheduler.schedule(() -> {
            System.out.println("\n*** SPIKE STARTED ***");
            currentRPS.set(spikeRPS);
            
            // Schedule spike end
            scheduler.schedule(() -> {
                System.out.println("\n*** SPIKE ENDED ***");
                currentRPS.set(baseRPS);
            }, spikeDurationSeconds, TimeUnit.SECONDS);
            
        }, 60, TimeUnit.SECONDS);
        
        // Generate load
        CountDownLatch latch = new CountDownLatch(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis() - startTime > testDuration) {
                latch.countDown();
                return;
            }
            
            int rps = currentRPS.get();
            
            for (int i = 0; i < rps / 10; i++) {
                executor.submit(() -> {
                    long requestStart = System.currentTimeMillis();
                    
                    try {
                        restTemplate.getForObject(baseUrl + "/benchmark/normal", String.class);
                    } catch (Exception e) {
                        totalErrors.incrementAndGet();
                    } finally {
                        long latency = System.currentTimeMillis() - requestStart;
                        totalRequests.incrementAndGet();
                        totalLatency.addAndGet(latency);
                    }
                });
            }
            
        }, 0, 100, TimeUnit.MILLISECONDS);
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        scheduler.shutdown();
        
        // Final results
        long duration = System.currentTimeMillis() - startTime;
        printTestResults("Spike Load Test", totalRequests.get(), totalErrors.get(), 
            totalLatency.get(), duration);
        
        metricsCollector.stopCollection();
        metricsCollector.printReport();
    }

    /**
     * Error Storm Test - Simulate cascading failures
     */
    public void runErrorStormTest(int normalRPS, int stormDurationMinutes, double errorRate) {
        System.out.println("\n=== ERROR STORM TEST ===");
        System.out.println("Normal RPS: " + normalRPS);
        System.out.println("Storm Duration: " + stormDurationMinutes + " minutes");
        System.out.println("Error Rate: " + (errorRate * 100) + "%");
        System.out.println("======================\n");

        metricsCollector.startCollection();
        
        AtomicLong totalRequests = new AtomicLong(0);
        AtomicLong totalErrors = new AtomicLong(0);
        AtomicLong notificationBacklog = new AtomicLong(0);
        
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        CountDownLatch latch = new CountDownLatch(1);
        
        long startTime = System.currentTimeMillis();
        long stormEndTime = startTime + (stormDurationMinutes * 60 * 1000);
        long testEndTime = stormEndTime + (2 * 60 * 1000); // Run 2 more minutes after storm
        
        scheduler.scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis() > testEndTime) {
                latch.countDown();
                return;
            }
            
            boolean inStorm = System.currentTimeMillis() < stormEndTime;
            double currentErrorRate = inStorm ? errorRate : 0.01; // 1% error rate normally
            
            for (int i = 0; i < normalRPS / 10; i++) {
                executor.submit(() -> {
                    boolean shouldError = Math.random() < currentErrorRate;
                    
                    try {
                        if (shouldError) {
                            restTemplate.getForObject(baseUrl + "/benchmark/error", String.class);
                        } else {
                            restTemplate.getForObject(baseUrl + "/benchmark/normal", String.class);
                        }
                    } catch (HttpServerErrorException e) {
                        totalErrors.incrementAndGet();
                        notificationBacklog.incrementAndGet();
                    } finally {
                        totalRequests.incrementAndGet();
                    }
                });
            }
            
            // Monitor notification backlog
            if (totalRequests.get() % 1000 == 0) {
                long backlog = notificationBacklog.get();
                System.out.println("Notification backlog: " + backlog);
                
                // Simulate notification processing
                long processed = Math.min(backlog, 50); // Process up to 50 per interval
                notificationBacklog.addAndGet(-processed);
            }
            
        }, 0, 100, TimeUnit.MILLISECONDS);
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        scheduler.shutdown();
        
        // Final results
        System.out.println("\nError Storm Test Results:");
        System.out.println("Total Requests: " + totalRequests.get());
        System.out.println("Total Errors: " + totalErrors.get());
        System.out.println("Final Notification Backlog: " + notificationBacklog.get());
        
        metricsCollector.stopCollection();
        metricsCollector.printReport();
    }

    /**
     * Concurrent Users Test - Simulate many concurrent users
     */
    public void runConcurrentUsersTest(int numUsers, int requestsPerUser) {
        System.out.println("\n=== CONCURRENT USERS TEST ===");
        System.out.println("Number of Users: " + numUsers);
        System.out.println("Requests per User: " + requestsPerUser);
        System.out.println("===========================\n");

        metricsCollector.startCollection();
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numUsers);
        
        List<Future<UserResult>> futures = new ArrayList<>();
        
        // Create user tasks
        for (int userId = 0; userId < numUsers; userId++) {
            final int id = userId;
            
            Future<UserResult> future = executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all users to be ready
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return new UserResult(id, 0, 0, 0);
                }
                
                long userLatency = 0;
                int userErrors = 0;
                
                for (int i = 0; i < requestsPerUser; i++) {
                    long start = System.currentTimeMillis();
                    
                    try {
                        // Mix of operations
                        if (i % 10 == 0) {
                            restTemplate.getForObject(baseUrl + "/benchmark/heavy", String.class);
                        } else if (i % 20 == 0) {
                            restTemplate.getForObject(baseUrl + "/benchmark/error", String.class);
                        } else {
                            restTemplate.getForObject(baseUrl + "/benchmark/normal", String.class);
                        }
                    } catch (Exception e) {
                        userErrors++;
                    }
                    
                    userLatency += (System.currentTimeMillis() - start);
                    
                    // Simulate think time
                    try {
                        Thread.sleep(10 + (int)(Math.random() * 40));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                endLatch.countDown();
                return new UserResult(id, requestsPerUser, userErrors, userLatency);
            });
            
            futures.add(future);
        }
        
        // Start all users simultaneously
        long testStart = System.currentTimeMillis();
        startLatch.countDown();
        
        // Wait for completion
        try {
            endLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long testDuration = System.currentTimeMillis() - testStart;
        
        // Collect results
        long totalRequests = 0;
        long totalErrors = 0;
        long totalLatency = 0;
        
        for (Future<UserResult> future : futures) {
            try {
                UserResult result = future.get();
                totalRequests += result.requests;
                totalErrors += result.errors;
                totalLatency += result.totalLatency;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        printTestResults("Concurrent Users Test", totalRequests, totalErrors, 
            totalLatency, testDuration);
        
        metricsCollector.stopCollection();
        metricsCollector.printReport();
    }

    private void printProgress(long requests, long errors, long avgLatency) {
        System.out.printf("Progress - Requests: %d, Errors: %d, Avg Latency: %d ms%n", 
            requests, errors, avgLatency);
    }

    private void printTestResults(String testName, long totalRequests, long totalErrors, 
                                long totalLatency, long duration) {
        System.out.println("\n=== " + testName + " Results ===");
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Total Errors: " + totalErrors);
        System.out.println("Error Rate: " + String.format("%.2f%%", 
            (totalErrors * 100.0) / totalRequests));
        System.out.println("Average Latency: " + (totalLatency / Math.max(1, totalRequests)) + " ms");
        System.out.println("Actual RPS: " + (totalRequests * 1000 / duration));
        System.out.println("Test Duration: " + (duration / 1000) + " seconds");
        System.out.println("========================\n");
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

    public void cleanup() {
        executor.shutdown();
        if (context != null) {
            context.close();
        }
    }

    private static class UserResult {
        final int userId;
        final int requests;
        final int errors;
        final long totalLatency;
        
        UserResult(int userId, int requests, int errors, long totalLatency) {
            this.userId = userId;
            this.requests = requests;
            this.errors = errors;
            this.totalLatency = totalLatency;
        }
    }

    public static void main(String[] args) {
        // Example usage
        LoadTestScenarios scenarios = new LoadTestScenarios("benchmark-full", 8090);
        
        try {
            // Run different scenarios
            scenarios.runSustainedLoadTest(100, 2); // 100 RPS for 2 minutes
            scenarios.runSpikeLoadTest(50, 500, 30); // Spike from 50 to 500 RPS
            scenarios.runErrorStormTest(100, 1, 0.9); // 90% error rate storm
            scenarios.runConcurrentUsersTest(50, 100); // 50 users, 100 requests each
        } finally {
            scenarios.cleanup();
        }
    }
}