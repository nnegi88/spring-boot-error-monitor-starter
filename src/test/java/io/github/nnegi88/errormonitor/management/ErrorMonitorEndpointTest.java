package io.github.nnegi88.errormonitor.management;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.metrics.ErrorMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.SecurityContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErrorMonitorEndpointTest {
    
    @Mock
    private ErrorMonitorProperties properties;
    
    @Mock
    private ErrorMetrics errorMetrics;
    
    private ErrorMonitorEndpoint endpoint;
    
    @BeforeEach
    void setUp() {
        endpoint = new ErrorMonitorEndpoint(properties, errorMetrics);
    }
    
    @Test
    void testStatus() {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        when(errorMetrics.getErrorCount()).thenReturn(42L);
        when(errorMetrics.getErrorRate()).thenReturn(2.5);
        
        // When
        Map<String, Object> status = endpoint.status();
        
        // Then
        assertThat(status).containsEntry("enabled", true);
        assertThat(status).containsKey("enabled");
        assertThat(status).containsKey("statistics");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) status.get("statistics");
        assertThat(statistics).containsEntry("totalErrors", 42L);
        assertThat(statistics).containsEntry("errorRate", "2.50 errors/minute");
    }
    
    @Test
    void testStatus_Disabled() {
        // Given
        when(properties.isEnabled()).thenReturn(false);
        when(errorMetrics.getErrorCount()).thenReturn(0L);
        when(errorMetrics.getErrorRate()).thenReturn(0.0);
        
        // When
        Map<String, Object> status = endpoint.status();
        
        // Then
        assertThat(status).containsEntry("enabled", false);
        assertThat(status).containsKey("statistics");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) status.get("statistics");
        assertThat(statistics).containsEntry("totalErrors", 0L);
        assertThat(statistics).containsEntry("errorRate", "0.00 errors/minute");
    }
    
    @Test
    void testToggle_Enable() {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        
        // When
        Map<String, Object> result = endpoint.toggle(true);
        
        // Then
        assertThat(result).containsKey("message");
        assertThat(result).containsEntry("enabled", true);
    }
    
    @Test
    void testToggle_Disable() {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        
        // When
        Map<String, Object> result = endpoint.toggle(false);
        
        // Then
        assertThat(result).containsEntry("message", "Error monitoring temporarily disabled");
        assertThat(result).containsEntry("enabled", false);
    }
    
    @Test
    void testResetStatistics() {
        // When
        Map<String, Object> result = endpoint.resetStatistics();
        
        // Then
        verify(errorMetrics).reset();
        assertThat(result).containsEntry("message", "Error statistics have been reset");
        assertThat(result).containsKey("resetTime");
    }
    
    @Test
    void testIsTemporarilyDisabled() {
        // Initially should not be disabled
        assertThat(endpoint.isTemporarilyDisabled()).isFalse();
        
        // Disable temporarily
        when(properties.isEnabled()).thenReturn(true);
        endpoint.toggle(false);
        
        // Then
        assertThat(endpoint.isTemporarilyDisabled()).isTrue();
    }
    
    @Test
    void testToggle_ReEnable() {
        // Given - first disable
        when(properties.isEnabled()).thenReturn(true);
        endpoint.toggle(false);
        
        // When - re-enable
        Map<String, Object> result = endpoint.toggle(true);
        
        // Then
        assertThat(result).containsEntry("message", "Error monitoring re-enabled");
        assertThat(result).containsEntry("enabled", true);
    }
    
    @Test
    void testStatus_WithConfiguration() {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApplicationName()).thenReturn("test-app");
        when(properties.getEnvironment()).thenReturn("test");
        
        ErrorMonitorProperties.NotificationProperties notificationProps = 
                new ErrorMonitorProperties.NotificationProperties();
        notificationProps.setPlatform("slack");
        when(properties.getNotification()).thenReturn(notificationProps);
        
        ErrorMonitorProperties.RateLimitingProperties rateLimitingProps = 
                new ErrorMonitorProperties.RateLimitingProperties();
        // RateLimiting is enabled when maxErrorsPerMinute > 0
        rateLimitingProps.setMaxErrorsPerMinute(10);
        rateLimitingProps.setBurstLimit(5);
        when(properties.getRateLimiting()).thenReturn(rateLimitingProps);
        
        // When
        Map<String, Object> status = endpoint.status();
        
        // Then
        assertThat(status).containsKey("configuration");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) status.get("configuration");
        assertThat(config).containsEntry("applicationName", "test-app");
        assertThat(config).containsEntry("environment", "test");
        assertThat(config).containsEntry("notificationPlatform", "slack");
    }
}