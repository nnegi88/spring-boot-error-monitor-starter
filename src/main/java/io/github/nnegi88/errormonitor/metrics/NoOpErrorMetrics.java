package io.github.nnegi88.errormonitor.metrics;

import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingClass("io.micrometer.core.instrument.MeterRegistry")
public class NoOpErrorMetrics implements ErrorMetrics {
    
    @Override
    public void recordError(ErrorEvent errorEvent) {
    }
    
    @Override
    public void recordNotificationSuccess(String platform) {
    }
    
    @Override
    public void recordNotificationFailure(String platform, Throwable error) {
    }
    
    @Override
    public void recordProcessingTime(long durationMillis) {
    }
    
    @Override
    public void recordRateLimited() {
    }
    
    @Override
    public long getErrorCount() {
        return 0;
    }
    
    @Override
    public long getErrorCount(ErrorSeverity severity) {
        return 0;
    }
    
    @Override
    public long getErrorCount(String exceptionType) {
        return 0;
    }
    
    @Override
    public double getErrorRate() {
        return 0.0;
    }
    
    @Override
    public double getNotificationSuccessRate(String platform) {
        return 100.0;
    }
    
    @Override
    public void reset() {
    }
}