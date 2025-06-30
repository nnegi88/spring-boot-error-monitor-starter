package io.github.nnegi88.errormonitor.domain.port;

import io.github.nnegi88.errormonitor.domain.model.NotificationMessage;
import io.github.nnegi88.errormonitor.domain.model.NotificationResult;

import java.util.concurrent.CompletableFuture;

/**
 * Port interface for notification services.
 * Implementations handle the specifics of sending notifications to different platforms.
 */
public interface NotificationService {
    
    /**
     * Send a notification message asynchronously.
     * 
     * @param message the notification message to send
     * @return a CompletableFuture containing the result of the notification attempt
     */
    CompletableFuture<NotificationResult> sendNotification(NotificationMessage message);
    
    /**
     * Check if this service supports the given configuration.
     * 
     * @param config the notification configuration
     * @return true if this service can handle the configuration
     */
    boolean supports(NotificationConfig config);
    
    /**
     * Get the name of this notification service.
     * 
     * @return the service name (e.g., "slack", "teams")
     */
    String getServiceName();
    
    /**
     * Check if the service is properly configured and ready to send notifications.
     * 
     * @return true if the service is ready
     */
    default boolean isReady() {
        return true;
    }
    
    /**
     * Perform any necessary cleanup when the service is being shut down.
     */
    default void shutdown() {
        // Default implementation does nothing
    }
}