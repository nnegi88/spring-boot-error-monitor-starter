package io.github.nnegi88.errormonitor.management;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.metrics.ErrorMetrics;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@Endpoint(id = "errorMonitor")
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
public class ErrorMonitorEndpoint {
    
    private final ErrorMonitorProperties properties;
    private final ErrorMetrics errorMetrics;
    private volatile boolean temporarilyDisabled = false;
    private Instant startTime = Instant.now();
    
    public ErrorMonitorEndpoint(ErrorMonitorProperties properties, ErrorMetrics errorMetrics) {
        this.properties = properties;
        this.errorMetrics = errorMetrics;
    }
    
    @ReadOperation
    public Map<String, Object> status() {
        Map<String, Object> status = new HashMap<>();
        
        // Basic status
        status.put("enabled", properties.isEnabled() && !temporarilyDisabled);
        status.put("temporarilyDisabled", temporarilyDisabled);
        status.put("startTime", startTime.toString());
        status.put("uptime", calculateUptime());
        
        // Configuration summary
        Map<String, Object> config = new HashMap<>();
        config.put("applicationName", properties.getApplicationName());
        config.put("environment", properties.getEnvironment());
        
        if (properties.getNotification() != null) {
            config.put("notificationPlatform", properties.getNotification().getPlatform());
        }
        
        if (properties.getRateLimiting() != null) {
            Map<String, Object> rateLimit = new HashMap<>();
            rateLimit.put("enabled", properties.getRateLimiting().isEnabled());
            rateLimit.put("maxErrorsPerMinute", properties.getRateLimiting().getMaxErrorsPerMinute());
            rateLimit.put("burstLimit", properties.getRateLimiting().getBurstLimit());
            config.put("rateLimiting", rateLimit);
        }
        
        status.put("configuration", config);
        
        // Error statistics
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalErrors", errorMetrics.getErrorCount());
        statistics.put("errorRate", String.format("%.2f errors/minute", errorMetrics.getErrorRate()));
        
        // Error breakdown by severity
        Map<String, Long> severityBreakdown = new HashMap<>();
        severityBreakdown.put("CRITICAL", errorMetrics.getErrorCount(io.github.nnegi88.errormonitor.model.ErrorSeverity.CRITICAL));
        severityBreakdown.put("HIGH", errorMetrics.getErrorCount(io.github.nnegi88.errormonitor.model.ErrorSeverity.HIGH));
        severityBreakdown.put("MEDIUM", errorMetrics.getErrorCount(io.github.nnegi88.errormonitor.model.ErrorSeverity.MEDIUM));
        severityBreakdown.put("LOW", errorMetrics.getErrorCount(io.github.nnegi88.errormonitor.model.ErrorSeverity.LOW));
        severityBreakdown.put("ERROR", errorMetrics.getErrorCount(io.github.nnegi88.errormonitor.model.ErrorSeverity.ERROR));
        statistics.put("bySeverity", severityBreakdown);
        
        // Notification statistics
        Map<String, Object> notifications = new HashMap<>();
        if (properties.getNotification() != null) {
            String platform = properties.getNotification().getPlatform();
            if ("slack".equals(platform) || "both".equals(platform)) {
                notifications.put("slackSuccessRate", 
                    String.format("%.2f%%", errorMetrics.getNotificationSuccessRate("slack")));
            }
            if ("teams".equals(platform) || "both".equals(platform)) {
                notifications.put("teamsSuccessRate", 
                    String.format("%.2f%%", errorMetrics.getNotificationSuccessRate("teams")));
            }
        }
        statistics.put("notifications", notifications);
        
        status.put("statistics", statistics);
        
        return status;
    }
    
    @WriteOperation
    public Map<String, Object> toggle(boolean enable) {
        Map<String, Object> result = new HashMap<>();
        
        if (enable && temporarilyDisabled) {
            temporarilyDisabled = false;
            result.put("message", "Error monitoring re-enabled");
        } else if (!enable && !temporarilyDisabled) {
            temporarilyDisabled = true;
            result.put("message", "Error monitoring temporarily disabled");
        } else {
            result.put("message", "No change - already " + (enable ? "enabled" : "disabled"));
        }
        
        result.put("enabled", properties.isEnabled() && !temporarilyDisabled);
        return result;
    }
    
    @DeleteOperation
    public Map<String, Object> resetStatistics() {
        errorMetrics.reset();
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Error statistics have been reset");
        result.put("resetTime", Instant.now().toString());
        
        return result;
    }
    
    public boolean isTemporarilyDisabled() {
        return temporarilyDisabled;
    }
    
    private String calculateUptime() {
        long seconds = java.time.Duration.between(startTime, Instant.now()).getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;
        
        return String.format("%d hours, %d minutes, %d seconds", hours, minutes, seconds);
    }
}