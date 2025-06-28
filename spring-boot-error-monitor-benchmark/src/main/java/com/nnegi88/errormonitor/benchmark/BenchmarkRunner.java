package com.nnegi88.errormonitor.benchmark;

import org.openjdk.jmh.Main;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.results.format.ResultFormatType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Main runner for all performance benchmarks.
 */
public class BenchmarkRunner {

    public static void main(String[] args) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path resultsDir = Paths.get("target", "benchmark-results", timestamp);
        Files.createDirectories(resultsDir);
        
        System.out.println("Spring Boot Error Monitor - Performance Benchmark Suite");
        System.out.println("======================================================");
        System.out.println("Timestamp: " + timestamp);
        System.out.println("Results directory: " + resultsDir);
        System.out.println();
        
        // Check if specific benchmark requested
        if (args.length > 0) {
            runSpecificBenchmark(args[0], resultsDir);
        } else {
            runAllBenchmarks(resultsDir);
        }
    }
    
    private static void runAllBenchmarks(Path resultsDir) {
        System.out.println("Running all benchmarks...\n");
        
        // 1. Request Processing Overhead
        runBenchmark("RequestOverheadBenchmark", 
            resultsDir.resolve("request-overhead.json"),
            "Request Processing Overhead Benchmark");
        
        // 2. Memory Usage
        runBenchmark("MemoryUsageBenchmark", 
            resultsDir.resolve("memory-usage.json"),
            "Memory Usage Benchmark");
        
        // 3. Rate Limiting
        runBenchmark("RateLimitingBenchmark", 
            resultsDir.resolve("rate-limiting.json"),
            "Rate Limiting Performance Benchmark");
        
        // 4. Notification Pipeline
        runBenchmark("NotificationBenchmark", 
            resultsDir.resolve("notification-pipeline.json"),
            "Notification Pipeline Benchmark");
        
        System.out.println("\nAll benchmarks completed!");
        System.out.println("Results saved to: " + resultsDir);
        
        generateSummaryReport(resultsDir);
    }
    
    private static void runSpecificBenchmark(String benchmarkName, Path resultsDir) {
        System.out.println("Running specific benchmark: " + benchmarkName);
        
        Path resultFile = resultsDir.resolve(benchmarkName.toLowerCase() + ".json");
        runBenchmark(benchmarkName, resultFile, benchmarkName);
    }
    
    private static void runBenchmark(String benchmarkClass, Path resultFile, String description) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Running: " + description);
        System.out.println("=".repeat(60));
        
        try {
            Options opt = new OptionsBuilder()
                .include(".*" + benchmarkClass + ".*")
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(10)
                .measurementTime(TimeValue.seconds(1))
                .threads(1)
                .forks(2)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .resultFormat(ResultFormatType.JSON)
                .result(resultFile.toString())
                .build();
                
            new Runner(opt).run();
            
            System.out.println("✓ Completed: " + description);
            System.out.println("  Results: " + resultFile);
            
        } catch (RunnerException e) {
            System.err.println("✗ Failed: " + description);
            e.printStackTrace();
        }
    }
    
    private static void generateSummaryReport(Path resultsDir) {
        System.out.println("\nGenerating summary report...");
        
        try {
            // Create a summary report combining all results
            Path summaryFile = resultsDir.resolve("benchmark-summary.txt");
            
            StringBuilder summary = new StringBuilder();
            summary.append("Spring Boot Error Monitor - Benchmark Summary\n");
            summary.append("=============================================\n");
            summary.append("Generated: ").append(LocalDateTime.now()).append("\n\n");
            
            // Add quick results summary
            summary.append("Quick Results:\n");
            summary.append("- Request Processing Overhead: Check request-overhead.json\n");
            summary.append("- Memory Usage: Check memory-usage.json\n");
            summary.append("- Rate Limiting: Check rate-limiting.json\n");
            summary.append("- Notification Pipeline: Check notification-pipeline.json\n");
            
            Files.write(summaryFile, summary.toString().getBytes());
            
            System.out.println("Summary report: " + summaryFile);
            
        } catch (IOException e) {
            System.err.println("Failed to generate summary report: " + e.getMessage());
        }
    }
    
    /**
     * Quick benchmark mode for development
     */
    public static class QuickBenchmark {
        public static void main(String[] args) throws RunnerException {
            Options opt = new OptionsBuilder()
                .include(".*RequestOverheadBenchmark.*normalRequest.*")
                .warmupIterations(2)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(3)
                .measurementTime(TimeValue.seconds(1))
                .forks(1)
                .build();
                
            new Runner(opt).run();
        }
    }
}