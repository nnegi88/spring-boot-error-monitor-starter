package io.github.nnegi88.errormonitor.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.github.nnegi88.errormonitor.domain.model.LogEvent;
import io.github.nnegi88.errormonitor.domain.port.AsyncProcessor;
import io.github.nnegi88.errormonitor.domain.port.NotificationConfig;
import io.github.nnegi88.errormonitor.domain.service.NotificationOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Unified Logback appender that follows SOLID principles.
 * Uses dependency injection and delegates to domain services for notification processing.
 */
public class UnifiedNotificationAppender extends AppenderBase<ILoggingEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(UnifiedNotificationAppender.class);
    
    private NotificationOrchestrator orchestrator;
    private AsyncProcessor asyncProcessor;
    private List<NotificationConfig> configurations;
    private boolean async = true;
    
    @Override
    protected void append(ILoggingEvent event) {
        if (orchestrator == null || configurations == null || configurations.isEmpty()) {
            return;
        }
        
        try {
            LogEvent logEvent = convertToLogEvent(event);
            
            if (async && asyncProcessor != null && asyncProcessor.canAcceptTasks()) {
                asyncProcessor.processAsync(() -> processEvent(logEvent))
                        .exceptionally(throwable -> {
                            logger.error("Failed to process log event asynchronously", throwable);
                            return null;
                        });
            } else {
                processEvent(logEvent);
            }
            
        } catch (Exception e) {
            logger.error("Failed to append log event for notification", e);
        }
    }
    
    private void processEvent(LogEvent logEvent) {
        orchestrator.processEvent(logEvent, configurations)
                .thenAccept(results -> {
                    long successCount = results.stream().mapToLong(result -> result.isSuccessful() ? 1 : 0).sum();
                    long failureCount = results.size() - successCount;
                    
                    if (failureCount > 0) {
                        logger.warn("Some notifications failed: {} successful, {} failed", successCount, failureCount);
                    } else if (successCount > 0) {
                        logger.debug("All notifications sent successfully: {} notifications", successCount);
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("Failed to process notification event", throwable);
                    return null;
                });
    }
    
    private LogEvent convertToLogEvent(ILoggingEvent event) {
        return LogEvent.builder()
                .level(event.getLevel().toString())
                .message(event.getMessage())
                .formattedMessage(event.getFormattedMessage())
                .loggerName(event.getLoggerName())
                .timestamp(Instant.ofEpochMilli(event.getTimeStamp()))
                .threadName(event.getThreadName())
                .throwable(event.getThrowableProxy() != null ? 
                        new RuntimeException(event.getThrowableProxy().getMessage()) : null)
                .mdcProperties(event.getMDCPropertyMap() != null ? 
                        Map.copyOf(event.getMDCPropertyMap()) : Map.of())
                .build();
    }
    
    @Override
    public void start() {
        if (orchestrator == null) {
            addError("NotificationOrchestrator is required but not set");
            return;
        }
        
        if (configurations == null || configurations.isEmpty()) {
            addWarn("No notification configurations provided");
            return;
        }
        
        logger.info("Starting UnifiedNotificationAppender with {} configurations, async: {}", 
                configurations.size(), async);
        
        super.start();
    }
    
    @Override
    public void stop() {
        logger.info("Stopping UnifiedNotificationAppender");
        
        if (asyncProcessor != null) {
            asyncProcessor.shutdown();
        }
        
        super.stop();
    }
    
    // Dependency injection setters
    public void setOrchestrator(NotificationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }
    
    public void setAsyncProcessor(AsyncProcessor asyncProcessor) {
        this.asyncProcessor = asyncProcessor;
    }
    
    public void setConfigurations(List<NotificationConfig> configurations) {
        this.configurations = configurations;
    }
    
    public void setAsync(boolean async) {
        this.async = async;
    }
    
    // Getters for configuration
    public NotificationOrchestrator getOrchestrator() {
        return orchestrator;
    }
    
    public AsyncProcessor getAsyncProcessor() {
        return asyncProcessor;
    }
    
    public List<NotificationConfig> getConfigurations() {
        return configurations;
    }
    
    public boolean isAsync() {
        return async;
    }
}