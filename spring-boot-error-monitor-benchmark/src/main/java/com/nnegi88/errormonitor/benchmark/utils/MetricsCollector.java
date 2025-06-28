package com.nnegi88.errormonitor.benchmark.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects and reports metrics during benchmark execution.
 */
public class MetricsCollector {
    
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final ObjectMapper objectMapper;
    
    private final Map<String, List<DataPoint>> timeSeriesData;
    private final Map<String, AtomicLong> counters;
    private final ScheduledExecutorService scheduler;
    
    private volatile boolean collecting = false;
    private long startTime;

    public MetricsCollector(String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.objectMapper = new ObjectMapper();
        
        this.timeSeriesData = new ConcurrentHashMap<>();
        this.counters = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void startCollection() {
        if (collecting) {
            return;
        }
        
        collecting = true;
        startTime = System.currentTimeMillis();
        
        // Initialize counters
        counters.put("total_requests", new AtomicLong(0));
        counters.put("total_errors", new AtomicLong(0));
        counters.put("notifications_sent", new AtomicLong(0));
        
        // Start periodic collection
        scheduler.scheduleAtFixedRate(this::collectMetrics, 0, 1, TimeUnit.SECONDS);
    }

    public void stopCollection() {
        collecting = false;
        scheduler.shutdown();
        
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void collectMetrics() {
        if (!collecting) {
            return;
        }
        
        long timestamp = System.currentTimeMillis() - startTime;
        
        // Collect JVM metrics
        collectJVMMetrics(timestamp);
        
        // Collect application metrics via actuator
        collectActuatorMetrics(timestamp);
        
        // Collect error monitor specific metrics
        collectErrorMonitorMetrics(timestamp);
    }

    private void collectJVMMetrics(long timestamp) {
        // Memory metrics
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
        
        addDataPoint("heap_used_mb", timestamp, heapUsed / (1024.0 * 1024));
        addDataPoint("heap_max_mb", timestamp, heapMax / (1024.0 * 1024));
        addDataPoint("non_heap_used_mb", timestamp, nonHeapUsed / (1024.0 * 1024));
        
        // Thread metrics
        int threadCount = threadBean.getThreadCount();
        int peakThreadCount = threadBean.getPeakThreadCount();
        
        addDataPoint("thread_count", timestamp, threadCount);
        addDataPoint("peak_thread_count", timestamp, peakThreadCount);
        
        // CPU metrics (approximation)
        double processCpuLoad = getProcessCpuLoad();
        addDataPoint("cpu_usage_percent", timestamp, processCpuLoad * 100);
    }

    private void collectActuatorMetrics(long timestamp) {
        try {
            // Get metrics from actuator endpoint
            Map<String, Object> metrics = restTemplate.getForObject(
                baseUrl + "/actuator/metrics", Map.class);
            
            if (metrics != null && metrics.containsKey("names")) {
                List<String> metricNames = (List<String>) metrics.get("names");
                
                // Collect specific metrics
                collectMetricValue("http.server.requests", timestamp);
                collectMetricValue("error.monitor.errors.total", timestamp);
                collectMetricValue("error.monitor.notifications", timestamp);
                collectMetricValue("error.monitor.processing.time", timestamp);
            }
        } catch (Exception e) {
            // Ignore errors during metric collection
        }
    }

    private void collectMetricValue(String metricName, long timestamp) {
        try {
            Map<String, Object> metric = restTemplate.getForObject(
                baseUrl + "/actuator/metrics/" + metricName, Map.class);
            
            if (metric != null && metric.containsKey("measurements")) {
                List<Map<String, Object>> measurements = 
                    (List<Map<String, Object>>) metric.get("measurements");
                
                for (Map<String, Object> measurement : measurements) {
                    String statistic = (String) measurement.get("statistic");
                    double value = ((Number) measurement.get("value")).doubleValue();
                    
                    String key = metricName + "." + statistic.toLowerCase();
                    addDataPoint(key, timestamp, value);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private void collectErrorMonitorMetrics(long timestamp) {
        try {
            // Get error monitor status
            Map<String, Object> status = restTemplate.getForObject(
                baseUrl + "/actuator/errorMonitor", Map.class);
            
            if (status != null) {
                Map<String, Object> statistics = (Map<String, Object>) status.get("statistics");
                if (statistics != null) {
                    Number totalErrors = (Number) statistics.get("totalErrors");
                    if (totalErrors != null) {
                        addDataPoint("error_monitor.total_errors", timestamp, 
                            totalErrors.doubleValue());
                    }
                }
            }
            
            // Get error statistics
            Map<String, Object> errorStats = restTemplate.getForObject(
                baseUrl + "/actuator/errorStatistics", Map.class);
            
            if (errorStats != null && errorStats.containsKey("summary")) {
                Map<String, Object> summary = (Map<String, Object>) errorStats.get("summary");
                Number errorRate = (Number) summary.get("errorsPerMinute");
                if (errorRate != null) {
                    addDataPoint("error_rate_per_minute", timestamp, errorRate.doubleValue());
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private void addDataPoint(String metric, long timestamp, double value) {
        timeSeriesData.computeIfAbsent(metric, k -> new ArrayList<>())
            .add(new DataPoint(timestamp, value));
    }

    public void incrementCounter(String name) {
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void printReport() {
        System.out.println("\n=== METRICS REPORT ===");
        
        // Print counters
        System.out.println("\nCounters:");
        counters.forEach((name, value) -> {
            System.out.println("  " + name + ": " + value.get());
        });
        
        // Print time series summary
        System.out.println("\nTime Series Metrics:");
        timeSeriesData.forEach((metric, dataPoints) -> {
            if (!dataPoints.isEmpty()) {
                double min = dataPoints.stream().mapToDouble(dp -> dp.value).min().orElse(0);
                double max = dataPoints.stream().mapToDouble(dp -> dp.value).max().orElse(0);
                double avg = dataPoints.stream().mapToDouble(dp -> dp.value).average().orElse(0);
                double last = dataPoints.get(dataPoints.size() - 1).value;
                
                System.out.printf("  %s: min=%.2f, max=%.2f, avg=%.2f, last=%.2f%n",
                    metric, min, max, avg, last);
            }
        });
        
        // Print percentiles for key metrics
        printPercentiles("heap_used_mb", "Heap Usage (MB)");
        printPercentiles("cpu_usage_percent", "CPU Usage (%)");
        printPercentiles("error_rate_per_minute", "Error Rate (per minute)");
        
        System.out.println("\n===================");
    }

    private void printPercentiles(String metric, String displayName) {
        List<DataPoint> dataPoints = timeSeriesData.get(metric);
        if (dataPoints == null || dataPoints.isEmpty()) {
            return;
        }
        
        double[] values = dataPoints.stream()
            .mapToDouble(dp -> dp.value)
            .sorted()
            .toArray();
        
        System.out.println("\n" + displayName + " Percentiles:");
        System.out.printf("  P50: %.2f%n", getPercentile(values, 50));
        System.out.printf("  P90: %.2f%n", getPercentile(values, 90));
        System.out.printf("  P95: %.2f%n", getPercentile(values, 95));
        System.out.printf("  P99: %.2f%n", getPercentile(values, 99));
    }

    private double getPercentile(double[] sortedValues, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.length) - 1;
        return sortedValues[Math.max(0, Math.min(index, sortedValues.length - 1))];
    }

    private double getProcessCpuLoad() {
        try {
            javax.management.MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            javax.management.ObjectName name = javax.management.ObjectName.getInstance(
                "java.lang:type=OperatingSystem");
            javax.management.AttributeList list = mbs.getAttributes(name, 
                new String[]{"ProcessCpuLoad"});
            
            if (list.isEmpty()) return 0.0;
            
            javax.management.Attribute att = (javax.management.Attribute) list.get(0);
            Double value = (Double) att.getValue();
            
            return value != null ? value : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public void exportToJson(String filename) {
        try {
            Map<String, Object> report = new HashMap<>();
            report.put("startTime", startTime);
            report.put("duration", System.currentTimeMillis() - startTime);
            report.put("counters", counters);
            report.put("timeSeries", timeSeriesData);
            
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new java.io.File(filename), report);
            
            System.out.println("Metrics exported to: " + filename);
        } catch (Exception e) {
            System.err.println("Failed to export metrics: " + e.getMessage());
        }
    }

    private static class DataPoint {
        final long timestamp;
        final double value;
        
        DataPoint(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
}