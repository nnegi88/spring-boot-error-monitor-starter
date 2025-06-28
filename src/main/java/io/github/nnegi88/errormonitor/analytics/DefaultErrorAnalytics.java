package io.github.nnegi88.errormonitor.analytics;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.management.ErrorStatisticsEndpoint;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "spring.error-monitor.analytics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DefaultErrorAnalytics implements ErrorAnalytics {
    
    private final ErrorAggregator errorAggregator;
    private final ErrorTrendAnalyzer trendAnalyzer;
    private final ErrorStatisticsEndpoint statisticsEndpoint;
    private final ErrorMonitorProperties properties;
    
    public DefaultErrorAnalytics(ErrorAggregator errorAggregator,
                               ErrorTrendAnalyzer trendAnalyzer,
                               ErrorStatisticsEndpoint statisticsEndpoint,
                               ErrorMonitorProperties properties) {
        this.errorAggregator = errorAggregator;
        this.trendAnalyzer = trendAnalyzer;
        this.statisticsEndpoint = statisticsEndpoint;
        this.properties = properties;
    }
    
    @Override
    public void analyzeError(ErrorEvent errorEvent) {
        // Aggregate the error
        errorAggregator.aggregate(errorEvent);
        
        // Record for trend analysis
        trendAnalyzer.recordError(errorEvent);
        
        // Update statistics
        String errorType = errorEvent.getException() != null ? 
                errorEvent.getException().getClass().getSimpleName() : "Unknown";
        String endpoint = errorEvent.getRequestContext() != null ? 
                errorEvent.getRequestContext().getUrl() : null;
                
        statisticsEndpoint.recordError(
                errorType,
                errorEvent.getMessage(),
                errorEvent.getSeverity(),
                endpoint
        );
    }
    
    @Override
    public List<ErrorGroup> getErrorGroups() {
        return errorAggregator.getErrorGroups();
    }
    
    @Override
    public List<ErrorGroup> getTopErrorGroups(int limit) {
        return errorAggregator.getTopErrorGroups(limit);
    }
    
    @Override
    public ErrorTrend getErrorTrend(Instant startTime, Instant endTime) {
        return trendAnalyzer.analyzeTrend(startTime, endTime);
    }
    
    @Override
    public Map<String, Object> getAnalyticsSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // Error groups summary
        List<ErrorGroup> topGroups = getTopErrorGroups(5);
        summary.put("topErrorGroups", topGroups);
        summary.put("totalErrorGroups", getErrorGroups().size());
        
        // Trend analysis for last hour
        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        ErrorTrend hourlyTrend = getErrorTrend(oneHourAgo, now);
        
        Map<String, Object> trendInfo = new HashMap<>();
        trendInfo.put("isSpike", hourlyTrend.isSpike());
        trendInfo.put("currentRate", String.format("%.2f errors/minute", hourlyTrend.getErrorRate()));
        trendInfo.put("averageRate", String.format("%.2f errors/minute", hourlyTrend.getAverageRate()));
        trendInfo.put("percentageChange", String.format("%.2f%%", hourlyTrend.getPercentageChange()));
        summary.put("hourlyTrend", trendInfo);
        
        // Spike detection
        List<ErrorTrendAnalyzer.SpikeAlert> spikes = trendAnalyzer.detectSpikes();
        summary.put("activeSpikes", spikes);
        
        // Statistics from endpoint
        Map<String, Object> statistics = statisticsEndpoint.getAllStatistics();
        summary.put("statistics", statistics);
        
        // Analytics configuration
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", isAnalyticsEnabled());
        if (properties.getAnalytics() != null) {
            config.put("retentionPeriod", properties.getAnalytics().getRetentionPeriod());
            config.put("aggregationEnabled", properties.getAnalytics().isAggregationEnabled());
            config.put("trendAnalysisEnabled", properties.getAnalytics().isTrendAnalysisEnabled());
        }
        summary.put("configuration", config);
        
        return summary;
    }
    
    @Override
    public void clearAnalytics() {
        errorAggregator.clear();
        trendAnalyzer.clearHistory();
    }
    
    private boolean isAnalyticsEnabled() {
        return properties.getAnalytics() != null && properties.getAnalytics().isEnabled();
    }
}