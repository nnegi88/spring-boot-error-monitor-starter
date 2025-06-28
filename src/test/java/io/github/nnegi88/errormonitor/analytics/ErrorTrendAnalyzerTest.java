package io.github.nnegi88.errormonitor.analytics;

import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorTrendAnalyzerTest {
    
    private ErrorTrendAnalyzer analyzer;
    
    @BeforeEach
    void setUp() {
        analyzer = new ErrorTrendAnalyzer();
    }
    
    @Test
    void testRecordErrorAndAnalyzeTrend() throws InterruptedException {
        // Given - Record errors over time
        for (int i = 0; i < 10; i++) {
            ErrorEvent event = ErrorEvent.builder()
                    .exception(new RuntimeException("Error " + i))
                    .message("Test error " + i)
                    .build();
            analyzer.recordError(event);
            Thread.sleep(50); // Spread errors over time
        }
        
        // When
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(1, ChronoUnit.MINUTES);
        ErrorAnalytics.ErrorTrend trend = analyzer.analyzeTrend(startTime, endTime);
        
        // Then
        assertThat(trend).isNotNull();
        assertThat(trend.getTimeSlots()).isNotEmpty();
        assertThat(trend.getErrorRate()).isGreaterThan(0);
        assertThat(trend.getAverageRate()).isGreaterThan(0);
    }
    
    @Test
    void testSpikeDetection() throws InterruptedException {
        // Given - Create baseline errors spread over time
        for (int i = 0; i < 20; i++) {
            ErrorEvent event = ErrorEvent.builder()
                    .exception(new RuntimeException("Baseline error"))
                    .build();
            analyzer.recordError(event);
            Thread.sleep(100); // Spread over 2 seconds
        }
        
        // Wait a bit to separate baseline from spike
        Thread.sleep(500);
        
        // Create spike - many errors in a short time
        for (int i = 0; i < 100; i++) {
            ErrorEvent event = ErrorEvent.builder()
                    .exception(new RuntimeException("Spike error"))
                    .build();
            analyzer.recordError(event);
            if (i % 10 == 0) {
                Thread.sleep(10); // Small delays to avoid timing issues
            }
        }
        
        // When
        List<ErrorTrendAnalyzer.SpikeAlert> alerts = analyzer.detectSpikes();
        
        // Then
        assertThat(alerts).isNotEmpty();
        ErrorTrendAnalyzer.SpikeAlert alert = alerts.get(0);
        assertThat(alert.getCurrentRate()).isGreaterThan(alert.getNormalRate());
        assertThat(alert.getSpikeMultiplier()).isGreaterThan(1.0);
    }
    
    @Test
    void testAnalyzeErrorTypeTrends() {
        // Given
        for (int i = 0; i < 5; i++) {
            analyzer.recordError(ErrorEvent.builder()
                    .exception(new NullPointerException("NPE " + i))
                    .build());
            
            analyzer.recordError(ErrorEvent.builder()
                    .exception(new IllegalArgumentException("IAE " + i))
                    .build());
        }
        
        // When
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(1, ChronoUnit.MINUTES);
        Map<String, ErrorAnalytics.ErrorTrend> typeTrends = analyzer.analyzeErrorTypeTrends(startTime, endTime);
        
        // Then
        assertThat(typeTrends).containsKeys("NullPointerException", "IllegalArgumentException");
        assertThat(typeTrends.get("NullPointerException").getTimeSlots()).isNotEmpty();
        assertThat(typeTrends.get("IllegalArgumentException").getTimeSlots()).isNotEmpty();
    }
    
    @Test
    void testTimeSlots() {
        // Given
        for (int i = 0; i < 20; i++) {
            analyzer.recordError(ErrorEvent.builder()
                    .exception(new RuntimeException("Error " + i))
                    .build());
        }
        
        // When
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(10, ChronoUnit.MINUTES);
        ErrorAnalytics.ErrorTrend trend = analyzer.analyzeTrend(startTime, endTime, Duration.ofMinutes(2));
        
        // Then
        assertThat(trend.getTimeSlots()).isNotEmpty();
        
        ErrorAnalytics.ErrorTrend.TimeSlot firstSlot = trend.getTimeSlots().get(0);
        assertThat(firstSlot.getStartTime()).isNotNull();
        assertThat(firstSlot.getEndTime()).isNotNull();
        assertThat(firstSlot.getErrorCount()).isGreaterThanOrEqualTo(0);
        assertThat(firstSlot.getErrorRate()).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    void testClearHistory() {
        // Given
        for (int i = 0; i < 5; i++) {
            analyzer.recordError(ErrorEvent.builder()
                    .exception(new RuntimeException("Error " + i))
                    .build());
        }
        
        // Verify errors are recorded
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(1, ChronoUnit.MINUTES);
        ErrorAnalytics.ErrorTrend beforeClear = analyzer.analyzeTrend(startTime, endTime);
        assertThat(beforeClear.getTimeSlots()).isNotEmpty();
        
        // When
        analyzer.clearHistory();
        
        // Then
        ErrorAnalytics.ErrorTrend afterClear = analyzer.analyzeTrend(startTime, endTime);
        assertThat(afterClear.getTimeSlots()).allMatch(slot -> slot.getErrorCount() == 0);
    }
    
    @Test
    void testPercentageChange() {
        // Given - Create baseline
        for (int i = 0; i < 10; i++) {
            analyzer.recordError(ErrorEvent.builder()
                    .exception(new RuntimeException("Error"))
                    .build());
        }
        
        // When
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(5, ChronoUnit.MINUTES);
        ErrorAnalytics.ErrorTrend trend = analyzer.analyzeTrend(startTime, endTime);
        
        // Then
        double percentageChange = trend.getPercentageChange();
        assertThat(percentageChange).isNotNull();
        
        // If current rate equals average rate, percentage change should be near 0
        if (Math.abs(trend.getErrorRate() - trend.getAverageRate()) < 0.001) {
            assertThat(Math.abs(percentageChange)).isLessThan(1.0);
        }
    }
    
    @Test
    void testNoErrorsScenario() {
        // When
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(1, ChronoUnit.HOURS);
        ErrorAnalytics.ErrorTrend trend = analyzer.analyzeTrend(startTime, endTime);
        
        // Then
        assertThat(trend.getErrorRate()).isEqualTo(0.0);
        assertThat(trend.getAverageRate()).isEqualTo(0.0);
        assertThat(trend.isSpike()).isFalse();
        assertThat(trend.getPercentageChange()).isEqualTo(0.0);
    }
    
    @Test
    void testSpikeAlertDetails() {
        // Given
        ErrorTrendAnalyzer.SpikeAlert alert = new ErrorTrendAnalyzer.SpikeAlert(
                "RuntimeException",
                10.0,  // current rate
                2.0,   // normal rate
                Instant.now()
        );
        
        // Then
        assertThat(alert.getErrorType()).isEqualTo("RuntimeException");
        assertThat(alert.getCurrentRate()).isEqualTo(10.0);
        assertThat(alert.getNormalRate()).isEqualTo(2.0);
        assertThat(alert.getSpikeMultiplier()).isEqualTo(5.0);
        assertThat(alert.getDetectedAt()).isNotNull();
    }
    
    @Test
    void testErrorWithoutException() {
        // Given
        ErrorEvent event = ErrorEvent.builder()
                .message("Custom error without exception")
                .severity(ErrorSeverity.ERROR)
                .build();
        
        // When
        analyzer.recordError(event);
        
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(1, ChronoUnit.MINUTES);
        Map<String, ErrorAnalytics.ErrorTrend> typeTrends = analyzer.analyzeErrorTypeTrends(startTime, endTime);
        
        // Then
        assertThat(typeTrends).containsKey("Unknown");
    }
    
    @Test
    void testMaxHistorySize() {
        // Given - Add more than MAX_HISTORY_SIZE errors
        for (int i = 0; i < 15000; i++) {
            analyzer.recordError(ErrorEvent.builder()
                    .exception(new RuntimeException("Error " + i))
                    .build());
        }
        
        // When
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(1, ChronoUnit.HOURS);
        ErrorAnalytics.ErrorTrend trend = analyzer.analyzeTrend(startTime, endTime);
        
        // Then - Should still work but with limited history
        assertThat(trend).isNotNull();
        assertThat(trend.getTimeSlots()).isNotEmpty();
    }
}