package io.github.nnegi88.errormonitor.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ErrorEvent {
    private String id;
    private String applicationName;
    private String environment;
    private LocalDateTime timestamp;
    private Throwable exception;
    private ErrorSeverity severity;
    private String message;
    private RequestContext requestContext;
    private Map<String, Object> customContext = new HashMap<>();
    private String correlationId;
    private String userId;
    private String sessionId;
    
    public ErrorEvent() {
        this.timestamp = LocalDateTime.now();
        this.id = java.util.UUID.randomUUID().toString();
        this.severity = ErrorSeverity.ERROR;
    }
    
    public ErrorEvent(Throwable exception) {
        this();
        this.exception = exception;
        this.message = exception.getMessage();
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getApplicationName() {
        return applicationName;
    }
    
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Throwable getException() {
        return exception;
    }
    
    public void setException(Throwable exception) {
        this.exception = exception;
    }
    
    public ErrorSeverity getSeverity() {
        return severity;
    }
    
    public void setSeverity(ErrorSeverity severity) {
        this.severity = severity;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public RequestContext getRequestContext() {
        return requestContext;
    }
    
    public void setRequestContext(RequestContext requestContext) {
        this.requestContext = requestContext;
    }
    
    public Map<String, Object> getCustomContext() {
        return customContext;
    }
    
    public void setCustomContext(Map<String, Object> customContext) {
        this.customContext = customContext;
    }
    
    public void addCustomContext(String key, Object value) {
        this.customContext.put(key, value);
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ErrorEvent that = (ErrorEvent) o;
        
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (applicationName != null ? !applicationName.equals(that.applicationName) : that.applicationName != null) return false;
        if (environment != null ? !environment.equals(that.environment) : that.environment != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;
        return correlationId != null ? correlationId.equals(that.correlationId) : that.correlationId == null;
    }
    
    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (applicationName != null ? applicationName.hashCode() : 0);
        result = 31 * result + (environment != null ? environment.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (correlationId != null ? correlationId.hashCode() : 0);
        return result;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ErrorEvent errorEvent = new ErrorEvent();
        
        public Builder applicationName(String applicationName) {
            errorEvent.applicationName = applicationName;
            return this;
        }
        
        public Builder environment(String environment) {
            errorEvent.environment = environment;
            return this;
        }
        
        public Builder exception(Throwable exception) {
            errorEvent.exception = exception;
            return this;
        }
        
        public Builder message(String message) {
            errorEvent.message = message;
            return this;
        }
        
        public Builder severity(ErrorSeverity severity) {
            errorEvent.severity = severity;
            return this;
        }
        
        public Builder requestContext(RequestContext requestContext) {
            errorEvent.requestContext = requestContext;
            return this;
        }
        
        public Builder correlationId(String correlationId) {
            errorEvent.correlationId = correlationId;
            return this;
        }
        
        public Builder userId(String userId) {
            errorEvent.userId = userId;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            errorEvent.sessionId = sessionId;
            return this;
        }
        
        public Builder customContext(Map<String, Object> customContext) {
            errorEvent.customContext = customContext;
            return this;
        }
        
        public Builder addCustomContext(String key, Object value) {
            errorEvent.customContext.put(key, value);
            return this;
        }
        
        public ErrorEvent build() {
            return errorEvent;
        }
    }
}