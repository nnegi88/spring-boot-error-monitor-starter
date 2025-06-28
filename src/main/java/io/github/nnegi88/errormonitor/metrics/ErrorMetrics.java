package io.github.nnegi88.errormonitor.metrics;

import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;

public interface ErrorMetrics {
    
    void recordError(ErrorEvent errorEvent);
    
    void recordNotificationSuccess(String platform);
    
    void recordNotificationFailure(String platform, Throwable error);
    
    void recordProcessingTime(long durationMillis);
    
    void recordRateLimited();
    
    long getErrorCount();
    
    long getErrorCount(ErrorSeverity severity);
    
    long getErrorCount(String exceptionType);
    
    double getErrorRate();
    
    double getNotificationSuccessRate(String platform);
    
    void reset();
}