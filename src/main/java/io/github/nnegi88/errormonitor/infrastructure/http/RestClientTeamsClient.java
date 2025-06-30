package io.github.nnegi88.errormonitor.infrastructure.http;

import io.github.nnegi88.errormonitor.domain.model.NotificationResult;
import io.github.nnegi88.errormonitor.domain.port.TeamsClient;
import io.github.nnegi88.errormonitor.infrastructure.notification.teams.TeamsMessage;
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

import java.util.List;

/**
 * RestTemplate-based implementation of TeamsClient.
 * Provides a synchronous HTTP client for environments without WebFlux.
 */
public class RestClientTeamsClient implements TeamsClient {
    
    private static final Logger logger = LoggerFactory.getLogger(RestClientTeamsClient.class);
    private static final String SERVICE_NAME = "teams";
    
    private final RestTemplate restTemplate;
    
    public RestClientTeamsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Override
    public CompletableFuture<NotificationResult> sendMessage(TeamsMessage message, String webhookUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Sending Teams notification to: {}", maskWebhookUrl(webhookUrl));
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                HttpEntity<TeamsMessage> request = new HttpEntity<>(message, headers);
                
                ResponseEntity<String> response = restTemplate.postForEntity(
                        webhookUrl, 
                        request, 
                        String.class
                );
                
                int statusCode = response.getStatusCode().value();
                logger.debug("Teams notification sent successfully. Status: {}", statusCode);
                return NotificationResult.success(SERVICE_NAME, statusCode);
                
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                int statusCode = e.getStatusCode().value();
                String errorMsg = String.format("HTTP %d: %s", statusCode, e.getResponseBodyAsString());
                logger.error("Failed to send Teams notification: {}", errorMsg);
                return NotificationResult.failure(SERVICE_NAME, errorMsg, statusCode);
            } catch (Exception e) {
                String errorMsg = "Failed to send Teams notification: " + e.getMessage();
                logger.error(errorMsg, e);
                return NotificationResult.failure(SERVICE_NAME, errorMsg);
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> testConnection(String webhookUrl) {
        return sendMessage(
                TeamsMessage.builder()
                        .summary("Health check from Spring Boot Logback Alerting Starter")
                        .title("Health Check")
                        .text("Testing Teams webhook connectivity")
                        .themeColor("00FF00")
                        .sections(List.of())
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