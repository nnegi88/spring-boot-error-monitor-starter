package io.github.nnegi88.errormonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for standalone Logback Teams appender.
 * These properties are independent of the ErrorMonitor framework.
 */
@ConfigurationProperties(prefix = "logback.teams")
public class LogbackTeamsProperties {
    
    /**
     * Enable Logback Teams appender
     */
    private boolean enabled = false;
    
    /**
     * Teams webhook URL
     */
    private String webhookUrl;
    
    /**
     * Application name to display in Teams messages
     */
    private String applicationName = "${spring.application.name:Unknown}";
    
    /**
     * Environment name to display in Teams messages
     */
    private String environment = "${spring.profiles.active:default}";
    
    /**
     * Minimum log level to send to Teams (ERROR, WARN, INFO)
     */
    private String minimumLevel = "ERROR";
    
    /**
     * Include stack traces in Teams messages
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
     * Theme color for Teams cards (hex format without #)
     */
    private String themeColor = "FF0000";
    
    /**
     * Enable rate limiting to prevent flooding Teams
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
    
    public String getThemeColor() {
        return themeColor;
    }
    
    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
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