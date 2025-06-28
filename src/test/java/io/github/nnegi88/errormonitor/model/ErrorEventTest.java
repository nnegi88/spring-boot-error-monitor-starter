package io.github.nnegi88.errormonitor.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorEventTest {

    private ErrorEvent errorEvent;
    private LocalDateTime timestamp;
    
    @BeforeEach
    void setUp() {
        timestamp = LocalDateTime.now();
        errorEvent = new ErrorEvent();
        errorEvent.setTimestamp(timestamp);
        errorEvent.setApplicationName("test-app");
        errorEvent.setEnvironment("test");
    }
    
    @Test
    void testBuilderPattern() {
        Exception exception = new RuntimeException("Test error");
        RequestContext requestContext = new RequestContext();
        requestContext.setUrl("/test");
        
        ErrorEvent event = ErrorEvent.builder()
            .applicationName("test-app")
            .environment("production")
            .exception(exception)
            .message("Custom error message")
            .severity(ErrorSeverity.HIGH)
            .requestContext(requestContext)
            .correlationId("12345")
            .build();
            
        assertThat(event.getApplicationName()).isEqualTo("test-app");
        assertThat(event.getEnvironment()).isEqualTo("production");
        assertThat(event.getException()).isEqualTo(exception);
        assertThat(event.getMessage()).isEqualTo("Custom error message");
        assertThat(event.getSeverity()).isEqualTo(ErrorSeverity.HIGH);
        assertThat(event.getRequestContext()).isEqualTo(requestContext);
        assertThat(event.getCorrelationId()).isEqualTo("12345");
        assertThat(event.getTimestamp()).isNotNull();
    }
    
    @Test
    void testCustomContext() {
        Map<String, Object> customContext = new HashMap<>();
        customContext.put("userId", "user123");
        customContext.put("orderId", 456);
        
        errorEvent.setCustomContext(customContext);
        
        assertThat(errorEvent.getCustomContext()).containsExactlyEntriesOf(customContext);
    }
    
    @Test
    void testDefaultTimestamp() {
        ErrorEvent event = new ErrorEvent();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }
    
    @Test
    void testErrorSeverityEnum() {
        assertThat(ErrorSeverity.values()).containsExactly(
            ErrorSeverity.LOW,
            ErrorSeverity.MEDIUM,
            ErrorSeverity.HIGH,
            ErrorSeverity.CRITICAL,
            ErrorSeverity.ERROR,
            ErrorSeverity.WARNING,
            ErrorSeverity.INFO
        );
    }
    
    @Test
    void testEqualsAndHashCode() {
        String sameId = "test-id-123";
        
        ErrorEvent event1 = new ErrorEvent();
        event1.setId(sameId);
        event1.setApplicationName("app1");
        event1.setTimestamp(timestamp);
        
        ErrorEvent event2 = new ErrorEvent();
        event2.setId(sameId);
        event2.setApplicationName("app1");
        event2.setTimestamp(timestamp);
        
        ErrorEvent event3 = new ErrorEvent();
        event3.setId("different-id");
        event3.setApplicationName("app1");
        event3.setTimestamp(timestamp);
        
        assertThat(event1).isEqualTo(event2);
        assertThat(event1).isNotEqualTo(event3);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }
}