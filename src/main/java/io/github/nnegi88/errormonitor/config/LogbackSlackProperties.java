package io.github.nnegi88.errormonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for standalone Logback Slack appender.
 * These properties are independent of the ErrorMonitor framework.
 */
@ConfigurationProperties(prefix = "logback.slack")
public class LogbackSlackProperties {
    
    /**
     * Enable Logback Slack appender
     */
    private boolean enabled = false;
    
    /**
     * Slack webhook URL
     */
    private String webhookUrl;
    
    /**
     * Application name to display in Slack messages
     */
    private String applicationName = "${spring.application.name:Unknown}";
    
    /**
     * Environment name to display in Slack messages
     */
    private String environment = "${spring.profiles.active:default}";
    
    /**
     * Minimum log level to send to Slack (ERROR, WARN, INFO)
     */
    private String minimumLevel = "ERROR";
    
    /**
     * Include stack traces in Slack messages
     */
    private boolean includeStackTrace = true;
    
    /**
     * Connection timeout in milliseconds
     */
    private int connectionTimeout = 5000;
    
    /**
     * Read timeout in milliseconds
     */
    private int readTimeout = 5000;
    
    /**
     * Enable async processing
     */
    private boolean async = true;
    
    /**
     * Queue size for async processing
     */
    private int queueSize = 256;
    
    /**
     * Enable rate limiting to prevent flooding Slack
     */
    private boolean rateLimitEnabled = true;
    
    /**
     * Maximum messages per minute
     */
    private int maxMessagesPerMinute = 10;
    
    // Getters and setters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
    
    public String getApplicationName() {
        return applicationName;
    }
    
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    public String getMinimumLevel() {
        return minimumLevel;
    }
    
    public void setMinimumLevel(String minimumLevel) {
        this.minimumLevel = minimumLevel;
    }
    
    public boolean isIncludeStackTrace() {
        return includeStackTrace;
    }
    
    public void setIncludeStackTrace(boolean includeStackTrace) {
        this.includeStackTrace = includeStackTrace;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public int getReadTimeout() {
        return readTimeout;
    }
    
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
    
    public boolean isAsync() {
        return async;
    }
    
    public void setAsync(boolean async) {
        this.async = async;
    }
    
    public int getQueueSize() {
        return queueSize;
    }
    
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }
    
    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }
    
    public void setRateLimitEnabled(boolean rateLimitEnabled) {
        this.rateLimitEnabled = rateLimitEnabled;
    }
    
    public int getMaxMessagesPerMinute() {
        return maxMessagesPerMinute;
    }
    
    public void setMaxMessagesPerMinute(int maxMessagesPerMinute) {
        this.maxMessagesPerMinute = maxMessagesPerMinute;
    }
}