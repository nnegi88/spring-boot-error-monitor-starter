package io.github.nnegi88.errormonitor.notification.template;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import io.github.nnegi88.errormonitor.model.RequestContext;
import io.github.nnegi88.errormonitor.notification.teams.TeamsMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultTeamsMessageTemplateTest {

    private DefaultTeamsMessageTemplate template;
    private ErrorMonitorProperties properties;
    
    @BeforeEach
    void setUp() {
        properties = new ErrorMonitorProperties();
        properties.setNotification(new ErrorMonitorProperties.NotificationProperties());
        properties.getNotification().setTeams(new ErrorMonitorProperties.TeamsProperties());
        properties.getNotification().getTeams().setTitle("Application Error Alert");
        properties.getNotification().getTeams().setThemeColor("FF0000");
        
        properties.setContext(new ErrorMonitorProperties.ContextProperties());
        properties.getContext().setIncludeRequestDetails(true);
        properties.getContext().setIncludeStackTrace(true);
        properties.getContext().setMaxStackTraceLines(10);
        
        template = new DefaultTeamsMessageTemplate(properties, "test-app", "production");
    }
    
    @Test
    void testBuildBasicMessage() {
        ErrorEvent event = createBasicErrorEvent();
        
        TeamsMessage message = template.buildMessage(event);
        
        assertThat(message.getSummary()).isEqualTo("Application Error Alert");
        assertThat(message.getThemeColor()).isEqualTo("FF0000");
        assertThat(message.getSections()).isNotEmpty();
        
        TeamsMessage.Section mainSection = message.getSections().get(0);
        assertThat(mainSection.getActivityTitle()).isEqualTo("Application Error Detected");
        assertThat(mainSection.getActivitySubtitle()).isEqualTo("test-app - production");
        assertThat(mainSection.getFacts()).isNotEmpty();
    }
    
    @Test
    void testMessageWithRequestContext() {
        ErrorEvent event = createBasicErrorEvent();
        RequestContext requestContext = new RequestContext();
        requestContext.setUrl("/api/users/123");
        requestContext.setHttpMethod("GET");
        requestContext.setClientIp("192.168.1.100");
        requestContext.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        requestContext.setStatusCode(500);
        event.setRequestContext(requestContext);
        
        TeamsMessage message = template.buildMessage(event);
        
        TeamsMessage.Section mainSection = message.getSections().get(0);
        
        // Verify request context facts
        boolean hasUrl = mainSection.getFacts().stream()
            .anyMatch(fact -> "Request URL".equals(fact.getName()) && 
                             "/api/users/123".equals(fact.getValue()));
        boolean hasMethod = mainSection.getFacts().stream()
            .anyMatch(fact -> "HTTP Method".equals(fact.getName()) && 
                             "GET".equals(fact.getValue()));
        boolean hasIp = mainSection.getFacts().stream()
            .anyMatch(fact -> "Client IP".equals(fact.getName()) && 
                             "192.168.1.100".equals(fact.getValue()));
        boolean hasStatusCode = mainSection.getFacts().stream()
            .anyMatch(fact -> "Status Code".equals(fact.getName()) && 
                             "500".equals(fact.getValue()));
        boolean hasUserAgent = mainSection.getFacts().stream()
            .anyMatch(fact -> "User Agent".equals(fact.getName()) && 
                             fact.getValue().contains("Mozilla"));
                             
        assertThat(hasUrl).isTrue();
        assertThat(hasMethod).isTrue();
        assertThat(hasIp).isTrue();
        assertThat(hasStatusCode).isTrue();
        assertThat(hasUserAgent).isTrue();
    }
    
    @Test
    void testMessageWithStackTrace() {
        Exception exception = new RuntimeException("Test error");
        exception.setStackTrace(new StackTraceElement[]{
            new StackTraceElement("com.example.Service", "processData", "Service.java", 42),
            new StackTraceElement("com.example.Controller", "handleRequest", "Controller.java", 15)
        });
        
        ErrorEvent event = createBasicErrorEvent();
        event.setException(exception);
        
        TeamsMessage message = template.buildMessage(event);
        
        TeamsMessage.Section mainSection = message.getSections().get(0);
        assertThat(mainSection.getText()).isNotNull();
        assertThat(mainSection.getText()).contains("Stack Trace:");
        assertThat(mainSection.getText()).contains("RuntimeException: Test error");
        assertThat(mainSection.getText()).contains("com.example.Service.processData");
    }
    
    @Test
    void testMessageWithCustomContext() {
        ErrorEvent event = createBasicErrorEvent();
        Map<String, Object> customContext = new HashMap<>();
        customContext.put("userId", "user123");
        customContext.put("orderId", 456789);
        customContext.put("action", "checkout");
        event.setCustomContext(customContext);
        
        TeamsMessage message = template.buildMessage(event);
        
        TeamsMessage.Section mainSection = message.getSections().get(0);
        
        // Verify custom context facts
        boolean hasUserId = mainSection.getFacts().stream()
            .anyMatch(fact -> "userId".equals(fact.getName()) && 
                             "user123".equals(fact.getValue()));
        boolean hasOrderId = mainSection.getFacts().stream()
            .anyMatch(fact -> "orderId".equals(fact.getName()) && 
                             "456789".equals(fact.getValue()));
        boolean hasAction = mainSection.getFacts().stream()
            .anyMatch(fact -> "action".equals(fact.getName()) && 
                             "checkout".equals(fact.getValue()));
                             
        assertThat(hasUserId).isTrue();
        assertThat(hasOrderId).isTrue();
        assertThat(hasAction).isTrue();
    }
    
    @Test
    void testMessageWithoutRequestContext() {
        properties.getContext().setIncludeRequestDetails(false);
        template = new DefaultTeamsMessageTemplate(properties, "test-app", "production");
        
        ErrorEvent event = createBasicErrorEvent();
        RequestContext requestContext = new RequestContext();
        requestContext.setUrl("/api/test");
        event.setRequestContext(requestContext);
        
        TeamsMessage message = template.buildMessage(event);
        
        TeamsMessage.Section mainSection = message.getSections().get(0);
        
        // Should not have request URL fact
        boolean hasRequestUrl = mainSection.getFacts().stream()
            .anyMatch(fact -> "Request URL".equals(fact.getName()));
                    
        assertThat(hasRequestUrl).isFalse();
    }
    
    @Test
    void testMessageWithoutStackTrace() {
        properties.getContext().setIncludeStackTrace(false);
        template = new DefaultTeamsMessageTemplate(properties, "test-app", "production");
        
        ErrorEvent event = createBasicErrorEvent();
        event.setException(new RuntimeException("Test error"));
        
        TeamsMessage message = template.buildMessage(event);
        
        TeamsMessage.Section mainSection = message.getSections().get(0);
        
        // Should not have stack trace in text
        assertThat(mainSection.getText()).isNull();
    }
    
    @Test
    void testMessageWithAllFields() {
        ErrorEvent event = createBasicErrorEvent();
        event.setCorrelationId("abc123-def456");
        event.setSeverity(ErrorSeverity.CRITICAL);
        event.setMessage("Critical database connection failure");
        
        TeamsMessage message = template.buildMessage(event);
        
        TeamsMessage.Section mainSection = message.getSections().get(0);
        
        // Verify all basic facts are present
        boolean hasErrorType = mainSection.getFacts().stream()
            .anyMatch(fact -> "Error Type".equals(fact.getName()) && 
                             "RuntimeException".equals(fact.getValue()));
        boolean hasTimestamp = mainSection.getFacts().stream()
            .anyMatch(fact -> "Timestamp".equals(fact.getName()));
        boolean hasSeverity = mainSection.getFacts().stream()
            .anyMatch(fact -> "Severity".equals(fact.getName()) && 
                             "CRITICAL".equals(fact.getValue()));
        boolean hasMessage = mainSection.getFacts().stream()
            .anyMatch(fact -> "Error Message".equals(fact.getName()) && 
                             "Critical database connection failure".equals(fact.getValue()));
        boolean hasCorrelationId = mainSection.getFacts().stream()
            .anyMatch(fact -> "Correlation ID".equals(fact.getName()) && 
                             "abc123-def456".equals(fact.getValue()));
                             
        assertThat(hasErrorType).isTrue();
        assertThat(hasTimestamp).isTrue();
        assertThat(hasSeverity).isTrue();
        assertThat(hasMessage).isTrue();
        assertThat(hasCorrelationId).isTrue();
    }
    
    @Test
    void testMessageWithLongUserAgent() {
        ErrorEvent event = createBasicErrorEvent();
        RequestContext requestContext = new RequestContext();
        String longUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
        requestContext.setUserAgent(longUserAgent);
        event.setRequestContext(requestContext);
        
        TeamsMessage message = template.buildMessage(event);
        
        TeamsMessage.Section mainSection = message.getSections().get(0);
        
        // Verify user agent is truncated
        TeamsMessage.Fact userAgentFact = mainSection.getFacts().stream()
            .filter(fact -> "User Agent".equals(fact.getName()))
            .findFirst()
            .orElse(null);
            
        assertThat(userAgentFact).isNotNull();
        assertThat(userAgentFact.getValue()).hasSize(53); // 50 + "..."
        assertThat(userAgentFact.getValue()).endsWith("...");
    }
    
    @Test
    void testMessageWithNullValues() {
        ErrorEvent event = new ErrorEvent();
        event.setApplicationName("test-app");
        event.setEnvironment("production");
        event.setTimestamp(LocalDateTime.now());
        // Add a custom context with null value to test N/A handling
        event.getCustomContext().put("Custom Field", null);
        
        TeamsMessage message = template.buildMessage(event);
        
        assertThat(message).isNotNull();
        assertThat(message.getSections()).isNotEmpty();
        
        TeamsMessage.Section mainSection = message.getSections().get(0);
        
        // Verify N/A is used for null values
        boolean hasNAValues = mainSection.getFacts().stream()
            .anyMatch(fact -> "N/A".equals(fact.getValue()));
            
        assertThat(hasNAValues).isTrue();
    }
    
    private ErrorEvent createBasicErrorEvent() {
        ErrorEvent event = new ErrorEvent();
        event.setApplicationName("test-app");
        event.setEnvironment("production");
        event.setTimestamp(LocalDateTime.now());
        event.setException(new RuntimeException("Test error"));
        event.setMessage("An error occurred");
        event.setSeverity(ErrorSeverity.HIGH);
        return event;
    }
}