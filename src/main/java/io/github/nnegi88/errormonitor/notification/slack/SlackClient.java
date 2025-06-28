package io.github.nnegi88.errormonitor.notification.slack;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.notification.NotificationClient;
import io.github.nnegi88.errormonitor.notification.NotificationPlatform;
import io.github.nnegi88.errormonitor.notification.template.SlackMessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
@ConditionalOnProperty(prefix = "spring.error-monitor.notification", name = "platform", havingValue = "slack", matchIfMissing = false)
public class SlackClient implements NotificationClient {
    
    private static final Logger logger = LoggerFactory.getLogger(SlackClient.class);
    
    private final WebClient webClient;
    private final ErrorMonitorProperties properties;
    private final SlackMessageTemplate messageTemplate;
    
    public SlackClient(WebClient.Builder webClientBuilder, 
                      ErrorMonitorProperties properties,
                      SlackMessageTemplate messageTemplate) {
        this.properties = properties;
        this.messageTemplate = messageTemplate;
        this.webClient = webClientBuilder.build();
    }
    
    @Override
    public void sendNotification(ErrorEvent errorEvent) {
        if (!isEnabled()) {
            logger.debug("Slack notifications are disabled");
            return;
        }
        
        try {
            SlackMessage message = messageTemplate.buildMessage(errorEvent);
            
            webClient.post()
                .uri(properties.getNotification().getSlack().getWebhookUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(message), SlackMessage.class)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                    .maxBackoff(Duration.ofSeconds(10))
                    .doBeforeRetry(retrySignal -> 
                        logger.warn("Retrying Slack notification, attempt: {}", retrySignal.totalRetries() + 1)))
                .doOnSuccess(response -> logger.debug("Slack notification sent successfully"))
                .doOnError(error -> logger.error("Failed to send Slack notification", error))
                .subscribe();
                
        } catch (Exception e) {
            logger.error("Error sending Slack notification", e);
        }
    }
    
    @Override
    public boolean isEnabled() {
        return properties.isEnabled() && 
               properties.getNotification() != null &&
               properties.getNotification().getSlack() != null &&
               properties.getNotification().getSlack().getWebhookUrl() != null &&
               !properties.getNotification().getSlack().getWebhookUrl().isEmpty();
    }
    
    @Override
    public NotificationPlatform getPlatform() {
        return NotificationPlatform.SLACK;
    }
}