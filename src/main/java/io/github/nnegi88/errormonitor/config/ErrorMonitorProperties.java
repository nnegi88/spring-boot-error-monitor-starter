package io.github.nnegi88.errormonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "spring.error-monitor")
@Validated
public class ErrorMonitorProperties {
    
    private boolean enabled = true;
    
    @Valid
    private NotificationProperties notification = new NotificationProperties();
    
    @Valid
    private FilteringProperties filtering = new FilteringProperties();
    
    @Valid
    private RateLimitingProperties rateLimiting = new RateLimitingProperties();
    
    @Valid
    private ContextProperties context = new ContextProperties();
    
    @Valid
    private MetricsProperties metrics = new MetricsProperties();
    
    @Valid
    private AnalyticsProperties analytics = new AnalyticsProperties();
    
    private String applicationName;
    private String environment;
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public NotificationProperties getNotification() {
        return notification;
    }
    
    public void setNotification(NotificationProperties notification) {
        this.notification = notification;
    }
    
    public FilteringProperties getFiltering() {
        return filtering;
    }
    
    public void setFiltering(FilteringProperties filtering) {
        this.filtering = filtering;
    }
    
    public RateLimitingProperties getRateLimiting() {
        return rateLimiting;
    }
    
    public void setRateLimiting(RateLimitingProperties rateLimiting) {
        this.rateLimiting = rateLimiting;
    }
    
    public ContextProperties getContext() {
        return context;
    }
    
    public void setContext(ContextProperties context) {
        this.context = context;
    }
    
    public MetricsProperties getMetrics() {
        return metrics;
    }
    
    public void setMetrics(MetricsProperties metrics) {
        this.metrics = metrics;
    }
    
    public AnalyticsProperties getAnalytics() {
        return analytics;
    }
    
    public void setAnalytics(AnalyticsProperties analytics) {
        this.analytics = analytics;
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
    
    public static class NotificationProperties {
        private String platform = "slack";
        
        @Valid
        private SlackProperties slack = new SlackProperties();
        
        @Valid
        private TeamsProperties teams = new TeamsProperties();
        
        public String getPlatform() {
            return platform;
        }
        
        public void setPlatform(String platform) {
            this.platform = platform;
        }
        
        public SlackProperties getSlack() {
            return slack;
        }
        
        public void setSlack(SlackProperties slack) {
            this.slack = slack;
        }
        
        public TeamsProperties getTeams() {
            return teams;
        }
        
        public void setTeams(TeamsProperties teams) {
            this.teams = teams;
        }
    }
    
    public static class SlackProperties {
        private String webhookUrl;
        private String channel = "#alerts";
        private String username = "Error Monitor";
        private String iconEmoji = ":warning:";
        
        public String getWebhookUrl() {
            return webhookUrl;
        }
        
        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }
        
        public String getChannel() {
            return channel;
        }
        
        public void setChannel(String channel) {
            this.channel = channel;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getIconEmoji() {
            return iconEmoji;
        }
        
        public void setIconEmoji(String iconEmoji) {
            this.iconEmoji = iconEmoji;
        }
    }
    
    public static class TeamsProperties {
        private String webhookUrl;
        private String title = "Application Error Alert";
        private String themeColor = "FF0000";
        
        public String getWebhookUrl() {
            return webhookUrl;
        }
        
        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getThemeColor() {
            return themeColor;
        }
        
        public void setThemeColor(String themeColor) {
            this.themeColor = themeColor;
        }
    }
    
    public static class FilteringProperties {
        private List<String> enabledPackages = new ArrayList<>();
        private List<String> excludedExceptions = new ArrayList<>();
        private String minimumSeverity = "ERROR";
        
        public List<String> getEnabledPackages() {
            return enabledPackages;
        }
        
        public void setEnabledPackages(List<String> enabledPackages) {
            this.enabledPackages = enabledPackages;
        }
        
        public List<String> getExcludedExceptions() {
            return excludedExceptions;
        }
        
        public void setExcludedExceptions(List<String> excludedExceptions) {
            this.excludedExceptions = excludedExceptions;
        }
        
        public String getMinimumSeverity() {
            return minimumSeverity;
        }
        
        public void setMinimumSeverity(String minimumSeverity) {
            this.minimumSeverity = minimumSeverity;
        }
    }
    
    public static class RateLimitingProperties {
        private int maxErrorsPerMinute = 10;
        private int burstLimit = 5;
        
        public int getMaxErrorsPerMinute() {
            return maxErrorsPerMinute;
        }
        
        public void setMaxErrorsPerMinute(int maxErrorsPerMinute) {
            this.maxErrorsPerMinute = maxErrorsPerMinute;
        }
        
        public int getBurstLimit() {
            return burstLimit;
        }
        
        public void setBurstLimit(int burstLimit) {
            this.burstLimit = burstLimit;
        }
        
        public boolean isEnabled() {
            return maxErrorsPerMinute > 0;
        }
    }
    
    public static class ContextProperties {
        private boolean includeRequestDetails = true;
        private boolean includeStackTrace = true;
        private int maxStackTraceLines = 20;
        private boolean maskSensitiveData = true;
        
        public boolean isIncludeRequestDetails() {
            return includeRequestDetails;
        }
        
        public void setIncludeRequestDetails(boolean includeRequestDetails) {
            this.includeRequestDetails = includeRequestDetails;
        }
        
        public boolean isIncludeStackTrace() {
            return includeStackTrace;
        }
        
        public void setIncludeStackTrace(boolean includeStackTrace) {
            this.includeStackTrace = includeStackTrace;
        }
        
        public int getMaxStackTraceLines() {
            return maxStackTraceLines;
        }
        
        public void setMaxStackTraceLines(int maxStackTraceLines) {
            this.maxStackTraceLines = maxStackTraceLines;
        }
        
        public boolean isMaskSensitiveData() {
            return maskSensitiveData;
        }
        
        public void setMaskSensitiveData(boolean maskSensitiveData) {
            this.maskSensitiveData = maskSensitiveData;
        }
    }
    
    public static class MetricsProperties {
        private boolean enabled = true;
        private List<String> tags = new ArrayList<>();
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public List<String> getTags() {
            return tags;
        }
        
        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }
    
    public static class AnalyticsProperties {
        private boolean enabled = true;
        private String retentionPeriod = "7d";
        private boolean aggregationEnabled = true;
        private boolean trendAnalysisEnabled = true;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getRetentionPeriod() {
            return retentionPeriod;
        }
        
        public void setRetentionPeriod(String retentionPeriod) {
            this.retentionPeriod = retentionPeriod;
        }
        
        public boolean isAggregationEnabled() {
            return aggregationEnabled;
        }
        
        public void setAggregationEnabled(boolean aggregationEnabled) {
            this.aggregationEnabled = aggregationEnabled;
        }
        
        public boolean isTrendAnalysisEnabled() {
            return trendAnalysisEnabled;
        }
        
        public void setTrendAnalysisEnabled(boolean trendAnalysisEnabled) {
            this.trendAnalysisEnabled = trendAnalysisEnabled;
        }
    }
}