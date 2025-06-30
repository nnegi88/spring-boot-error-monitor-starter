package io.github.nnegi88.errormonitor.domain.service;

import io.github.nnegi88.errormonitor.domain.model.LogEvent;
import io.github.nnegi88.errormonitor.domain.model.NotificationMessage;
import io.github.nnegi88.errormonitor.domain.model.NotificationResult;
import io.github.nnegi88.errormonitor.domain.port.NotificationConfig;
import io.github.nnegi88.errormonitor.domain.port.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Domain service that orchestrates the notification process.
 * Coordinates between log events, configurations, and notification services.
 */
public class NotificationOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationOrchestrator.class);
    
    private final List<NotificationService> notificationServices;
    
    public NotificationOrchestrator(List<NotificationService> notificationServices) {
        this.notificationServices = notificationServices;
    }
    
    /**
     * Process a log event and send notifications to all applicable services.
     * 
     * @param logEvent the log event to process
     * @param configurations the list of notification configurations
     * @return a CompletableFuture that completes when all notifications are sent
     */
    public CompletableFuture<List<NotificationResult>> processEvent(LogEvent logEvent, List<NotificationConfig> configurations) {
        if (!shouldProcess(logEvent, configurations)) {
            return CompletableFuture.completedFuture(List.of());
        }
        
        // Convert log event to notification message
        NotificationMessage message = convertToNotificationMessage(logEvent);
        
        // Send notifications to all applicable services
        List<CompletableFuture<NotificationResult>> futures = configurations.stream()
                .filter(config -> shouldSendNotification(logEvent, config))
                .flatMap(config -> notificationServices.stream()
                        .filter(service -> service.supports(config))
                        .map(service -> sendNotification(service, message, config)))
                .collect(Collectors.toList());
        
        // Combine all futures
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
    
    private boolean shouldProcess(LogEvent logEvent, List<NotificationConfig> configurations) {
        return logEvent != null && 
               configurations != null && 
               !configurations.isEmpty() &&
               configurations.stream().anyMatch(NotificationConfig::isEnabled);
    }
    
    private boolean shouldSendNotification(LogEvent logEvent, NotificationConfig config) {
        if (!config.isEnabled()) {
            return false;
        }
        
        // Check minimum log level
        String configLevel = config.getMinimumLevel();
        if (configLevel != null) {
            LogLevel eventLevel = LogLevel.fromString(logEvent.getLevel());
            LogLevel minimumLevel = LogLevel.fromString(configLevel);
            
            if (eventLevel.ordinal() < minimumLevel.ordinal()) {
                return false;
            }
        }
        
        return true;
    }
    
    private CompletableFuture<NotificationResult> sendNotification(
            NotificationService service, NotificationMessage message, NotificationConfig config) {
        
        // Add configuration-specific metadata
        NotificationMessage enrichedMessage = enrichMessage(message, config);
        
        return service.sendNotification(enrichedMessage)
                .exceptionally(throwable -> {
                    logger.error("Failed to send notification via {}: {}", 
                            service.getServiceName(), throwable.getMessage(), throwable);
                    return NotificationResult.failure(service.getServiceName(), throwable.getMessage());
                });
    }
    
    private NotificationMessage convertToNotificationMessage(LogEvent logEvent) {
        NotificationMessage.Builder builder = NotificationMessage.builder()
                .content(logEvent.getFormattedMessage() != null ? logEvent.getFormattedMessage() : logEvent.getMessage())
                .level(logEvent.getLevel())
                .metadata(new java.util.HashMap<>(logEvent.getMdcProperties()));
        
        // Add stack trace if present
        if (logEvent.hasThrowable()) {
            String stackTrace = getStackTrace(logEvent.getThrowable());
            builder.stackTrace(stackTrace);
        }
        
        // Generate a title from the logger name
        if (logEvent.getLoggerName() != null) {
            String title = extractClassNameFromLogger(logEvent.getLoggerName());
            builder.title(title);
        }
        
        return builder.build();
    }
    
    private NotificationMessage enrichMessage(NotificationMessage message, NotificationConfig config) {
        // Merge metadata and add webhook URL
        java.util.Map<String, Object> enrichedMetadata = mergeMetadata(message.getMetadata(), config.getAdditionalProperties());
        enrichedMetadata.put("webhookUrl", config.getWebhookUrl());
        
        return NotificationMessage.builder()
                .title(message.getTitle())
                .content(message.getContent())
                .level(message.getLevel())
                .applicationName(config.getApplicationName())
                .environment(config.getEnvironment())
                .stackTrace(message.getStackTrace())
                .metadata(enrichedMetadata)
                .build();
    }
    
    private java.util.Map<String, Object> mergeMetadata(
            java.util.Map<String, Object> messageMetadata,
            java.util.Map<String, Object> configMetadata) {
        
        java.util.Map<String, Object> merged = new java.util.HashMap<>(messageMetadata);
        if (configMetadata != null) {
            merged.putAll(configMetadata);
        }
        return merged;
    }
    
    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    private String extractClassNameFromLogger(String loggerName) {
        if (loggerName == null) {
            return null;
        }
        
        int lastDot = loggerName.lastIndexOf('.');
        return lastDot >= 0 ? loggerName.substring(lastDot + 1) : loggerName;
    }
    
    /**
     * Enum representing log levels in order of severity.
     */
    private enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR;
        
        public static LogLevel fromString(String level) {
            if (level == null) {
                return ERROR;
            }
            
            try {
                return LogLevel.valueOf(level.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ERROR;
            }
        }
    }
}