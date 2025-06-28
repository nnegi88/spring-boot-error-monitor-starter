package io.github.nnegi88.errormonitor.analytics;

import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ErrorAggregator {
    
    private final Map<String, AggregatedError> errorGroups = new ConcurrentHashMap<>();
    private static final int MAX_STACK_TRACE_LINES = 5;
    
    public void aggregate(ErrorEvent errorEvent) {
        String groupKey = generateGroupKey(errorEvent);
        
        errorGroups.compute(groupKey, (key, existingGroup) -> {
            if (existingGroup == null) {
                return new AggregatedError(errorEvent);
            } else {
                existingGroup.addOccurrence(errorEvent);
                return existingGroup;
            }
        });
    }
    
    public List<ErrorAnalytics.ErrorGroup> getErrorGroups() {
        return new ArrayList<>(errorGroups.values());
    }
    
    public List<ErrorAnalytics.ErrorGroup> getTopErrorGroups(int limit) {
        return errorGroups.values().stream()
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public void clear() {
        errorGroups.clear();
    }
    
    private String generateGroupKey(ErrorEvent errorEvent) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // Include exception type
        if (errorEvent.getException() != null) {
            keyBuilder.append(errorEvent.getException().getClass().getName());
        } else {
            keyBuilder.append("NO_EXCEPTION");
        }
        
        keyBuilder.append(":");
        
        // Include key stack trace elements for grouping
        if (errorEvent.getException() != null) {
            String stackTrace = extractStackTraceString(errorEvent.getException());
            if (stackTrace != null && !stackTrace.isEmpty()) {
                String stackPattern = extractStackPattern(stackTrace);
                keyBuilder.append(stackPattern);
            }
        }
        
        return keyBuilder.toString();
    }
    
    private String extractStackPattern(String stackTrace) {
        String[] lines = stackTrace.split("\n");
        StringBuilder pattern = new StringBuilder();
        
        int count = 0;
        for (String line : lines) {
            line = line.trim();
            
            // Skip framework lines
            if (isFrameworkLine(line)) {
                continue;
            }
            
            // Extract method signature
            if (line.startsWith("at ")) {
                pattern.append(extractMethodSignature(line)).append("|");
                count++;
                
                if (count >= MAX_STACK_TRACE_LINES) {
                    break;
                }
            }
        }
        
        return pattern.toString();
    }
    
    private boolean isFrameworkLine(String line) {
        return line.contains("java.lang.reflect") ||
               line.contains("org.springframework") ||
               line.contains("javax.servlet") ||
               line.contains("org.apache.catalina") ||
               line.contains("org.apache.tomcat");
    }
    
    private String extractMethodSignature(String stackLine) {
        // Extract class and method from stack trace line
        int startIndex = stackLine.indexOf("at ") + 3;
        int endIndex = stackLine.indexOf("(");
        
        if (endIndex > startIndex) {
            String fullMethod = stackLine.substring(startIndex, endIndex);
            // Keep only class name and method, remove package details
            int lastDot = fullMethod.lastIndexOf(".");
            if (lastDot > 0) {
                int secondLastDot = fullMethod.lastIndexOf(".", lastDot - 1);
                if (secondLastDot > 0) {
                    return fullMethod.substring(secondLastDot + 1);
                }
            }
            return fullMethod;
        }
        
        return stackLine;
    }
    
    private String extractStackTraceString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    static class AggregatedError implements ErrorAnalytics.ErrorGroup {
        private final String groupKey;
        private final String errorType;
        private final String pattern;
        private long count = 1;
        private final Instant firstOccurrence;
        private Instant lastOccurrence;
        private final Set<String> affectedEndpoints = new HashSet<>();
        private final Map<ErrorSeverity, Long> severityDistribution = new EnumMap<>(ErrorSeverity.class);
        
        AggregatedError(ErrorEvent errorEvent) {
            this.groupKey = generateKey(errorEvent);
            this.errorType = errorEvent.getException() != null ? 
                    errorEvent.getException().getClass().getSimpleName() : "Unknown";
            this.pattern = errorEvent.getMessage();
            this.firstOccurrence = Instant.now();
            this.lastOccurrence = firstOccurrence;
            
            addOccurrence(errorEvent);
        }
        
        synchronized void addOccurrence(ErrorEvent errorEvent) {
            count++;
            lastOccurrence = Instant.now();
            
            if (errorEvent.getRequestContext() != null && 
                errorEvent.getRequestContext().getUrl() != null) {
                affectedEndpoints.add(errorEvent.getRequestContext().getUrl());
            }
            
            ErrorSeverity severity = errorEvent.getSeverity() != null ? 
                    errorEvent.getSeverity() : ErrorSeverity.ERROR;
            severityDistribution.merge(severity, 1L, Long::sum);
        }
        
        private String generateKey(ErrorEvent errorEvent) {
            if (errorEvent.getException() != null) {
                return errorEvent.getException().getClass().getName() + ":" + 
                       (errorEvent.getMessage() != null ? errorEvent.getMessage().hashCode() : "");
            }
            return "UNKNOWN:" + (errorEvent.getMessage() != null ? errorEvent.getMessage().hashCode() : "");
        }
        
        @Override
        public String getGroupKey() {
            return groupKey;
        }
        
        @Override
        public String getErrorType() {
            return errorType;
        }
        
        @Override
        public String getPattern() {
            return pattern;
        }
        
        @Override
        public long getCount() {
            return count;
        }
        
        @Override
        public Instant getFirstOccurrence() {
            return firstOccurrence;
        }
        
        @Override
        public Instant getLastOccurrence() {
            return lastOccurrence;
        }
        
        @Override
        public List<String> getAffectedEndpoints() {
            return new ArrayList<>(affectedEndpoints);
        }
        
        @Override
        public Map<String, Long> getSeverityDistribution() {
            Map<String, Long> distribution = new HashMap<>();
            severityDistribution.forEach((severity, count) -> 
                    distribution.put(severity.name(), count));
            return distribution;
        }
    }
}