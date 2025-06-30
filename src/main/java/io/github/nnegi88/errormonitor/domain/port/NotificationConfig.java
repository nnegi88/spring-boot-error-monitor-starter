package io.github.nnegi88.errormonitor.domain.port;

import java.util.Map;

/**
 * Port interface for notification configuration.
 * Segregated interface focusing only on core notification settings.
 */
public interface NotificationConfig {
    
    /**
     * Get the webhook URL for the notification service.
     * 
     * @return the webhook URL
     */
    String getWebhookUrl();
    
    /**
     * Get the application name to include in notifications.
     * 
     * @return the application name
     */
    String getApplicationName();
    
    /**
     * Get the environment name to include in notifications.
     * 
     * @return the environment name
     */
    String getEnvironment();
    
    /**
     * Get the minimum log level that should trigger notifications.
     * 
     * @return the minimum log level (e.g., "ERROR", "WARN")
     */
    String getMinimumLevel();
    
    /**
     * Check if this notification configuration is enabled.
     * 
     * @return true if notifications are enabled
     */
    boolean isEnabled();
    
    /**
     * Get additional service-specific properties.
     * 
     * @return a map of additional properties
     */
    Map<String, Object> getAdditionalProperties();
}