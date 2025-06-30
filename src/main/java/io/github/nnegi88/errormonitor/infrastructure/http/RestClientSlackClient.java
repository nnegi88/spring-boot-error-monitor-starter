package io.github.nnegi88.errormonitor.infrastructure.http;

import io.github.nnegi88.errormonitor.domain.model.NotificationResult;
import io.github.nnegi88.errormonitor.domain.port.SlackClient;
import io.github.nnegi88.errormonitor.infrastructure.notification.slack.SlackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import java.util.concurrent.CompletableFuture;

/**
 * RestTemplate-based implementation of SlackClient.
 * Provides a synchronous HTTP client for environments without WebFlux.
 */
public class RestClientSlackClient implements SlackClient {
    
    private static final Logger logger = LoggerFactory.getLogger(RestClientSlackClient.class);
    private static final String SERVICE_NAME = "slack";
    
    private final RestTemplate restTemplate;
    
    public RestClientSlackClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Override
    public CompletableFuture<NotificationResult> sendMessage(SlackMessage message, String webhookUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Sending Slack notification to: {}", maskWebhookUrl(webhookUrl));
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                HttpEntity<SlackMessage> request = new HttpEntity<>(message, headers);
                
                ResponseEntity<String> response = restTemplate.postForEntity(
                        webhookUrl, 
                        request, 
                        String.class
                );
                
                int statusCode = response.getStatusCode().value();
                logger.debug("Slack notification sent successfully. Status: {}", statusCode);
                return NotificationResult.success(SERVICE_NAME, statusCode);
                
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                int statusCode = e.getStatusCode().value();
                String errorMsg = String.format("HTTP %d: %s", statusCode, e.getResponseBodyAsString());
                logger.error("Failed to send Slack notification: {}", errorMsg);
                return NotificationResult.failure(SERVICE_NAME, errorMsg, statusCode);
            } catch (Exception e) {
                String errorMsg = "Failed to send Slack notification: " + e.getMessage();
                logger.error(errorMsg, e);
                return NotificationResult.failure(SERVICE_NAME, errorMsg);
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> testConnection(String webhookUrl) {
        return sendMessage(
                SlackMessage.builder()
                        .text("Health check from Spring Boot Logback Alerting Starter")
                        .build(),
                webhookUrl
        )
        .thenApply(NotificationResult::isSuccessful)
        .exceptionally(throwable -> false);
    }
    
    private String maskWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.length() < 20) {
            return "***";
        }
        try {
            java.net.URI uri = java.net.URI.create(webhookUrl);
            return uri.getHost() + "/***";
        } catch (Exception e) {
            return "***";
        }
    }
}