package io.github.nnegi88.errormonitor.notification.template;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.RequestContext;
import io.github.nnegi88.errormonitor.notification.slack.SlackMessage;
import io.github.nnegi88.errormonitor.util.StackTraceFormatter;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DefaultSlackMessageTemplate implements SlackMessageTemplate {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final ErrorMonitorProperties properties;
    private final String applicationName;
    private final String environment;
    
    public DefaultSlackMessageTemplate(ErrorMonitorProperties properties, String applicationName, String environment) {
        this.properties = properties;
        this.applicationName = applicationName;
        this.environment = environment;
    }
    
    @Override
    public SlackMessage buildMessage(ErrorEvent event) {
        SlackMessage.SlackMessageBuilder builder = SlackMessage.builder()
            .text(buildFallbackText(event))
            .channel(properties.getNotification().getSlack().getChannel())
            .username(properties.getNotification().getSlack().getUsername())
            .iconEmoji(properties.getNotification().getSlack().getIconEmoji());
        
        // Add header block
        builder.addBlock(SlackMessage.Block.header("Application Error Alert"));
        
        // Add main fields
        List<SlackMessage.Field> fields = new ArrayList<>();
        fields.add(SlackMessage.Field.markdown("*Application:*\\n" + applicationName));
        fields.add(SlackMessage.Field.markdown("*Environment:*\\n" + environment));
        
        if (event.getException() != null) {
            fields.add(SlackMessage.Field.markdown("*Error Type:*\\n" + event.getException().getClass().getSimpleName()));
        }
        
        fields.add(SlackMessage.Field.markdown("*Timestamp:*\\n" + event.getTimestamp().format(DATE_FORMATTER)));
        
        if (event.getSeverity() != null) {
            fields.add(SlackMessage.Field.markdown("*Severity:*\\n" + event.getSeverity()));
        }
        
        if (event.getCorrelationId() != null) {
            fields.add(SlackMessage.Field.markdown("*Correlation ID:*\\n" + event.getCorrelationId()));
        }
        
        builder.addBlock(SlackMessage.Block.section(null, fields));
        
        // Add error message
        if (event.getMessage() != null) {
            SlackMessage.Text messageText = SlackMessage.Text.markdown("*Error Message:*\\n" + event.getMessage());
            builder.addBlock(SlackMessage.Block.section(messageText, null));
        }
        
        // Add request context if available
        if (properties.getContext().isIncludeRequestDetails() && event.getRequestContext() != null) {
            SlackMessage.Text requestHeader = SlackMessage.Text.markdown("*Request Details*");
            builder.addBlock(SlackMessage.Block.section(requestHeader, null));
            builder.addBlock(buildRequestContextBlock(event.getRequestContext()));
        }
        
        // Add stack trace if enabled
        if (properties.getContext().isIncludeStackTrace() && event.getException() != null) {
            String stackTrace = StackTraceFormatter.format(
                event.getException(), 
                properties.getContext().getMaxStackTraceLines()
            );
            SlackMessage.Text stackTraceText = SlackMessage.Text.markdown("*Stack Trace:*\\n```" + stackTrace + "```");
            builder.addBlock(SlackMessage.Block.section(stackTraceText, null));
        }
        
        // Add custom context if available
        if (!event.getCustomContext().isEmpty()) {
            SlackMessage.Text contextHeader = SlackMessage.Text.markdown("*Additional Context*");
            builder.addBlock(SlackMessage.Block.section(contextHeader, null));
            
            List<SlackMessage.Field> contextFields = new ArrayList<>();
            event.getCustomContext().forEach((key, value) -> {
                contextFields.add(SlackMessage.Field.markdown("*" + key + ":*\\n" + String.valueOf(value)));
            });
            builder.addBlock(SlackMessage.Block.section(null, contextFields));
        }
        
        return builder.build();
    }
    
    private String buildFallbackText(ErrorEvent event) {
        return String.format("Application Error Detected in %s - %s", 
            applicationName, 
            event.getMessage() != null ? event.getMessage() : "Unknown Error");
    }
    
    private SlackMessage.Block buildRequestContextBlock(RequestContext context) {
        List<SlackMessage.Field> fields = new ArrayList<>();
        
        if (context.getUrl() != null) {
            fields.add(SlackMessage.Field.markdown("*Request URL:*\\n" + context.getUrl()));
        }
        
        if (context.getHttpMethod() != null) {
            fields.add(SlackMessage.Field.markdown("*HTTP Method:*\\n" + context.getHttpMethod()));
        }
        
        if (context.getClientIp() != null) {
            fields.add(SlackMessage.Field.markdown("*Client IP:*\\n" + context.getClientIp()));
        }
        
        if (context.getUserAgent() != null) {
            fields.add(SlackMessage.Field.markdown("*User Agent:*\\n" + context.getUserAgent()));
        }
        
        return SlackMessage.Block.section(null, fields);
    }
}