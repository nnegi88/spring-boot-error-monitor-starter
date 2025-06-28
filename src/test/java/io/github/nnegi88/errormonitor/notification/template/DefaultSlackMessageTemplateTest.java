package io.github.nnegi88.errormonitor.notification.template;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import io.github.nnegi88.errormonitor.model.RequestContext;
import io.github.nnegi88.errormonitor.notification.slack.SlackMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultSlackMessageTemplateTest {

    private DefaultSlackMessageTemplate template;
    private ErrorMonitorProperties properties;
    
    @BeforeEach
    void setUp() {
        properties = new ErrorMonitorProperties();
        properties.setNotification(new ErrorMonitorProperties.NotificationProperties());
        properties.getNotification().setSlack(new ErrorMonitorProperties.SlackProperties());
        properties.getNotification().getSlack().setChannel("#alerts");
        properties.getNotification().getSlack().setUsername("Error Monitor");
        properties.getNotification().getSlack().setIconEmoji(":warning:");
        
        properties.setContext(new ErrorMonitorProperties.ContextProperties());
        properties.getContext().setIncludeRequestDetails(true);
        properties.getContext().setIncludeStackTrace(true);
        properties.getContext().setMaxStackTraceLines(10);
        
        template = new DefaultSlackMessageTemplate(properties, "test-app", "production");
    }
    
    @Test
    void testBuildBasicMessage() {
        ErrorEvent event = createBasicErrorEvent();
        
        SlackMessage message = template.buildMessage(event);
        
        assertThat(message.getText()).contains("Application Error Detected");
        assertThat(message.getChannel()).isEqualTo("#alerts");
        assertThat(message.getUsername()).isEqualTo("Error Monitor");
        assertThat(message.getIconEmoji()).isEqualTo(":warning:");
        assertThat(message.getBlocks()).isNotEmpty();
    }
    
    @Test
    void testMessageWithRequestContext() {
        ErrorEvent event = createBasicErrorEvent();
        RequestContext requestContext = new RequestContext();
        requestContext.setUrl("/api/users/123");
        requestContext.setHttpMethod("GET");
        requestContext.setClientIp("192.168.1.100");
        requestContext.setUserAgent("Mozilla/5.0");
        requestContext.setStatusCode(500);
        event.setRequestContext(requestContext);
        
        SlackMessage message = template.buildMessage(event);
        
        // Find the request details header block
        int requestHeaderIndex = -1;
        for (int i = 0; i < message.getBlocks().size(); i++) {
            SlackMessage.Block block = message.getBlocks().get(i);
            if (block.getText() != null && 
                    block.getText().getText() != null &&
                    block.getText().getText().contains("Request Details")) {
                requestHeaderIndex = i;
                break;
            }
        }
        
        assertThat(requestHeaderIndex).isNotEqualTo(-1);
        assertThat(message.getBlocks().size()).isGreaterThan(requestHeaderIndex + 1);
        
        // Get the block with actual request fields (next block after header)
        SlackMessage.Block requestFieldsBlock = message.getBlocks().get(requestHeaderIndex + 1);
        assertThat(requestFieldsBlock).isNotNull();
        assertThat(requestFieldsBlock.getFields()).isNotEmpty();
        
        // Verify request context fields
        boolean hasUrl = requestFieldsBlock.getFields().stream()
            .anyMatch(field -> field.getText().contains("/api/users/123"));
        boolean hasMethod = requestFieldsBlock.getFields().stream()
            .anyMatch(field -> field.getText().contains("GET"));
        boolean hasIp = requestFieldsBlock.getFields().stream()
            .anyMatch(field -> field.getText().contains("192.168.1.100"));
            
        assertThat(hasUrl).isTrue();
        assertThat(hasMethod).isTrue();
        assertThat(hasIp).isTrue();
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
        
        SlackMessage message = template.buildMessage(event);
        
        // Find stack trace section
        SlackMessage.Block stackTraceBlock = message.getBlocks().stream()
            .filter(block -> block.getText() != null && 
                    block.getText().getText() != null &&
                    block.getText().getText().contains("Stack Trace"))
            .findFirst()
            .orElse(null);
            
        assertThat(stackTraceBlock).isNotNull();
        assertThat(stackTraceBlock.getText().getText()).contains("RuntimeException: Test error");
        assertThat(stackTraceBlock.getText().getText()).contains("com.example.Service.processData");
    }
    
    @Test
    void testMessageWithCustomContext() {
        ErrorEvent event = createBasicErrorEvent();
        Map<String, Object> customContext = new HashMap<>();
        customContext.put("userId", "user123");
        customContext.put("orderId", 456789);
        customContext.put("action", "checkout");
        event.setCustomContext(customContext);
        
        SlackMessage message = template.buildMessage(event);
        
        // Find custom context header block
        int contextHeaderIndex = -1;
        for (int i = 0; i < message.getBlocks().size(); i++) {
            SlackMessage.Block block = message.getBlocks().get(i);
            if (block.getText() != null && 
                    block.getText().getText() != null &&
                    block.getText().getText().contains("Additional Context")) {
                contextHeaderIndex = i;
                break;
            }
        }
        
        assertThat(contextHeaderIndex).isNotEqualTo(-1);
        assertThat(message.getBlocks().size()).isGreaterThan(contextHeaderIndex + 1);
        
        // Get the block with actual context fields (next block after header)
        SlackMessage.Block contextFieldsBlock = message.getBlocks().get(contextHeaderIndex + 1);
        assertThat(contextFieldsBlock).isNotNull();
        assertThat(contextFieldsBlock.getFields()).hasSize(3);
        
        boolean hasUserId = contextFieldsBlock.getFields().stream()
            .anyMatch(field -> field.getText().contains("user123"));
        boolean hasOrderId = contextFieldsBlock.getFields().stream()
            .anyMatch(field -> field.getText().contains("456789"));
        boolean hasAction = contextFieldsBlock.getFields().stream()
            .anyMatch(field -> field.getText().contains("checkout"));
            
        assertThat(hasUserId).isTrue();
        assertThat(hasOrderId).isTrue();
        assertThat(hasAction).isTrue();
    }
    
    @Test
    void testMessageWithoutRequestContext() {
        properties.getContext().setIncludeRequestDetails(false);
        template = new DefaultSlackMessageTemplate(properties, "test-app", "production");
        
        ErrorEvent event = createBasicErrorEvent();
        RequestContext requestContext = new RequestContext();
        requestContext.setUrl("/api/test");
        event.setRequestContext(requestContext);
        
        SlackMessage message = template.buildMessage(event);
        
        // Should not have request details section
        boolean hasRequestSection = message.getBlocks().stream()
            .anyMatch(block -> block.getText() != null && 
                    block.getText().getText() != null &&
                    block.getText().getText().contains("Request Details"));
                    
        assertThat(hasRequestSection).isFalse();
    }
    
    @Test
    void testMessageWithoutStackTrace() {
        properties.getContext().setIncludeStackTrace(false);
        template = new DefaultSlackMessageTemplate(properties, "test-app", "production");
        
        ErrorEvent event = createBasicErrorEvent();
        event.setException(new RuntimeException("Test error"));
        
        SlackMessage message = template.buildMessage(event);
        
        // Should not have stack trace section
        boolean hasStackTraceSection = message.getBlocks().stream()
            .anyMatch(block -> block.getText() != null && 
                    block.getText().getText() != null &&
                    block.getText().getText().contains("Stack Trace"));
                    
        assertThat(hasStackTraceSection).isFalse();
    }
    
    @Test
    void testMessageWithCorrelationId() {
        ErrorEvent event = createBasicErrorEvent();
        event.setCorrelationId("abc123-def456");
        
        SlackMessage message = template.buildMessage(event);
        
        // Find error details section
        SlackMessage.Block errorBlock = message.getBlocks().stream()
            .filter(block -> "section".equals(block.getType()) && block.getFields() != null)
            .findFirst()
            .orElse(null);
            
        assertThat(errorBlock).isNotNull();
        
        boolean hasCorrelationId = errorBlock.getFields().stream()
            .anyMatch(field -> field.getText().contains("abc123-def456"));
            
        assertThat(hasCorrelationId).isTrue();
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