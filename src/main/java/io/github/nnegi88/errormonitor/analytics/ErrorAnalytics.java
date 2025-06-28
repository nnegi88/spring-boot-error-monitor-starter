package io.github.nnegi88.errormonitor.analytics;

import io.github.nnegi88.errormonitor.model.ErrorEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface ErrorAnalytics {
    
    void analyzeError(ErrorEvent errorEvent);
    
    List<ErrorGroup> getErrorGroups();
    
    List<ErrorGroup> getTopErrorGroups(int limit);
    
    ErrorTrend getErrorTrend(Instant startTime, Instant endTime);
    
    Map<String, Object> getAnalyticsSummary();
    
    void clearAnalytics();
    
    interface ErrorGroup {
        String getGroupKey();
        String getErrorType();
        String getPattern();
        long getCount();
        Instant getFirstOccurrence();
        Instant getLastOccurrence();
        List<String> getAffectedEndpoints();
        Map<String, Long> getSeverityDistribution();
    }
    
    interface ErrorTrend {
        boolean isSpike();
        double getErrorRate();
        double getAverageRate();
        double getPercentageChange();
        List<TimeSlot> getTimeSlots();
        
        interface TimeSlot {
            Instant getStartTime();
            Instant getEndTime();
            long getErrorCount();
            double getErrorRate();
        }
    }
}