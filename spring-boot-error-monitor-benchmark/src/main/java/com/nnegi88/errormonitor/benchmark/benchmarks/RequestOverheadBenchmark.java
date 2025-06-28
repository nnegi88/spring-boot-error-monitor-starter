package com.nnegi88.errormonitor.benchmark.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpServerErrorException;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark to measure the overhead of error monitoring on request processing.
 * 
 * This benchmark compares:
 * 1. Baseline (error monitor disabled)
 * 2. Error monitor enabled with no errors
 * 3. Error monitor enabled with various error rates
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G", "-XX:+UseG1GC"})
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class RequestOverheadBenchmark {

    private ConfigurableApplicationContext contextWithMonitor;
    private ConfigurableApplicationContext contextWithoutMonitor;
    private RestTemplate restTemplate;
    private String baseUrlWithMonitor = "http://localhost:8081";
    private String baseUrlWithoutMonitor = "http://localhost:8082";

    @Setup(Level.Trial)
    public void setup() {
        // Start application with error monitor enabled
        System.setProperty("server.port", "8081");
        System.setProperty("spring.profiles.active", "benchmark-enabled");
        contextWithMonitor = SpringApplication.run(
            com.springboot.errormonitor.benchmark.utils.BenchmarkApplication.class,
            "--server.port=8081",
            "--spring.profiles.active=benchmark-enabled"
        );
        
        // Start application with error monitor disabled
        System.setProperty("server.port", "8082");
        System.setProperty("spring.profiles.active", "benchmark-disabled");
        contextWithoutMonitor = SpringApplication.run(
            com.springboot.errormonitor.benchmark.utils.BenchmarkApplication.class,
            "--server.port=8082", 
            "--spring.profiles.active=benchmark-disabled"
        );
        
        restTemplate = new RestTemplate();
        
        // Wait for applications to be ready
        waitForApplicationReady();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (contextWithMonitor != null) {
            contextWithMonitor.close();
        }
        if (contextWithoutMonitor != null) {
            contextWithoutMonitor.close();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void baseline_normalRequest(Blackhole blackhole) {
        String result = restTemplate.getForObject(
            baseUrlWithoutMonitor + "/benchmark/normal", 
            String.class
        );
        blackhole.consume(result);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void withMonitor_normalRequest(Blackhole blackhole) {
        String result = restTemplate.getForObject(
            baseUrlWithMonitor + "/benchmark/normal", 
            String.class
        );
        blackhole.consume(result);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void baseline_errorRequest(Blackhole blackhole) {
        try {
            restTemplate.getForObject(
                baseUrlWithoutMonitor + "/benchmark/error", 
                String.class
            );
        } catch (HttpServerErrorException e) {
            blackhole.consume(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void withMonitor_errorRequest(Blackhole blackhole) {
        try {
            restTemplate.getForObject(
                baseUrlWithMonitor + "/benchmark/error", 
                String.class
            );
        } catch (HttpServerErrorException e) {
            blackhole.consume(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OperationsPerInvocation(100)
    public void withMonitor_mixedRequests_1percentError(Blackhole blackhole) {
        for (int i = 0; i < 100; i++) {
            String result = restTemplate.getForObject(
                baseUrlWithMonitor + "/benchmark/conditional-error?errorRate=1", 
                String.class
            );
            blackhole.consume(result);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OperationsPerInvocation(100)
    public void withMonitor_mixedRequests_10percentError(Blackhole blackhole) {
        for (int i = 0; i < 100; i++) {
            try {
                String result = restTemplate.getForObject(
                    baseUrlWithMonitor + "/benchmark/conditional-error?errorRate=10", 
                    String.class
                );
                blackhole.consume(result);
            } catch (HttpServerErrorException e) {
                blackhole.consume(e);
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput_normalRequests(Blackhole blackhole) {
        String result = restTemplate.getForObject(
            baseUrlWithMonitor + "/benchmark/normal", 
            String.class
        );
        blackhole.consume(result);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput_errorRequests(Blackhole blackhole) {
        try {
            restTemplate.getForObject(
                baseUrlWithMonitor + "/benchmark/error", 
                String.class
            );
        } catch (HttpServerErrorException e) {
            blackhole.consume(e);
        }
    }

    @State(Scope.Thread)
    public static class ThreadState {
        int requestCount = 0;
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Threads(10)
    public void concurrent_normalRequests(ThreadState state, Blackhole blackhole) {
        state.requestCount++;
        String result = restTemplate.getForObject(
            baseUrlWithMonitor + "/benchmark/normal", 
            String.class
        );
        blackhole.consume(result);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Threads(10)
    public void concurrent_errorRequests(ThreadState state, Blackhole blackhole) {
        state.requestCount++;
        try {
            restTemplate.getForObject(
                baseUrlWithMonitor + "/benchmark/error", 
                String.class
            );
        } catch (HttpServerErrorException e) {
            blackhole.consume(e);
        }
    }

    private void waitForApplicationReady() {
        int maxRetries = 30;
        int retries = 0;
        
        while (retries < maxRetries) {
            try {
                restTemplate.getForObject(baseUrlWithMonitor + "/actuator/health", String.class);
                restTemplate.getForObject(baseUrlWithoutMonitor + "/actuator/health", String.class);
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
            throw new RuntimeException("Applications failed to start within timeout");
        }
    }
}