package io.github.nnegi88.errormonitor.health;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component("notificationWebhooks")
@ConditionalOnClass(HealthIndicator.class)
public class NotificationHealthIndicator implements HealthIndicator {
    
    private final ErrorMonitorProperties properties;
    private final WebClient.Builder webClientBuilder;
    private final Map<String, WebhookHealth> webhookHealthMap = new ConcurrentHashMap<>();
    
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration HEALTH_CACHE_TTL = Duration.ofMinutes(1);
    
    public NotificationHealthIndicator(ErrorMonitorProperties properties, 
                                     WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClientBuilder = webClientBuilder;
    }
    
    @Override
    public Health health() {
        if (!properties.isEnabled() || properties.getNotification() == null) {
            return Health.up()
                    .withDetail("status", "notifications disabled")
                    .build();
        }
        
        Health.Builder builder = Health.up();
        Map<String, Object> details = new HashMap<>();
        boolean anyDown = false;
        
        String platform = properties.getNotification().getPlatform();
        
        // Check Slack webhook
        if (("slack".equals(platform) || "both".equals(platform)) && 
            properties.getNotification().getSlack() != null) {
            
            String slackWebhook = properties.getNotification().getSlack().getWebhookUrl();
            if (slackWebhook != null && !slackWebhook.isEmpty()) {
                WebhookHealth slackHealth = checkWebhookHealth("slack", slackWebhook);
                details.put("slack", slackHealth.toMap());
                
                if (!slackHealth.isHealthy) {
                    anyDown = true;
                }
            }
        }
        
        // Check Teams webhook
        if (("teams".equals(platform) || "both".equals(platform)) && 
            properties.getNotification().getTeams() != null) {
            
            String teamsWebhook = properties.getNotification().getTeams().getWebhookUrl();
            if (teamsWebhook != null && !teamsWebhook.isEmpty()) {
                WebhookHealth teamsHealth = checkWebhookHealth("teams", teamsWebhook);
                details.put("teams", teamsHealth.toMap());
                
                if (!teamsHealth.isHealthy) {
                    anyDown = true;
                }
            }
        }
        
        if (anyDown) {
            builder = Health.down()
                    .withDetail("reason", "One or more notification webhooks are not reachable");
        }
        
        return builder.withDetails(details).build();
    }
    
    private WebhookHealth checkWebhookHealth(String platform, String webhookUrl) {
        String cacheKey = platform + ":" + webhookUrl;
        WebhookHealth cachedHealth = webhookHealthMap.get(cacheKey);
        
        // Return cached health if still valid
        if (cachedHealth != null && !cachedHealth.isExpired()) {
            return cachedHealth;
        }
        
        // Perform health check
        WebhookHealth health = new WebhookHealth(platform);
        
        try {
            // For webhook health checks, we can't actually POST to them without sending data
            // Instead, we'll check if the URL is valid and reachable
            boolean isReachable = isUrlReachable(webhookUrl);
            
            health.isHealthy = isReachable;
            health.status = isReachable ? "UP" : "DOWN";
            health.lastCheckTime = Instant.now();
            
            if (!isReachable) {
                health.errorMessage = "Webhook URL not reachable";
            }
            
        } catch (Exception e) {
            health.isHealthy = false;
            health.status = "DOWN";
            health.errorMessage = e.getMessage();
            health.lastCheckTime = Instant.now();
        }
        
        // Update last notification success time if we have it
        health.lastSuccessfulNotification = getLastSuccessfulNotificationTime(platform);
        
        // Cache the result
        webhookHealthMap.put(cacheKey, health);
        
        return health;
    }
    
    private boolean isUrlReachable(String webhookUrl) {
        try {
            // Extract host from webhook URL for basic connectivity check
            java.net.URL url = new java.net.URL(webhookUrl);
            String host = url.getHost();
            int port = url.getPort() == -1 ? (url.getProtocol().equals("https") ? 443 : 80) : url.getPort();
            
            // Simple socket connection test
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), 
                             (int) HEALTH_CHECK_TIMEOUT.toMillis());
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    public void recordSuccessfulNotification(String platform) {
        String key = platform + ":lastSuccess";
        webhookHealthMap.put(key, new WebhookHealth(platform) {{
            this.lastSuccessfulNotification = Instant.now();
        }});
    }
    
    private Instant getLastSuccessfulNotificationTime(String platform) {
        String key = platform + ":lastSuccess";
        WebhookHealth successRecord = webhookHealthMap.get(key);
        return successRecord != null ? successRecord.lastSuccessfulNotification : null;
    }
    
    private static class WebhookHealth {
        final String platform;
        boolean isHealthy = false;
        String status = "UNKNOWN";
        String errorMessage;
        Instant lastCheckTime;
        Instant lastSuccessfulNotification;
        
        WebhookHealth(String platform) {
            this.platform = platform;
        }
        
        boolean isExpired() {
            return lastCheckTime == null || 
                   Duration.between(lastCheckTime, Instant.now()).compareTo(HEALTH_CACHE_TTL) > 0;
        }
        
        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("status", status);
            map.put("healthy", isHealthy);
            
            if (errorMessage != null) {
                map.put("error", errorMessage);
            }
            
            if (lastCheckTime != null) {
                map.put("lastCheckTime", lastCheckTime.toString());
            }
            
            if (lastSuccessfulNotification != null) {
                Duration timeSinceSuccess = Duration.between(lastSuccessfulNotification, Instant.now());
                map.put("lastSuccessfulNotification", lastSuccessfulNotification.toString());
                map.put("timeSinceLastSuccess", formatDuration(timeSinceSuccess));
            }
            
            return map;
        }
        
        private String formatDuration(Duration duration) {
            long minutes = duration.toMinutes();
            if (minutes < 1) {
                return duration.getSeconds() + " seconds ago";
            } else if (minutes < 60) {
                return minutes + " minutes ago";
            } else {
                long hours = duration.toHours();
                return hours + " hours ago";
            }
        }
    }
}