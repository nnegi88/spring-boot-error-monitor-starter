package io.github.nnegi88.errormonitor.domain.port;

import io.github.nnegi88.errormonitor.domain.model.NotificationResult;
import io.github.nnegi88.errormonitor.infrastructure.notification.teams.TeamsMessage;
import java.util.concurrent.CompletableFuture;

/**
 * Domain port for Microsoft Teams notification operations.
 * Provides focused interface for Teams-specific communication.
 */
public interface TeamsClient {
    
    /**
     * Send a Teams message to the specified webhook URL.
     * 
     * @param message the Teams message to send
     * @param webhookUrl the Teams webhook URL
     * @return a CompletableFuture containing the notification result
     */
    CompletableFuture<NotificationResult> sendMessage(TeamsMessage message, String webhookUrl);
    
    /**
     * Test connectivity to a Teams webhook URL.
     * 
     * @param webhookUrl the Teams webhook URL to test
     * @return a CompletableFuture containing true if the webhook is reachable
     */
    default CompletableFuture<Boolean> testConnection(String webhookUrl) {
        // Default implementation - can be overridden for health checks
        return CompletableFuture.completedFuture(true);
    }
}