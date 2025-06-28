package io.github.nnegi88.errormonitor.analytics;

import io.github.nnegi88.errormonitor.model.ErrorEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Component
public class ErrorTrendAnalyzer {
    
    private static final int MAX_HISTORY_SIZE = 10000;
    private static final double SPIKE_THRESHOLD_MULTIPLIER = 2.0;
    private static final Duration DEFAULT_TIME_WINDOW = Duration.ofMinutes(5);
    
    private final Deque<TimestampedError> errorHistory = new ConcurrentLinkedDeque<>();
    private final Map<String, Deque<TimestampedError>> errorTypeHistory = new ConcurrentHashMap<>();
    
    public void recordError(ErrorEvent errorEvent) {
        TimestampedError timestampedError = new TimestampedError(errorEvent);
        
        // Add to global history
        errorHistory.addLast(timestampedError);
        if (errorHistory.size() > MAX_HISTORY_SIZE) {
            errorHistory.removeFirst();
        }
        
        // Add to type-specific history
        String errorType = getErrorType(errorEvent);
        Deque<TimestampedError> typeHistory = errorTypeHistory.computeIfAbsent(
                errorType, k -> new ConcurrentLinkedDeque<>());
        typeHistory.addLast(timestampedError);
        if (typeHistory.size() > MAX_HISTORY_SIZE / 10) {
            typeHistory.removeFirst();
        }
    }
    
    public ErrorAnalytics.ErrorTrend analyzeTrend(Instant startTime, Instant endTime) {
        return analyzeTrend(startTime, endTime, DEFAULT_TIME_WINDOW);
    }
    
    public ErrorAnalytics.ErrorTrend analyzeTrend(Instant startTime, Instant endTime, Duration timeSlotDuration) {
        List<TimestampedError> relevantErrors = errorHistory.stream()
                .filter(e -> !e.timestamp.isBefore(startTime) && !e.timestamp.isAfter(endTime))
                .collect(Collectors.toList());
        
        return new ErrorTrendImpl(relevantErrors, startTime, endTime, timeSlotDuration);
    }
    
    public Map<String, ErrorAnalytics.ErrorTrend> analyzeErrorTypeTrends(Instant startTime, Instant endTime) {
        Map<String, ErrorAnalytics.ErrorTrend> trends = new HashMap<>();
        
        for (Map.Entry<String, Deque<TimestampedError>> entry : errorTypeHistory.entrySet()) {
            List<TimestampedError> typeErrors = entry.getValue().stream()
                    .filter(e -> !e.timestamp.isBefore(startTime) && !e.timestamp.isAfter(endTime))
                    .collect(Collectors.toList());
                    
            if (!typeErrors.isEmpty()) {
                trends.put(entry.getKey(), new ErrorTrendImpl(typeErrors, startTime, endTime, DEFAULT_TIME_WINDOW));
            }
        }
        
        return trends;
    }
    
    public List<SpikeAlert> detectSpikes() {
        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        
        List<SpikeAlert> alerts = new ArrayList<>();
        
        // Analyze overall trend
        ErrorAnalytics.ErrorTrend overallTrend = analyzeTrend(oneHourAgo, now);
        if (overallTrend.isSpike()) {
            alerts.add(new SpikeAlert("Overall", overallTrend.getErrorRate(), 
                    overallTrend.getAverageRate(), now));
        }
        
        // Analyze per error type
        Map<String, ErrorAnalytics.ErrorTrend> typeTrends = analyzeErrorTypeTrends(oneHourAgo, now);
        for (Map.Entry<String, ErrorAnalytics.ErrorTrend> entry : typeTrends.entrySet()) {
            if (entry.getValue().isSpike()) {
                alerts.add(new SpikeAlert(entry.getKey(), entry.getValue().getErrorRate(), 
                        entry.getValue().getAverageRate(), now));
            }
        }
        
        return alerts;
    }
    
    public void clearHistory() {
        errorHistory.clear();
        errorTypeHistory.clear();
    }
    
    private String getErrorType(ErrorEvent errorEvent) {
        if (errorEvent.getException() != null) {
            return errorEvent.getException().getClass().getSimpleName();
        }
        return "Unknown";
    }
    
    private static class TimestampedError {
        final Instant timestamp;
        final String errorType;
        final String message;
        
        TimestampedError(ErrorEvent errorEvent) {
            this.timestamp = Instant.now();
            this.errorType = errorEvent.getException() != null ? 
                    errorEvent.getException().getClass().getSimpleName() : "Unknown";
            this.message = errorEvent.getMessage();
        }
    }
    
    private static class ErrorTrendImpl implements ErrorAnalytics.ErrorTrend {
        private final List<TimeSlotImpl> timeSlots;
        private final double averageRate;
        private final double currentRate;
        private final boolean isSpike;
        
        ErrorTrendImpl(List<TimestampedError> errors, Instant startTime, Instant endTime, Duration slotDuration) {
            this.timeSlots = calculateTimeSlots(errors, startTime, endTime, slotDuration);
            this.averageRate = calculateAverageRate();
            this.currentRate = calculateCurrentRate();
            this.isSpike = detectSpike();
        }
        
        private List<TimeSlotImpl> calculateTimeSlots(List<TimestampedError> errors, 
                                                     Instant startTime, Instant endTime, Duration slotDuration) {
            List<TimeSlotImpl> slots = new ArrayList<>();
            
            Instant slotStart = startTime;
            while (slotStart.isBefore(endTime)) {
                Instant slotEnd = slotStart.plus(slotDuration);
                if (slotEnd.isAfter(endTime)) {
                    slotEnd = endTime;
                }
                
                final Instant finalSlotStart = slotStart;
                final Instant finalSlotEnd = slotEnd;
                
                long errorCount = errors.stream()
                        .filter(e -> !e.timestamp.isBefore(finalSlotStart) && e.timestamp.isBefore(finalSlotEnd))
                        .count();
                
                double errorRate = errorCount / (slotDuration.toMinutes() > 0 ? slotDuration.toMinutes() : 1.0);
                
                slots.add(new TimeSlotImpl(slotStart, slotEnd, errorCount, errorRate));
                slotStart = slotEnd;
            }
            
            return slots;
        }
        
        private double calculateAverageRate() {
            if (timeSlots.isEmpty()) {
                return 0.0;
            }
            return timeSlots.stream()
                    .mapToDouble(TimeSlotImpl::getErrorRate)
                    .average()
                    .orElse(0.0);
        }
        
        private double calculateCurrentRate() {
            if (timeSlots.isEmpty()) {
                return 0.0;
            }
            
            // Get the last few time slots for current rate
            int recentSlots = Math.min(3, timeSlots.size());
            return timeSlots.subList(timeSlots.size() - recentSlots, timeSlots.size()).stream()
                    .mapToDouble(TimeSlotImpl::getErrorRate)
                    .average()
                    .orElse(0.0);
        }
        
        private boolean detectSpike() {
            // Consider it a spike if:
            // 1. Current rate is significantly higher than average
            // 2. There's a meaningful baseline (average > 0.1) OR current rate is high enough (> 1.0)
            if (averageRate > 0.1) {
                return currentRate > averageRate * SPIKE_THRESHOLD_MULTIPLIER;
            } else {
                // If average is very low, consider it a spike if current rate is > 1.0 error/minute
                return currentRate > 1.0;
            }
        }
        
        @Override
        public boolean isSpike() {
            return isSpike;
        }
        
        @Override
        public double getErrorRate() {
            return currentRate;
        }
        
        @Override
        public double getAverageRate() {
            return averageRate;
        }
        
        @Override
        public double getPercentageChange() {
            if (averageRate == 0) {
                return currentRate > 0 ? 100.0 : 0.0;
            }
            return ((currentRate - averageRate) / averageRate) * 100;
        }
        
        @Override
        public List<TimeSlot> getTimeSlots() {
            return new ArrayList<>(timeSlots);
        }
    }
    
    private static class TimeSlotImpl implements ErrorAnalytics.ErrorTrend.TimeSlot {
        private final Instant startTime;
        private final Instant endTime;
        private final long errorCount;
        private final double errorRate;
        
        TimeSlotImpl(Instant startTime, Instant endTime, long errorCount, double errorRate) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.errorCount = errorCount;
            this.errorRate = errorRate;
        }
        
        @Override
        public Instant getStartTime() {
            return startTime;
        }
        
        @Override
        public Instant getEndTime() {
            return endTime;
        }
        
        @Override
        public long getErrorCount() {
            return errorCount;
        }
        
        @Override
        public double getErrorRate() {
            return errorRate;
        }
    }
    
    public static class SpikeAlert {
        private final String errorType;
        private final double currentRate;
        private final double normalRate;
        private final Instant detectedAt;
        
        public SpikeAlert(String errorType, double currentRate, double normalRate, Instant detectedAt) {
            this.errorType = errorType;
            this.currentRate = currentRate;
            this.normalRate = normalRate;
            this.detectedAt = detectedAt;
        }
        
        public String getErrorType() {
            return errorType;
        }
        
        public double getCurrentRate() {
            return currentRate;
        }
        
        public double getNormalRate() {
            return normalRate;
        }
        
        public Instant getDetectedAt() {
            return detectedAt;
        }
        
        public double getSpikeMultiplier() {
            return normalRate > 0 ? currentRate / normalRate : 0;
        }
    }
}