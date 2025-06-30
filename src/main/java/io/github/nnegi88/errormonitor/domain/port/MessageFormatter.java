package io.github.nnegi88.errormonitor.domain.port;

import io.github.nnegi88.errormonitor.domain.model.NotificationMessage;

/**
 * Port interface for formatting notification messages into service-specific formats.
 * 
 * @param <T> the type of formatted message (e.g., SlackMessage, TeamsMessage)
 */
public interface MessageFormatter<T> {
    
    /**
     * Format a notification message into the service-specific format.
     * 
     * @param message the generic notification message
     * @param config the notification configuration
     * @return the formatted message ready for sending
     */
    T formatMessage(NotificationMessage message, NotificationConfig config);
    
    /**
     * Get the type of message this formatter produces.
     * 
     * @return the message type class
     */
    Class<T> getMessageType();
    
    /**
     * Get the name of the service this formatter supports.
     * 
     * @return the service name (e.g., "slack", "teams")
     */
    String getServiceName();
}