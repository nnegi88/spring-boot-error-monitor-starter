# Spring Boot Error Monitor - Performance Benchmarks

This module contains comprehensive performance benchmarks for the Spring Boot Error Monitor library.

## Overview

The benchmark suite measures:
- Request processing overhead
- Memory usage and growth
- Rate limiting performance
- Notification pipeline efficiency
- Load handling capabilities

## Running Benchmarks

### Prerequisites
- Java 11+
- Maven 3.6+
- At least 4GB RAM allocated to JVM
- CPU performance mode enabled (disable throttling)

### Quick Start

1. Build the error monitor library first:
```bash
cd ..
mvn clean install -DskipTests
```

2. Run all benchmarks:
```bash
cd spring-boot-error-monitor-benchmark
mvn clean compile
mvn exec:java -Dexec.mainClass="com.nnegi88.errormonitor.benchmark.BenchmarkRunner"
```

### Running Specific Benchmarks

```bash
# Request overhead only
mvn exec:java -Dexec.mainClass="com.nnegi88.errormonitor.benchmark.BenchmarkRunner" -Dexec.args="RequestOverheadBenchmark"

# Memory usage only
mvn exec:java -Dexec.mainClass="com.nnegi88.errormonitor.benchmark.BenchmarkRunner" -Dexec.args="MemoryUsageBenchmark"

# Rate limiting only
mvn exec:java -Dexec.mainClass="com.nnegi88.errormonitor.benchmark.BenchmarkRunner" -Dexec.args="RateLimitingBenchmark"

# Notification pipeline only
mvn exec:java -Dexec.mainClass="com.nnegi88.errormonitor.benchmark.BenchmarkRunner" -Dexec.args="NotificationBenchmark"
```

### Running Load Tests

```bash
# Run all load test scenarios
mvn exec:java -Dexec.mainClass="com.nnegi88.errormonitor.benchmark.scenarios.LoadTestScenarios"
```

### Using JMH Directly

```bash
# Run with custom JMH options
java -jar target/spring-boot-error-monitor-benchmark-1.0.0-SNAPSHOT.jar -wi 5 -i 10 -f 2 -t 4
```

## Benchmark Descriptions

### 1. RequestOverheadBenchmark
Measures the overhead added by error monitoring to normal request processing:
- Baseline performance (monitoring disabled)
- Performance with monitoring enabled
- Impact on error vs non-error requests
- Concurrent request handling

### 2. MemoryUsageBenchmark
Analyzes memory consumption:
- Initial footprint with different feature sets
- Memory growth under sustained load
- GC pressure and frequency
- Analytics data retention impact

### 3. RateLimitingBenchmark
Tests the efficiency of rate limiting:
- Single check performance
- Concurrent access patterns
- Burst scenario handling
- Memory usage of timestamp tracking

### 4. NotificationBenchmark
Evaluates the notification pipeline:
- Async processing overhead
- Thread pool utilization
- Backpressure handling
- Network I/O impact simulation

### 5. LoadTestScenarios
Comprehensive load testing:
- Sustained load (1 hour at target RPS)
- Spike load (sudden traffic increase)
- Error storm (high error rate)
- Concurrent users simulation

## Configuration Profiles

The benchmarks use different configuration profiles to test various feature combinations:

- `benchmark-baseline`: Error monitoring disabled
- `benchmark-basic`: Minimal features enabled
- `benchmark-metrics`: With metrics collection
- `benchmark-analytics`: With analytics enabled
- `benchmark-full`: All features enabled

## Interpreting Results

### JMH Output Format
```
Benchmark                          Mode  Cnt   Score   Error  Units
RequestOverheadBenchmark.baseline  avgt   20  145.32 ± 12.45  us/op
```

- **Mode**: Measurement mode (avgt = average time, thrpt = throughput)
- **Cnt**: Number of measurement iterations
- **Score**: The measured value
- **Error**: Standard deviation
- **Units**: Measurement units

### Performance Targets
Based on the PRD requirements:
- Request overhead: < 5ms
- Memory footprint: < 50MB
- Non-blocking async processing
- No performance degradation under load

## Results Location

Results are saved to:
```
target/benchmark-results/[timestamp]/
├── request-overhead.json
├── memory-usage.json
├── rate-limiting.json
├── notification-pipeline.json
└── benchmark-summary.txt
```

## Tips for Accurate Results

1. **System Preparation**:
   - Close unnecessary applications
   - Disable CPU frequency scaling
   - Use performance governor on Linux
   - Ensure consistent room temperature

2. **JVM Tuning**:
   ```bash
   export JAVA_OPTS="-Xms4g -Xmx4g -XX:+UseG1GC -XX:+AlwaysPreTouch"
   ```

3. **Multiple Runs**:
   - Run benchmarks multiple times
   - Discard outliers
   - Use median values for reporting

4. **Baseline Comparison**:
   - Always compare against baseline (disabled) configuration
   - Calculate percentage overhead

## Troubleshooting

### Out of Memory Errors
Increase heap size in the benchmark fork JVM args:
```java
@Fork(value = 2, jvmArgs = {"-Xms4G", "-Xmx4G"})
```

### Port Conflicts
Change the port numbers in benchmark classes if 8081-8090 are in use.

### Slow Startup
Reduce warmup iterations for faster development cycles:
```bash
-wi 2 -i 5
```

## Contributing

When adding new benchmarks:
1. Follow JMH best practices
2. Include warmup and measurement phases
3. Document what is being measured
4. Add configuration for different scenarios
5. Update this README