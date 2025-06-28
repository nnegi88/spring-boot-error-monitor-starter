package io.github.nnegi88.errormonitor.core;

import io.github.nnegi88.errormonitor.analytics.ErrorAnalytics;
import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.filter.ErrorFilter;
import io.github.nnegi88.errormonitor.metrics.ErrorMetrics;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.notification.NotificationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

public class DefaultErrorProcessor implements ErrorProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultErrorProcessor.class);
    
    private final NotificationClient notificationClient;
    private final ErrorFilter errorFilter;
    private final ErrorMonitorProperties properties;
    private final ErrorMetrics errorMetrics;
    private final String applicationName;
    private final String environment;
    
    @Autowired(required = false)
    private ErrorAnalytics errorAnalytics;
    
    public DefaultErrorProcessor(NotificationClient notificationClient,
                               ErrorFilter errorFilter,
                               ErrorMonitorProperties properties,
                               ErrorMetrics errorMetrics,
                               String applicationName,
                               String environment) {
        this.notificationClient = notificationClient;
        this.errorFilter = errorFilter;
        this.properties = properties;
        this.errorMetrics = errorMetrics;
        this.applicationName = applicationName;
        this.environment = environment;
    }
    
    @Override
    @Async
    public void processError(ErrorEvent errorEvent) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Set application context
            errorEvent.setApplicationName(applicationName);
            errorEvent.setEnvironment(environment);
            
            // Check if we should process this error
            if (!shouldProcess(errorEvent)) {
                logger.debug("Error event filtered out: {}", errorEvent.getMessage());
                errorMetrics.recordRateLimited();
                return;
            }
            
            // Record error metrics
            errorMetrics.recordError(errorEvent);
            
            // Analyze error if analytics is enabled
            if (errorAnalytics != null) {
                try {
                    errorAnalytics.analyzeError(errorEvent);
                } catch (Exception e) {
                    logger.warn("Failed to analyze error event", e);
                }
            }
            
            // Send notification
            CompletableFuture.runAsync(() -> {
                try {
                    notificationClient.sendNotification(errorEvent);
                    errorMetrics.recordNotificationSuccess(determineNotificationPlatform());
                } catch (Exception e) {
                    logger.error("Failed to send notification for error event", e);
                    errorMetrics.recordNotificationFailure(determineNotificationPlatform(), e);
                }
            });
            
        } catch (Exception e) {
            logger.error("Failed to process error event", e);
        } finally {
            // Record processing time
            long processingTime = System.currentTimeMillis() - startTime;
            errorMetrics.recordProcessingTime(processingTime);
        }
    }
    
    @Override
    public boolean shouldProcess(ErrorEvent errorEvent) {
        if (!properties.isEnabled()) {
            return false;
        }
        
        return errorFilter.shouldReport(errorEvent);
    }
    
    private String determineNotificationPlatform() {
        if (properties.getNotification() != null && properties.getNotification().getPlatform() != null) {
            return properties.getNotification().getPlatform().toString();
        }
        return "unknown";
    }
}