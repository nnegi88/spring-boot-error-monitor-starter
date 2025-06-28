package io.github.nnegi88.errormonitor.management;

import io.github.nnegi88.errormonitor.analytics.ErrorAnalytics;
import io.github.nnegi88.errormonitor.metrics.ErrorMetrics;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Endpoint(id = "errorStatistics")
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
public class ErrorStatisticsEndpoint {
    
    private final ErrorMetrics errorMetrics;
    private final Map<String, ErrorTypeStatistics> errorTypeStats = new ConcurrentHashMap<>();
    private final Map<String, List<TimestampedError>> recentErrors = new ConcurrentHashMap<>();
    private static final int MAX_RECENT_ERRORS = 100;
    
    public ErrorStatisticsEndpoint(ErrorMetrics errorMetrics) {
        this.errorMetrics = errorMetrics;
    }
    
    @ReadOperation
    public Map<String, Object> getAllStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // Overall metrics
        statistics.put("summary", getSummaryStatistics());
        
        // Top error types
        statistics.put("topErrorTypes", getTopErrorTypes(10));
        
        // Error trends
        statistics.put("trends", getErrorTrends());
        
        // Recent errors
        statistics.put("recentErrors", getRecentErrors(10));
        
        // Severity distribution
        statistics.put("severityDistribution", getSeverityDistribution());
        
        // Notification performance
        statistics.put("notificationPerformance", getNotificationPerformance());
        
        return statistics;
    }
    
    @ReadOperation
    public Map<String, Object> getStatisticsByType(@Selector String errorType) {
        Map<String, Object> result = new HashMap<>();
        
        ErrorTypeStatistics stats = errorTypeStats.get(errorType);
        if (stats == null) {
            result.put("error", "No statistics found for error type: " + errorType);
            return result;
        }
        
        result.put("errorType", errorType);
        result.put("count", stats.count);
        result.put("firstOccurrence", stats.firstOccurrence.toString());
        result.put("lastOccurrence", stats.lastOccurrence.toString());
        result.put("averageOccurrencePerHour", stats.getAverageOccurrencePerHour());
        result.put("affectedEndpoints", stats.affectedEndpoints);
        result.put("severityBreakdown", stats.severityCount);
        
        return result;
    }
    
    private Map<String, Object> getSummaryStatistics() {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("totalErrors", errorMetrics.getErrorCount());
        summary.put("errorRate", String.format("%.2f errors/minute", errorMetrics.getErrorRate()));
        summary.put("uniqueErrorTypes", errorTypeStats.size());
        
        // Calculate time windows
        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        Instant oneDayAgo = now.minus(1, ChronoUnit.DAYS);
        
        long errorsLastHour = recentErrors.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.timestamp.isAfter(oneHourAgo))
                .count();
                
        long errorsLastDay = recentErrors.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.timestamp.isAfter(oneDayAgo))
                .count();
        
        summary.put("errorsLastHour", errorsLastHour);
        summary.put("errorsLast24Hours", errorsLastDay);
        
        return summary;
    }
    
    private List<Map<String, Object>> getTopErrorTypes(int limit) {
        return errorTypeStats.entrySet().stream()
                .sorted(Map.Entry.<String, ErrorTypeStatistics>comparingByValue(
                        (a, b) -> Long.compare(b.count, a.count)))
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> errorInfo = new HashMap<>();
                    errorInfo.put("errorType", entry.getKey());
                    errorInfo.put("count", entry.getValue().count);
                    errorInfo.put("percentage", 
                        String.format("%.2f%%", 
                            (double) entry.getValue().count / errorMetrics.getErrorCount() * 100));
                    errorInfo.put("lastOccurrence", entry.getValue().lastOccurrence.toString());
                    return errorInfo;
                })
                .collect(Collectors.toList());
    }
    
    private Map<String, Object> getErrorTrends() {
        Map<String, Object> trends = new HashMap<>();
        
        // Calculate hourly trend
        Instant now = Instant.now();
        Map<Integer, Long> hourlyCount = new HashMap<>();
        
        for (int i = 0; i < 24; i++) {
            Instant hourStart = now.minus(i + 1, ChronoUnit.HOURS);
            Instant hourEnd = now.minus(i, ChronoUnit.HOURS);
            
            long count = recentErrors.values().stream()
                    .flatMap(List::stream)
                    .filter(e -> e.timestamp.isAfter(hourStart) && e.timestamp.isBefore(hourEnd))
                    .count();
                    
            hourlyCount.put(23 - i, count);
        }
        
        trends.put("hourly", hourlyCount);
        
        // Detect spikes
        double average = hourlyCount.values().stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
                
        List<Integer> spikeHours = hourlyCount.entrySet().stream()
                .filter(entry -> entry.getValue() > average * 2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
                
        trends.put("spikeHours", spikeHours);
        trends.put("averagePerHour", String.format("%.2f", average));
        
        return trends;
    }
    
    private List<Map<String, Object>> getRecentErrors(int limit) {
        return recentErrors.values().stream()
                .flatMap(List::stream)
                .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
                .limit(limit)
                .map(error -> {
                    Map<String, Object> errorInfo = new HashMap<>();
                    errorInfo.put("timestamp", error.timestamp.toString());
                    errorInfo.put("errorType", error.errorType);
                    errorInfo.put("message", error.message);
                    errorInfo.put("severity", error.severity);
                    errorInfo.put("endpoint", error.endpoint);
                    return errorInfo;
                })
                .collect(Collectors.toList());
    }
    
    private Map<String, Object> getSeverityDistribution() {
        Map<String, Object> distribution = new HashMap<>();
        
        long total = errorMetrics.getErrorCount();
        if (total == 0) {
            return distribution;
        }
        
        for (ErrorSeverity severity : ErrorSeverity.values()) {
            long count = errorMetrics.getErrorCount(severity);
            distribution.put(severity.name(), Map.of(
                    "count", count,
                    "percentage", String.format("%.2f%%", (double) count / total * 100)
            ));
        }
        
        return distribution;
    }
    
    private Map<String, Object> getNotificationPerformance() {
        Map<String, Object> performance = new HashMap<>();
        
        performance.put("slack", Map.of(
                "successRate", String.format("%.2f%%", errorMetrics.getNotificationSuccessRate("slack"))
        ));
        
        performance.put("teams", Map.of(
                "successRate", String.format("%.2f%%", errorMetrics.getNotificationSuccessRate("teams"))
        ));
        
        return performance;
    }
    
    public void recordError(String errorType, String message, ErrorSeverity severity, String endpoint) {
        // Update error type statistics
        ErrorTypeStatistics stats = errorTypeStats.computeIfAbsent(errorType, 
                k -> new ErrorTypeStatistics());
        stats.recordOccurrence(severity, endpoint);
        
        // Record recent error
        String key = errorType;
        List<TimestampedError> errors = recentErrors.computeIfAbsent(key, 
                k -> Collections.synchronizedList(new ArrayList<>()));
        
        errors.add(new TimestampedError(errorType, message, severity, endpoint));
        
        // Keep only recent errors
        if (errors.size() > MAX_RECENT_ERRORS) {
            errors.remove(0);
        }
    }
    
    private static class ErrorTypeStatistics {
        long count = 0;
        Instant firstOccurrence;
        Instant lastOccurrence;
        Map<ErrorSeverity, Long> severityCount = new EnumMap<>(ErrorSeverity.class);
        Set<String> affectedEndpoints = new HashSet<>();
        
        synchronized void recordOccurrence(ErrorSeverity severity, String endpoint) {
            count++;
            lastOccurrence = Instant.now();
            if (firstOccurrence == null) {
                firstOccurrence = lastOccurrence;
            }
            
            severityCount.merge(severity, 1L, Long::sum);
            
            if (endpoint != null) {
                affectedEndpoints.add(endpoint);
            }
        }
        
        double getAverageOccurrencePerHour() {
            if (firstOccurrence == null || count == 0) {
                return 0.0;
            }
            
            long hours = ChronoUnit.HOURS.between(firstOccurrence, Instant.now());
            if (hours == 0) {
                hours = 1;
            }
            
            return (double) count / hours;
        }
    }
    
    private static class TimestampedError {
        final Instant timestamp;
        final String errorType;
        final String message;
        final ErrorSeverity severity;
        final String endpoint;
        
        TimestampedError(String errorType, String message, ErrorSeverity severity, String endpoint) {
            this.timestamp = Instant.now();
            this.errorType = errorType;
            this.message = message;
            this.severity = severity;
            this.endpoint = endpoint;
        }
    }
}