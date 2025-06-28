package io.github.nnegi88.errormonitor.notification.teams;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.notification.NotificationClient;
import io.github.nnegi88.errormonitor.notification.NotificationPlatform;
import io.github.nnegi88.errormonitor.notification.template.TeamsMessageTemplate;
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
@ConditionalOnProperty(prefix = "spring.error-monitor.notification", name = "platform", havingValue = "teams", matchIfMissing = false)
public class TeamsClient implements NotificationClient {
    
    private static final Logger logger = LoggerFactory.getLogger(TeamsClient.class);
    
    private final WebClient webClient;
    private final ErrorMonitorProperties properties;
    private final TeamsMessageTemplate messageTemplate;
    
    public TeamsClient(WebClient.Builder webClientBuilder, 
                      ErrorMonitorProperties properties,
                      TeamsMessageTemplate messageTemplate) {
        this.properties = properties;
        this.messageTemplate = messageTemplate;
        this.webClient = webClientBuilder.build();
    }
    
    @Override
    public void sendNotification(ErrorEvent errorEvent) {
        if (!isEnabled()) {
            logger.debug("Teams notifications are disabled");
            return;
        }
        
        try {
            TeamsMessage message = messageTemplate.buildMessage(errorEvent);
            
            webClient.post()
                .uri(properties.getNotification().getTeams().getWebhookUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(message), TeamsMessage.class)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                    .maxBackoff(Duration.ofSeconds(10))
                    .doBeforeRetry(retrySignal -> 
                        logger.warn("Retrying Teams notification, attempt: {}", retrySignal.totalRetries() + 1)))
                .doOnSuccess(response -> logger.debug("Teams notification sent successfully"))
                .doOnError(error -> logger.error("Failed to send Teams notification", error))
                .subscribe();
                
        } catch (Exception e) {
            logger.error("Error sending Teams notification", e);
        }
    }
    
    @Override
    public boolean isEnabled() {
        return properties.isEnabled() && 
               properties.getNotification() != null &&
               properties.getNotification().getTeams() != null &&
               properties.getNotification().getTeams().getWebhookUrl() != null &&
               !properties.getNotification().getTeams().getWebhookUrl().isEmpty();
    }
    
    @Override
    public NotificationPlatform getPlatform() {
        return NotificationPlatform.TEAMS;
    }
}