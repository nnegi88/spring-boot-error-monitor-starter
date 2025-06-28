package io.github.nnegi88.errormonitor.core;

import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;

public class DefaultErrorMonitor implements ErrorMonitor {
    
    private final ErrorProcessor errorProcessor;
    
    public DefaultErrorMonitor(ErrorProcessor errorProcessor) {
        this.errorProcessor = errorProcessor;
    }
    
    @Override
    public ErrorReportBuilder reportError(String message, Throwable exception) {
        ErrorEvent event = new ErrorEvent(exception);
        if (message != null) {
            event.setMessage(message);
        }
        return new DefaultErrorReportBuilder(event, errorProcessor);
    }
    
    @Override
    public ErrorReportBuilder reportError(Throwable exception) {
        return reportError(null, exception);
    }
    
    @Override
    public ErrorReportBuilder reportError(String message) {
        ErrorEvent event = new ErrorEvent();
        event.setMessage(message);
        return new DefaultErrorReportBuilder(event, errorProcessor);
    }
    
    private static class DefaultErrorReportBuilder implements ErrorReportBuilder {
        private final ErrorEvent errorEvent;
        private final ErrorProcessor errorProcessor;
        
        public DefaultErrorReportBuilder(ErrorEvent errorEvent, ErrorProcessor errorProcessor) {
            this.errorEvent = errorEvent;
            this.errorProcessor = errorProcessor;
        }
        
        @Override
        public ErrorReportBuilder withContext(String key, Object value) {
            errorEvent.addCustomContext(key, value);
            return this;
        }
        
        @Override
        public ErrorReportBuilder withSeverity(ErrorSeverity severity) {
            errorEvent.setSeverity(severity);
            return this;
        }
        
        @Override
        public ErrorReportBuilder withCorrelationId(String correlationId) {
            errorEvent.setCorrelationId(correlationId);
            return this;
        }
        
        @Override
        public ErrorReportBuilder withUserId(String userId) {
            errorEvent.setUserId(userId);
            return this;
        }
        
        @Override
        public ErrorReportBuilder withSessionId(String sessionId) {
            errorEvent.setSessionId(sessionId);
            return this;
        }
        
        @Override
        public void send() {
            errorProcessor.processError(errorEvent);
        }
    }
}