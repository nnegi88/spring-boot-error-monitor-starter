package io.github.nnegi88.errormonitor.infrastructure.config;

import io.github.nnegi88.errormonitor.domain.port.NotificationConfig;

import java.util.Map;

/**
 * Slack-specific implementation of NotificationConfig.
 * Follows interface segregation principle by only implementing required methods.
 */
public class SlackConfig implements NotificationConfig {
    
    private final String webhookUrl;
    private final String applicationName;
    private final String environment;
    private final String minimumLevel;
    private final boolean enabled;
    private final Map<String, Object> additionalProperties;
    
    public SlackConfig(String webhookUrl, String applicationName, String environment, 
                      String minimumLevel, boolean enabled, Map<String, Object> additionalProperties) {
        this.webhookUrl = webhookUrl;
        this.applicationName = applicationName;
        this.environment = environment;
        this.minimumLevel = minimumLevel;
        this.enabled = enabled;
        this.additionalProperties = additionalProperties != null ? 
                Map.copyOf(additionalProperties) : Map.of();
    }
    
    @Override
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    @Override
    public String getApplicationName() {
        return applicationName;
    }
    
    @Override
    public String getEnvironment() {
        return environment;
    }
    
    @Override
    public String getMinimumLevel() {
        return minimumLevel;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String webhookUrl;
        private String applicationName;
        private String environment;
        private String minimumLevel = "ERROR";
        private boolean enabled = true;
        private Map<String, Object> additionalProperties = Map.of();
        
        public Builder webhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
            return this;
        }
        
        public Builder applicationName(String applicationName) {
            this.applicationName = applicationName;
            return this;
        }
        
        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }
        
        public Builder minimumLevel(String minimumLevel) {
            this.minimumLevel = minimumLevel;
            return this;
        }
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public Builder additionalProperties(Map<String, Object> additionalProperties) {
            this.additionalProperties = additionalProperties;
            return this;
        }
        
        public SlackConfig build() {
            return new SlackConfig(webhookUrl, applicationName, environment, 
                                 minimumLevel, enabled, additionalProperties);
        }
    }
}