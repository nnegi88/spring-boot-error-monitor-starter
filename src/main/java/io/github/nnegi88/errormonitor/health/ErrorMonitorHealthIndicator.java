package io.github.nnegi88.errormonitor.health;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.metrics.ErrorMetrics;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component("errorMonitor")
@ConditionalOnClass(HealthIndicator.class)
public class ErrorMonitorHealthIndicator implements HealthIndicator {
    
    private final ErrorMonitorProperties properties;
    private final ErrorMetrics errorMetrics;
    private final AtomicReference<Instant> lastProcessedError = new AtomicReference<>(Instant.now());
    private final AtomicReference<Health.Builder> lastHealthStatus = new AtomicReference<>(Health.up());
    
    public ErrorMonitorHealthIndicator(ErrorMonitorProperties properties, ErrorMetrics errorMetrics) {
        this.properties = properties;
        this.errorMetrics = errorMetrics;
    }
    
    @Override
    public Health health() {
        try {
            Health.Builder builder = Health.up();
            Map<String, Object> details = new HashMap<>();
            
            // Check if error monitor is enabled
            boolean isEnabled = properties.isEnabled();
            details.put("enabled", isEnabled);
            
            if (!isEnabled) {
                details.put("reason", "Error monitoring is disabled");
                return Health.down().withDetails(details).build();
            }
            
            // Add configuration details
            details.put("applicationName", properties.getApplicationName());
            details.put("environment", properties.getEnvironment());
            
            // Add metrics information
            long totalErrors = errorMetrics.getErrorCount();
            details.put("totalErrors", totalErrors);
            
            // Format error rate to match test expectations
            double errorRate = errorMetrics.getErrorRate();
            String formattedRate = formatErrorRate(errorRate);
            details.put("errorRate", formattedRate + " errors/minute");
            
            // Add critical errors if any
            if (totalErrors > 0) {
                long criticalErrors = errorMetrics.getErrorCount(io.github.nnegi88.errormonitor.model.ErrorSeverity.CRITICAL);
                if (criticalErrors > 0) {
                    details.put("criticalErrors", criticalErrors);
                }
            }
            
            // Add notification configuration
            if (properties.getNotification() != null) {
                Map<String, Object> notificationDetails = new HashMap<>();
                String platform = properties.getNotification().getPlatform();
                notificationDetails.put("platform", platform);
                
                // Handle different platform configurations
                if ("slack".equals(platform) || "both".equals(platform)) {
                    double slackSuccessRate = errorMetrics.getNotificationSuccessRate("slack");
                    notificationDetails.put("slackSuccessRate", formatSuccessRate(slackSuccessRate));
                }
                
                if ("teams".equals(platform) || "both".equals(platform)) {
                    double teamsSuccessRate = errorMetrics.getNotificationSuccessRate("teams");
                    notificationDetails.put("teamsSuccessRate", formatSuccessRate(teamsSuccessRate));
                }
                
                details.put("notification", notificationDetails);
            }
            
            // Add rate limiting details
            if (properties.getRateLimiting() != null && properties.getRateLimiting().isEnabled()) {
                Map<String, Object> rateLimitDetails = new HashMap<>();
                rateLimitDetails.put("enabled", true);
                rateLimitDetails.put("maxErrorsPerMinute", properties.getRateLimiting().getMaxErrorsPerMinute());
                rateLimitDetails.put("burstLimit", properties.getRateLimiting().getBurstLimit());
                details.put("rateLimiting", rateLimitDetails);
            }
            
            // Check for high error rate
            if (errorRate > 100) {
                builder = Health.down()
                        .withDetail("reason", "High error rate detected");
            } else if (errorRate > 50) {
                builder = Health.status("WARNING")
                        .withDetail("reason", "Elevated error rate");
            }
            
            // Add time since last error
            Duration timeSinceLastError = Duration.between(lastProcessedError.get(), Instant.now());
            details.put("timeSinceLastError", formatDuration(timeSinceLastError));
            
            builder.withDetails(details);
            lastHealthStatus.set(builder);
            
            return builder.build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getClass().getName() + ": " + e.getMessage())
                    .build();
        }
    }
    
    private String formatErrorRate(double rate) {
        // Format to remove trailing zeros
        if (rate == (long) rate) {
            return String.format("%.1f", rate);
        } else {
            String formatted = String.format("%.1f", rate);
            // If still ends with .0, keep it as per test expectations
            return formatted;
        }
    }
    
    private String formatSuccessRate(double rate) {
        // Format to match test expectations
        if (rate == (long) rate) {
            return String.format("%.1f%%", rate);
        } else {
            return String.format("%.1f%%", rate);
        }
    }
    
    public void recordErrorProcessed() {
        lastProcessedError.set(Instant.now());
    }
    
    private String formatDuration(Duration duration) {
        long minutes = duration.toMinutes();
        if (minutes < 1) {
            return duration.getSeconds() + " seconds";
        } else if (minutes < 60) {
            return minutes + " minutes";
        } else {
            long hours = duration.toHours();
            return hours + " hours";
        }
    }
}