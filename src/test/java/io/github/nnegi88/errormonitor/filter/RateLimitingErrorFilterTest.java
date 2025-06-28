package io.github.nnegi88.errormonitor.filter;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingErrorFilterTest {

    private RateLimitingErrorFilter rateLimitingFilter;
    private ErrorMonitorProperties.RateLimitingProperties rateLimitingProperties;
    
    @BeforeEach
    void setUp() {
        rateLimitingProperties = new ErrorMonitorProperties.RateLimitingProperties();
        rateLimitingProperties.setMaxErrorsPerMinute(10);
        rateLimitingProperties.setBurstLimit(3);
        
        rateLimitingFilter = new RateLimitingErrorFilter(rateLimitingProperties);
    }
    
    @Test
    void testBurstLimit() {
        ErrorEvent event = createErrorEvent();
        
        // First 3 events should pass (burst limit)
        assertThat(rateLimitingFilter.shouldReport(event)).isTrue();
        assertThat(rateLimitingFilter.shouldReport(event)).isTrue();
        assertThat(rateLimitingFilter.shouldReport(event)).isTrue();
        
        // 4th event should be rate limited
        assertThat(rateLimitingFilter.shouldReport(event)).isFalse();
    }
    
    @Test
    void testPerMinuteLimit() throws InterruptedException {
        // Set burst limit high enough to not interfere with the per-minute test
        rateLimitingProperties.setBurstLimit(15);
        rateLimitingFilter = new RateLimitingErrorFilter(rateLimitingProperties);
        
        ErrorEvent event = createErrorEvent();
        
        // Send 10 events (max per minute)
        for (int i = 0; i < 10; i++) {
            assertThat(rateLimitingFilter.shouldReport(event)).isTrue();
            // Small delay to avoid burst limit
            TimeUnit.MILLISECONDS.sleep(100);
        }
        
        // 11th event should be rate limited
        assertThat(rateLimitingFilter.shouldReport(event)).isFalse();
    }
    
    @Test
    void testTimeWindowReset() throws InterruptedException {
        ErrorEvent event = createErrorEvent();
        
        // Fill up the burst limit
        for (int i = 0; i < 3; i++) {
            rateLimitingFilter.shouldReport(event);
        }
        
        // Should be rate limited
        assertThat(rateLimitingFilter.shouldReport(event)).isFalse();
        
        // Wait for rate limit window to reset
        TimeUnit.SECONDS.sleep(1);
        
        // Should be allowed again
        assertThat(rateLimitingFilter.shouldReport(event)).isTrue();
    }
    
    @Test
    void testDisabledRateLimiting() {
        rateLimitingProperties.setMaxErrorsPerMinute(-1);
        rateLimitingProperties.setBurstLimit(-1);
        rateLimitingFilter = new RateLimitingErrorFilter(rateLimitingProperties);
        
        ErrorEvent event = createErrorEvent();
        
        // Should allow unlimited events
        for (int i = 0; i < 100; i++) {
            assertThat(rateLimitingFilter.shouldReport(event)).isTrue();
        }
    }
    
    @Test
    void testOldEventsCleanup() throws InterruptedException {
        ErrorEvent event = createErrorEvent();
        
        // Send some events
        for (int i = 0; i < 5; i++) {
            rateLimitingFilter.shouldReport(event);
            TimeUnit.MILLISECONDS.sleep(100);
        }
        
        // Wait for events to become old (> 1 minute)
        TimeUnit.SECONDS.sleep(61);
        
        // Should be able to send new events
        assertThat(rateLimitingFilter.shouldReport(event)).isTrue();
    }
    
    private ErrorEvent createErrorEvent() {
        ErrorEvent event = new ErrorEvent();
        event.setApplicationName("test-app");
        event.setEnvironment("test");
        event.setTimestamp(LocalDateTime.now());
        event.setException(new RuntimeException("Test error"));
        return event;
    }
}