package io.github.nnegi88.errormonitor.metrics;

import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnClass(MeterRegistry.class)
public class MicrometerErrorMetrics implements ErrorMetrics {
    
    private static final String METRIC_PREFIX = "error.monitor";
    
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> errorCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> notificationCounters = new ConcurrentHashMap<>();
    private final Timer processingTimer;
    private final Counter rateLimitCounter;
    private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());
    
    public MicrometerErrorMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.processingTimer = Timer.builder(METRIC_PREFIX + ".processing.time")
                .description("Error processing time")
                .register(meterRegistry);
                
        this.rateLimitCounter = Counter.builder(METRIC_PREFIX + ".rate.limited")
                .description("Number of errors that were rate limited")
                .register(meterRegistry);
    }
    
    @Override
    public void recordError(ErrorEvent errorEvent) {
        String severityTag = errorEvent.getSeverity() != null ? 
                errorEvent.getSeverity().name() : ErrorSeverity.ERROR.name();
        String exceptionType = errorEvent.getException() != null ?
                errorEvent.getException().getClass().getSimpleName() : "Unknown";
        String application = errorEvent.getApplicationName() != null ?
                errorEvent.getApplicationName() : "unknown";
                
        Tags baseTags = Tags.of(
                "severity", severityTag,
                "exception.type", exceptionType,
                "application", application
        );
        
        final Tags finalTags = errorEvent.getEnvironment() != null ? 
                baseTags.and("environment", errorEvent.getEnvironment()) : baseTags;
        
        String key = finalTags.toString();
        errorCounters.computeIfAbsent(key, k -> 
                Counter.builder(METRIC_PREFIX + ".errors.total")
                        .description("Total number of errors")
                        .tags(finalTags)
                        .register(meterRegistry)
        ).increment();
    }
    
    @Override
    public void recordNotificationSuccess(String platform) {
        getNotificationCounter(platform, "success").increment();
    }
    
    @Override
    public void recordNotificationFailure(String platform, Throwable error) {
        String errorType = error != null ? error.getClass().getSimpleName() : "Unknown";
        Tags tags = Tags.of(
                "platform", platform,
                "status", "failure",
                "error.type", errorType
        );
        
        String key = tags.toString();
        notificationCounters.computeIfAbsent(key, k ->
                Counter.builder(METRIC_PREFIX + ".notifications")
                        .description("Notification attempts")
                        .tags(tags)
                        .register(meterRegistry)
        ).increment();
    }
    
    @Override
    public void recordProcessingTime(long durationMillis) {
        processingTimer.record(durationMillis, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public void recordRateLimited() {
        rateLimitCounter.increment();
    }
    
    @Override
    public long getErrorCount() {
        return errorCounters.values().stream()
                .mapToLong(counter -> (long) counter.count())
                .sum();
    }
    
    @Override
    public long getErrorCount(ErrorSeverity severity) {
        return errorCounters.entrySet().stream()
                .filter(entry -> entry.getKey().contains("severity=" + severity.name()))
                .mapToLong(entry -> (long) entry.getValue().count())
                .sum();
    }
    
    @Override
    public long getErrorCount(String exceptionType) {
        return errorCounters.entrySet().stream()
                .filter(entry -> entry.getKey().contains("exception.type=" + exceptionType))
                .mapToLong(entry -> (long) entry.getValue().count())
                .sum();
    }
    
    @Override
    public double getErrorRate() {
        long timeSinceReset = System.currentTimeMillis() - lastResetTime.get();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeSinceReset);
        if (minutes == 0) {
            minutes = 1;
        }
        return (double) getErrorCount() / minutes;
    }
    
    @Override
    public double getNotificationSuccessRate(String platform) {
        long successCount = getNotificationCount(platform, "success");
        long failureCount = getNotificationCount(platform, "failure");
        long total = successCount + failureCount;
        
        if (total == 0) {
            return 100.0;
        }
        
        return (double) successCount / total * 100;
    }
    
    @Override
    public void reset() {
        lastResetTime.set(System.currentTimeMillis());
    }
    
    private Counter getNotificationCounter(String platform, String status) {
        Tags tags = Tags.of(
                "platform", platform,
                "status", status
        );
        
        String key = tags.toString();
        return notificationCounters.computeIfAbsent(key, k ->
                Counter.builder(METRIC_PREFIX + ".notifications")
                        .description("Notification attempts")
                        .tags(tags)
                        .register(meterRegistry)
        );
    }
    
    private long getNotificationCount(String platform, String status) {
        return notificationCounters.entrySet().stream()
                .filter(entry -> entry.getKey().contains("platform=" + platform) && 
                               entry.getKey().contains("status=" + status))
                .mapToLong(entry -> (long) entry.getValue().count())
                .sum();
    }
}