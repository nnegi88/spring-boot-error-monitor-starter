package io.github.nnegi88.errormonitor.domain.port;

import io.github.nnegi88.errormonitor.domain.model.NotificationResult;
import io.github.nnegi88.errormonitor.infrastructure.notification.slack.SlackMessage;
import java.util.concurrent.CompletableFuture;

/**
 * Domain port for Slack notification operations.
 * Provides focused interface for Slack-specific communication.
 */
public interface SlackClient {
    
    /**
     * Send a Slack message to the specified webhook URL.
     * 
     * @param message the Slack message to send
     * @param webhookUrl the Slack webhook URL
     * @return a CompletableFuture containing the notification result
     */
    CompletableFuture<NotificationResult> sendMessage(SlackMessage message, String webhookUrl);
    
    /**
     * Test connectivity to a Slack webhook URL.
     * 
     * @param webhookUrl the Slack webhook URL to test
     * @return a CompletableFuture containing true if the webhook is reachable
     */
    default CompletableFuture<Boolean> testConnection(String webhookUrl) {
        // Default implementation - can be overridden for health checks
        return CompletableFuture.completedFuture(true);
    }
}