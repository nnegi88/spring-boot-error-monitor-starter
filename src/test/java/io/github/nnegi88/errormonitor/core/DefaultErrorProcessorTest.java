package io.github.nnegi88.errormonitor.core;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.filter.ErrorFilter;
import io.github.nnegi88.errormonitor.metrics.ErrorMetrics;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import io.github.nnegi88.errormonitor.notification.NotificationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultErrorProcessorTest {

    @Mock
    private NotificationClient notificationClient;
    
    @Mock
    private ErrorFilter errorFilter;
    
    @Mock
    private ErrorMonitorProperties properties;
    
    @Mock
    private ErrorMetrics errorMetrics;
    
    private DefaultErrorProcessor errorProcessor;
    
    @BeforeEach
    void setUp() {
        errorProcessor = new DefaultErrorProcessor(
            notificationClient,
            errorFilter,
            properties,
            errorMetrics,
            "test-app",
            "test"
        );
    }
    
    @Test
    void testProcessError_EnabledAndFilterPasses() {
        // Given
        ErrorEvent event = createErrorEvent();
        when(properties.isEnabled()).thenReturn(true);
        when(errorFilter.shouldReport(event)).thenReturn(true);
        doNothing().when(notificationClient).sendNotification(any());
        
        // When
        errorProcessor.processError(event);
        
        // Then
        // Allow async processing to start
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        verify(errorFilter).shouldReport(event);
        assertThat(event.getApplicationName()).isEqualTo("test-app");
        assertThat(event.getEnvironment()).isEqualTo("test");
    }
    
    @Test
    void testProcessError_DisabledMonitor() {
        // Given
        ErrorEvent event = createErrorEvent();
        when(properties.isEnabled()).thenReturn(false);
        
        // When
        errorProcessor.processError(event);
        
        // Then
        verify(errorFilter, never()).shouldReport(any());
        verify(notificationClient, never()).sendNotification(any());
    }
    
    @Test
    void testProcessError_FilterRejects() {
        // Given
        ErrorEvent event = createErrorEvent();
        when(properties.isEnabled()).thenReturn(true);
        when(errorFilter.shouldReport(event)).thenReturn(false);
        
        // When
        errorProcessor.processError(event);
        
        // Then
        verify(errorFilter).shouldReport(event);
        verify(notificationClient, never()).sendNotification(any());
    }
    
    @Test
    void testShouldProcess_Enabled() {
        // Given
        ErrorEvent event = createErrorEvent();
        when(properties.isEnabled()).thenReturn(true);
        when(errorFilter.shouldReport(event)).thenReturn(true);
        
        // When
        boolean result = errorProcessor.shouldProcess(event);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    void testShouldProcess_Disabled() {
        // Given
        ErrorEvent event = createErrorEvent();
        when(properties.isEnabled()).thenReturn(false);
        
        // When
        boolean result = errorProcessor.shouldProcess(event);
        
        // Then
        assertThat(result).isFalse();
        verify(errorFilter, never()).shouldReport(any());
    }
    
    @Test
    void testProcessError_NotificationException() {
        // Given
        ErrorEvent event = createErrorEvent();
        when(properties.isEnabled()).thenReturn(true);
        when(errorFilter.shouldReport(event)).thenReturn(true);
        doThrow(new RuntimeException("Connection failed")).when(notificationClient).sendNotification(any());
        
        // When - Should not throw
        errorProcessor.processError(event);
        
        // Then - Error is logged but doesn't propagate
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }
    }
    
    @Test
    void testProcessError_ApplicationContextSet() {
        // Given
        ErrorEvent event = createErrorEvent();
        event.setApplicationName(null);
        event.setEnvironment(null);
        
        when(properties.isEnabled()).thenReturn(true);
        when(errorFilter.shouldReport(event)).thenReturn(true);
        
        // When
        errorProcessor.processError(event);
        
        // Then
        assertThat(event.getApplicationName()).isEqualTo("test-app");
        assertThat(event.getEnvironment()).isEqualTo("test");
    }
    
    private ErrorEvent createErrorEvent() {
        ErrorEvent event = new ErrorEvent();
        event.setException(new RuntimeException("Test error"));
        event.setMessage("Test error message");
        event.setSeverity(ErrorSeverity.MEDIUM);
        return event;
    }
}