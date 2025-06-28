package io.github.nnegi88.errormonitor.metrics;

import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerErrorMetricsTest {
    
    private MeterRegistry meterRegistry;
    private MicrometerErrorMetrics metrics;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new MicrometerErrorMetrics(meterRegistry);
    }
    
    @Test
    void testRecordError() {
        // Given
        ErrorEvent event = ErrorEvent.builder()
                .applicationName("test-app")
                .environment("test")
                .severity(ErrorSeverity.ERROR)
                .exception(new RuntimeException("Test error"))
                .build();
        
        // When
        metrics.recordError(event);
        
        // Then
        Counter counter = meterRegistry.find("error.monitor.errors.total")
                .tag("severity", "ERROR")
                .tag("exception.type", "RuntimeException")
                .tag("application", "test-app")
                .tag("environment", "test")
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
    
    @Test
    void testRecordError_WithNullValues() {
        // Given
        ErrorEvent event = new ErrorEvent();
        event.setMessage("Test error without exception");
        
        // When
        metrics.recordError(event);
        
        // Then
        Counter counter = meterRegistry.find("error.monitor.errors.total")
                .tag("severity", "ERROR")
                .tag("exception.type", "Unknown")
                .tag("application", "unknown")
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
    
    @Test
    void testRecordNotificationSuccess() {
        // When
        metrics.recordNotificationSuccess("slack");
        metrics.recordNotificationSuccess("slack");
        metrics.recordNotificationSuccess("teams");
        
        // Then
        Counter slackCounter = meterRegistry.find("error.monitor.notifications")
                .tag("platform", "slack")
                .tag("status", "success")
                .counter();
        
        Counter teamsCounter = meterRegistry.find("error.monitor.notifications")
                .tag("platform", "teams")
                .tag("status", "success")
                .counter();
        
        assertThat(slackCounter.count()).isEqualTo(2.0);
        assertThat(teamsCounter.count()).isEqualTo(1.0);
    }
    
    @Test
    void testRecordNotificationFailure() {
        // When
        metrics.recordNotificationFailure("slack", new RuntimeException("Connection failed"));
        metrics.recordNotificationFailure("teams", null);
        
        // Then
        Counter slackCounter = meterRegistry.find("error.monitor.notifications")
                .tag("platform", "slack")
                .tag("status", "failure")
                .tag("error.type", "RuntimeException")
                .counter();
        
        Counter teamsCounter = meterRegistry.find("error.monitor.notifications")
                .tag("platform", "teams")
                .tag("status", "failure")
                .tag("error.type", "Unknown")
                .counter();
        
        assertThat(slackCounter.count()).isEqualTo(1.0);
        assertThat(teamsCounter.count()).isEqualTo(1.0);
    }
    
    @Test
    void testRecordProcessingTime() {
        // When
        metrics.recordProcessingTime(100);
        metrics.recordProcessingTime(200);
        
        // Then
        Timer timer = meterRegistry.find("error.monitor.processing.time").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(2);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }
    
    @Test
    void testRecordRateLimited() {
        // When
        metrics.recordRateLimited();
        metrics.recordRateLimited();
        metrics.recordRateLimited();
        
        // Then
        Counter counter = meterRegistry.find("error.monitor.rate.limited").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(3.0);
    }
    
    @Test
    void testGetErrorCount() {
        // Given
        ErrorEvent event1 = ErrorEvent.builder()
                .severity(ErrorSeverity.ERROR)
                .exception(new RuntimeException("Error 1"))
                .build();
        
        ErrorEvent event2 = ErrorEvent.builder()
                .severity(ErrorSeverity.CRITICAL)
                .exception(new NullPointerException("Error 2"))
                .build();
        
        // When
        metrics.recordError(event1);
        metrics.recordError(event2);
        
        // Then
        assertThat(metrics.getErrorCount()).isEqualTo(2);
    }
    
    @Test
    void testGetErrorCountBySeverity() {
        // Given
        ErrorEvent errorEvent = ErrorEvent.builder()
                .severity(ErrorSeverity.ERROR)
                .exception(new RuntimeException("Error"))
                .build();
        
        ErrorEvent criticalEvent = ErrorEvent.builder()
                .severity(ErrorSeverity.CRITICAL)
                .exception(new RuntimeException("Critical"))
                .build();
        
        ErrorEvent warningEvent = ErrorEvent.builder()
                .severity(ErrorSeverity.WARNING)
                .exception(new RuntimeException("Warning"))
                .build();
        
        // When
        metrics.recordError(errorEvent);
        metrics.recordError(errorEvent);
        metrics.recordError(criticalEvent);
        metrics.recordError(warningEvent);
        
        // Then
        assertThat(metrics.getErrorCount(ErrorSeverity.ERROR)).isEqualTo(2);
        assertThat(metrics.getErrorCount(ErrorSeverity.CRITICAL)).isEqualTo(1);
        assertThat(metrics.getErrorCount(ErrorSeverity.WARNING)).isEqualTo(1);
        assertThat(metrics.getErrorCount(ErrorSeverity.INFO)).isEqualTo(0);
    }
    
    @Test
    void testGetErrorCountByExceptionType() {
        // Given
        ErrorEvent runtimeError = ErrorEvent.builder()
                .exception(new RuntimeException("Runtime"))
                .build();
        
        ErrorEvent nullPointerError = ErrorEvent.builder()
                .exception(new NullPointerException("NPE"))
                .build();
        
        // When
        metrics.recordError(runtimeError);
        metrics.recordError(runtimeError);
        metrics.recordError(nullPointerError);
        
        // Then
        assertThat(metrics.getErrorCount("RuntimeException")).isEqualTo(2);
        assertThat(metrics.getErrorCount("NullPointerException")).isEqualTo(1);
        assertThat(metrics.getErrorCount("IllegalArgumentException")).isEqualTo(0);
    }
    
    @Test
    void testGetErrorRate() throws InterruptedException {
        // Given
        ErrorEvent event = ErrorEvent.builder()
                .exception(new RuntimeException("Error"))
                .build();
        
        // When
        metrics.recordError(event);
        metrics.recordError(event);
        metrics.recordError(event);
        
        // Wait a bit to ensure time has passed
        Thread.sleep(100);
        
        // Then
        double errorRate = metrics.getErrorRate();
        // Should be approximately 3 errors per minute (since less than a minute has passed)
        assertThat(errorRate).isGreaterThanOrEqualTo(3.0);
    }
    
    @Test
    void testGetNotificationSuccessRate() {
        // When
        metrics.recordNotificationSuccess("slack");
        metrics.recordNotificationSuccess("slack");
        metrics.recordNotificationSuccess("slack");
        metrics.recordNotificationFailure("slack", new RuntimeException());
        
        metrics.recordNotificationSuccess("teams");
        metrics.recordNotificationFailure("teams", new RuntimeException());
        metrics.recordNotificationFailure("teams", new RuntimeException());
        
        // Then
        assertThat(metrics.getNotificationSuccessRate("slack")).isEqualTo(75.0);
        assertThat(metrics.getNotificationSuccessRate("teams")).isEqualTo(33.33, offset(0.01));
        assertThat(metrics.getNotificationSuccessRate("discord")).isEqualTo(100.0); // No attempts
    }
    
    @Test
    void testReset() {
        // Given
        ErrorEvent event = ErrorEvent.builder()
                .exception(new RuntimeException("Error"))
                .build();
        
        metrics.recordError(event);
        
        // When
        long beforeReset = metrics.getErrorCount();
        metrics.reset();
        
        // Then
        assertThat(beforeReset).isEqualTo(1);
        // Note: Reset only affects the time tracking for rate calculation
        // Counters in Micrometer are monotonic and cannot be reset
    }
    
    private static org.assertj.core.data.Offset<Double> offset(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}