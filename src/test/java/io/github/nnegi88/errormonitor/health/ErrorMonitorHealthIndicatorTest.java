package io.github.nnegi88.errormonitor.health;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.metrics.ErrorMetrics;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorMonitorHealthIndicatorTest {
    
    @Mock
    private ErrorMonitorProperties properties;
    
    @Mock
    private ErrorMetrics errorMetrics;
    
    private ErrorMonitorHealthIndicator healthIndicator;
    
    @BeforeEach
    void setUp() {
        healthIndicator = new ErrorMonitorHealthIndicator(properties, errorMetrics);
    }
    
    @Test
    void testHealth_EnabledWithNoErrors() {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        when(errorMetrics.getErrorCount()).thenReturn(0L);
        when(errorMetrics.getErrorRate()).thenReturn(0.0);
        when(errorMetrics.getNotificationSuccessRate("slack")).thenReturn(100.0);
        
        ErrorMonitorProperties.NotificationProperties notificationProps = 
                new ErrorMonitorProperties.NotificationProperties();
        notificationProps.setPlatform("slack");
        when(properties.getNotification()).thenReturn(notificationProps);
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("enabled", true);
        assertThat(health.getDetails()).containsEntry("totalErrors", 0L);
        assertThat(health.getDetails()).containsEntry("errorRate", "0.0 errors/minute");
        assertThat(health.getDetails()).containsKey("notification");
        
        @SuppressWarnings("unchecked")
        var notification = (java.util.Map<String, Object>) health.getDetails().get("notification");
        assertThat(notification).containsEntry("platform", "slack");
        assertThat(notification).containsEntry("slackSuccessRate", "100.0%");
    }
    
    @Test
    void testHealth_EnabledWithErrors() {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        when(errorMetrics.getErrorCount()).thenReturn(42L);
        when(errorMetrics.getErrorRate()).thenReturn(2.5);
        when(errorMetrics.getErrorCount(ErrorSeverity.CRITICAL)).thenReturn(5L);
        when(errorMetrics.getNotificationSuccessRate("teams")).thenReturn(95.5);
        
        ErrorMonitorProperties.NotificationProperties notificationProps = 
                new ErrorMonitorProperties.NotificationProperties();
        notificationProps.setPlatform("teams");
        when(properties.getNotification()).thenReturn(notificationProps);
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("enabled", true);
        assertThat(health.getDetails()).containsEntry("totalErrors", 42L);
        assertThat(health.getDetails()).containsEntry("errorRate", "2.5 errors/minute");
        assertThat(health.getDetails()).containsEntry("criticalErrors", 5L);
        
        @SuppressWarnings("unchecked")
        var notification = (java.util.Map<String, Object>) health.getDetails().get("notification");
        assertThat(notification).containsEntry("platform", "teams");
        assertThat(notification).containsEntry("teamsSuccessRate", "95.5%");
    }
    
    @Test
    void testHealth_Disabled() {
        // Given
        when(properties.isEnabled()).thenReturn(false);
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("enabled", false);
        assertThat(health.getDetails()).containsEntry("reason", "Error monitoring is disabled");
    }
    
    @Test
    void testHealth_BothPlatforms() {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        when(errorMetrics.getErrorCount()).thenReturn(10L);
        when(errorMetrics.getErrorRate()).thenReturn(1.0);
        when(errorMetrics.getNotificationSuccessRate("slack")).thenReturn(98.0);
        when(errorMetrics.getNotificationSuccessRate("teams")).thenReturn(97.0);
        
        ErrorMonitorProperties.NotificationProperties notificationProps = 
                new ErrorMonitorProperties.NotificationProperties();
        notificationProps.setPlatform("both");
        when(properties.getNotification()).thenReturn(notificationProps);
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        
        @SuppressWarnings("unchecked")
        var notification = (java.util.Map<String, Object>) health.getDetails().get("notification");
        assertThat(notification).containsEntry("platform", "both");
        assertThat(notification).containsEntry("slackSuccessRate", "98.0%");
        assertThat(notification).containsEntry("teamsSuccessRate", "97.0%");
    }
    
    @Test
    void testHealth_WithHighErrorRate() {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        when(errorMetrics.getErrorCount()).thenReturn(1000L);
        when(errorMetrics.getErrorRate()).thenReturn(50.0);
        when(errorMetrics.getErrorCount(ErrorSeverity.CRITICAL)).thenReturn(100L);
        
        ErrorMonitorProperties.NotificationProperties notificationProps = 
                new ErrorMonitorProperties.NotificationProperties();
        notificationProps.setPlatform("slack");
        when(properties.getNotification()).thenReturn(notificationProps);
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        // Still UP because the error monitor itself is functioning
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("totalErrors", 1000L);
        assertThat(health.getDetails()).containsEntry("errorRate", "50.0 errors/minute");
        assertThat(health.getDetails()).containsEntry("criticalErrors", 100L);
    }
    
    @Test
    void testHealth_WithNullNotificationProperties() {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        when(errorMetrics.getErrorCount()).thenReturn(0L);
        when(errorMetrics.getErrorRate()).thenReturn(0.0);
        when(properties.getNotification()).thenReturn(null);
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("enabled", true);
        assertThat(health.getDetails()).doesNotContainKey("notification");
    }
    
    @Test
    void testHealth_WithException() {
        // Given
        when(properties.isEnabled()).thenThrow(new RuntimeException("Configuration error"));
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails().get("error").toString()).contains("Configuration error");
    }
}