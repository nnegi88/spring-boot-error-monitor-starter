package io.github.nnegi88.errormonitor.infrastructure.notification.slack;

import io.github.nnegi88.errormonitor.domain.model.NotificationMessage;
import io.github.nnegi88.errormonitor.domain.model.NotificationResult;
import io.github.nnegi88.errormonitor.domain.port.MessageFormatter;
import io.github.nnegi88.errormonitor.domain.port.NotificationConfig;
import io.github.nnegi88.errormonitor.domain.port.NotificationService;
import io.github.nnegi88.errormonitor.domain.port.SlackClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Slack implementation of NotificationService using WebClient.
 * Follows single responsibility principle by focusing only on Slack-specific notification logic.
 */
public class SlackNotificationService implements NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(SlackNotificationService.class);
    private static final String SERVICE_NAME = "slack";
    
    private final SlackClient slackClient;
    private final MessageFormatter<SlackMessage> messageFormatter;
    
    public SlackNotificationService(SlackClient slackClient, MessageFormatter<SlackMessage> messageFormatter) {
        this.slackClient = slackClient;
        this.messageFormatter = messageFormatter;
    }
    
    @Override
    public CompletableFuture<NotificationResult> sendNotification(NotificationMessage message) {
        try {
            // This would typically be injected, but for now we'll use a simple implementation
            NotificationConfig config = createConfigFromMessage(message);
            
            // Format the message
            SlackMessage slackMessage = messageFormatter.formatMessage(message, config);
            
            // Send via SlackClient
            return slackClient.sendMessage(slackMessage, config.getWebhookUrl())
                    .exceptionally(throwable -> {
                        String errorMsg = "Failed to send Slack notification: " + throwable.getMessage();
                        logger.error(errorMsg, throwable);
                        return NotificationResult.failure(SERVICE_NAME, errorMsg);
                    });
                    
        } catch (Exception e) {
            String errorMsg = "Failed to prepare Slack notification: " + e.getMessage();
            logger.error(errorMsg, e);
            return CompletableFuture.completedFuture(NotificationResult.failure(SERVICE_NAME, errorMsg));
        }
    }
    
    @Override
    public boolean supports(NotificationConfig config) {
        return config != null && 
               config.getWebhookUrl() != null && 
               config.getWebhookUrl().contains("hooks.slack.com") &&
               config.isEnabled();
    }
    
    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }
    
    @Override
    public boolean isReady() {
        return slackClient != null && messageFormatter != null;
    }
    
    @Override
    public void shutdown() {
        logger.debug("Shutting down SlackNotificationService");
        // WebClient shutdown is handled by Spring framework
    }
    
    // Temporary method to create config from message - in real implementation this would be injected
    private NotificationConfig createConfigFromMessage(NotificationMessage message) {
        return new NotificationConfig() {
            @Override
            public String getWebhookUrl() {
                // This should come from actual configuration
                return (String) message.getMetadata().get("webhookUrl");
            }
            
            @Override
            public String getApplicationName() {
                return message.getApplicationName();
            }
            
            @Override
            public String getEnvironment() {
                return message.getEnvironment();
            }
            
            @Override
            public String getMinimumLevel() {
                return "ERROR";
            }
            
            @Override
            public boolean isEnabled() {
                return true;
            }
            
            @Override
            public java.util.Map<String, Object> getAdditionalProperties() {
                return message.getMetadata();
            }
        };
    }
}