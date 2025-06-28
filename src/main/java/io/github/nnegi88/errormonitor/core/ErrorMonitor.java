package io.github.nnegi88.errormonitor.core;

import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;

public interface ErrorMonitor {
    ErrorReportBuilder reportError(String message, Throwable exception);
    ErrorReportBuilder reportError(Throwable exception);
    ErrorReportBuilder reportError(String message);
    
    interface ErrorReportBuilder {
        ErrorReportBuilder withContext(String key, Object value);
        ErrorReportBuilder withSeverity(ErrorSeverity severity);
        ErrorReportBuilder withCorrelationId(String correlationId);
        ErrorReportBuilder withUserId(String userId);
        ErrorReportBuilder withSessionId(String sessionId);
        void send();
    }
}