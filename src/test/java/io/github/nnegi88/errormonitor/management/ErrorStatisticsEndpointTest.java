package io.github.nnegi88.errormonitor.management;

import io.github.nnegi88.errormonitor.metrics.ErrorMetrics;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErrorStatisticsEndpointTest {
    
    @Mock
    private ErrorMetrics errorMetrics;
    
    private ErrorStatisticsEndpoint endpoint;
    
    @BeforeEach
    void setUp() {
        endpoint = new ErrorStatisticsEndpoint(errorMetrics);
    }
    
    @Test
    void testGetAllStatistics() {
        // Given
        when(errorMetrics.getErrorCount()).thenReturn(100L);
        when(errorMetrics.getErrorRate()).thenReturn(5.0);
        when(errorMetrics.getErrorCount(ErrorSeverity.CRITICAL)).thenReturn(10L);
        when(errorMetrics.getErrorCount(ErrorSeverity.HIGH)).thenReturn(15L);
        when(errorMetrics.getErrorCount(ErrorSeverity.MEDIUM)).thenReturn(20L);
        when(errorMetrics.getErrorCount(ErrorSeverity.LOW)).thenReturn(5L);
        when(errorMetrics.getErrorCount(ErrorSeverity.ERROR)).thenReturn(50L);
        when(errorMetrics.getErrorCount(ErrorSeverity.WARNING)).thenReturn(30L);
        when(errorMetrics.getErrorCount(ErrorSeverity.INFO)).thenReturn(10L);
        
        when(errorMetrics.getNotificationSuccessRate("slack")).thenReturn(95.5);
        when(errorMetrics.getNotificationSuccessRate("teams")).thenReturn(98.0);
        
        // When
        Map<String, Object> stats = endpoint.getAllStatistics();
        
        // Then
        assertThat(stats).containsKeys("summary", "topErrorTypes", "trends", 
                                     "recentErrors", "severityDistribution", "notificationPerformance");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) stats.get("summary");
        assertThat(summary).containsEntry("totalErrors", 100L);
        assertThat(summary).containsEntry("errorRate", "5.00 errors/minute");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> severityDist = (Map<String, Object>) stats.get("severityDistribution");
        assertThat(severityDist).isNotNull();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> notifPerf = (Map<String, Object>) stats.get("notificationPerformance");
        assertThat(notifPerf).containsKeys("slack", "teams");
    }
    
    @Test
    void testGetStatisticsByType() {
        // Given
        endpoint.recordError("NullPointerException", "Test error", ErrorSeverity.ERROR, "/api/test");
        
        // When
        Map<String, Object> stats = endpoint.getStatisticsByType("NullPointerException");
        
        // Then
        assertThat(stats).containsEntry("errorType", "NullPointerException");
        assertThat(stats).containsKeys("count", "firstOccurrence", "lastOccurrence", 
                                      "averageOccurrencePerHour", "affectedEndpoints", "severityBreakdown");
    }
    
    @Test
    void testGetStatisticsByType_NotFound() {
        // When
        Map<String, Object> stats = endpoint.getStatisticsByType("NonExistentException");
        
        // Then
        assertThat(stats).containsEntry("error", "No statistics found for error type: NonExistentException");
    }
    
    @Test
    void testRecordError() {
        // When
        endpoint.recordError("RuntimeException", "Runtime error", ErrorSeverity.ERROR, "/api/users");
        endpoint.recordError("RuntimeException", "Another runtime error", ErrorSeverity.CRITICAL, "/api/orders");
        
        // Then
        Map<String, Object> stats = endpoint.getStatisticsByType("RuntimeException");
        assertThat(stats).containsEntry("errorType", "RuntimeException");
        assertThat(stats).containsEntry("count", 2L);
        
        @SuppressWarnings("unchecked")
        var affectedEndpoints = (java.util.Set<String>) stats.get("affectedEndpoints");
        assertThat(affectedEndpoints).containsExactlyInAnyOrder("/api/users", "/api/orders");
    }
    
    @Test
    void testGetSummaryStatistics() {
        // Given
        when(errorMetrics.getErrorCount()).thenReturn(50L);
        when(errorMetrics.getErrorRate()).thenReturn(2.5);
        
        // When
        Map<String, Object> allStats = endpoint.getAllStatistics();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) allStats.get("summary");
        
        // Then
        assertThat(summary).containsEntry("totalErrors", 50L);
        assertThat(summary).containsEntry("errorRate", "2.50 errors/minute");
        assertThat(summary).containsKey("uniqueErrorTypes");
    }
    
    @Test
    void testSeverityDistribution() {
        // Given
        when(errorMetrics.getErrorCount()).thenReturn(100L);
        when(errorMetrics.getErrorCount(ErrorSeverity.CRITICAL)).thenReturn(10L);
        when(errorMetrics.getErrorCount(ErrorSeverity.HIGH)).thenReturn(15L);
        when(errorMetrics.getErrorCount(ErrorSeverity.MEDIUM)).thenReturn(20L);
        when(errorMetrics.getErrorCount(ErrorSeverity.LOW)).thenReturn(5L);
        when(errorMetrics.getErrorCount(ErrorSeverity.ERROR)).thenReturn(50L);
        when(errorMetrics.getErrorCount(ErrorSeverity.WARNING)).thenReturn(30L);
        when(errorMetrics.getErrorCount(ErrorSeverity.INFO)).thenReturn(10L);
        
        // When
        Map<String, Object> allStats = endpoint.getAllStatistics();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> severityDist = (Map<String, Object>) allStats.get("severityDistribution");
        
        // Then
        assertThat(severityDist).containsKeys("CRITICAL", "ERROR", "WARNING", "INFO");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> criticalInfo = (Map<String, Object>) severityDist.get("CRITICAL");
        assertThat(criticalInfo).containsEntry("count", 10L);
        assertThat(criticalInfo).containsEntry("percentage", "10.00%");
    }
    
    @Test
    void testNotificationPerformance() {
        // Given
        when(errorMetrics.getNotificationSuccessRate("slack")).thenReturn(95.5);
        when(errorMetrics.getNotificationSuccessRate("teams")).thenReturn(98.0);
        
        // When
        Map<String, Object> allStats = endpoint.getAllStatistics();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> notifPerf = (Map<String, Object>) allStats.get("notificationPerformance");
        
        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> slackPerf = (Map<String, Object>) notifPerf.get("slack");
        assertThat(slackPerf).containsEntry("successRate", "95.50%");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> teamsPerf = (Map<String, Object>) notifPerf.get("teams");
        assertThat(teamsPerf).containsEntry("successRate", "98.00%");
    }
}