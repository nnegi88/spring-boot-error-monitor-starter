package io.github.nnegi88.errormonitor.notification.template;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.RequestContext;
import io.github.nnegi88.errormonitor.notification.teams.TeamsMessage;
import io.github.nnegi88.errormonitor.util.StackTraceFormatter;

import java.time.format.DateTimeFormatter;

public class DefaultTeamsMessageTemplate implements TeamsMessageTemplate {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final ErrorMonitorProperties properties;
    private final String applicationName;
    private final String environment;
    
    public DefaultTeamsMessageTemplate(ErrorMonitorProperties properties, String applicationName, String environment) {
        this.properties = properties;
        this.applicationName = applicationName;
        this.environment = environment;
    }
    
    @Override
    public TeamsMessage buildMessage(ErrorEvent event) {
        TeamsMessage.TeamsMessageBuilder builder = TeamsMessage.builder()
            .title(properties.getNotification().getTeams().getTitle())
            .themeColor(properties.getNotification().getTeams().getThemeColor());
        
        // Create main section
        TeamsMessage.Section mainSection = new TeamsMessage.Section();
        mainSection.setActivityTitle("Application Error Detected");
        mainSection.setActivitySubtitle(applicationName + " - " + environment);
        
        // Add facts
        mainSection.setFacts(new java.util.ArrayList<>());
        
        if (event.getException() != null) {
            addFact(mainSection, "Error Type", event.getException().getClass().getSimpleName());
        }
        
        addFact(mainSection, "Timestamp", event.getTimestamp().format(DATE_FORMATTER));
        
        if (event.getSeverity() != null) {
            addFact(mainSection, "Severity", event.getSeverity().toString());
        }
        
        if (event.getMessage() != null) {
            addFact(mainSection, "Error Message", event.getMessage());
        }
        
        if (event.getCorrelationId() != null) {
            addFact(mainSection, "Correlation ID", event.getCorrelationId());
        }
        
        // Add request context if available
        if (properties.getContext().isIncludeRequestDetails() && event.getRequestContext() != null) {
            addRequestContextFacts(mainSection, event.getRequestContext());
        }
        
        // Add custom context
        if (!event.getCustomContext().isEmpty()) {
            event.getCustomContext().forEach((key, value) -> {
                addFact(mainSection, key, value != null ? String.valueOf(value) : null);
            });
        }
        
        // Add stack trace if enabled
        if (properties.getContext().isIncludeStackTrace() && event.getException() != null) {
            String stackTrace = StackTraceFormatter.format(
                event.getException(), 
                properties.getContext().getMaxStackTraceLines()
            );
            mainSection.setText("**Stack Trace:**\\n```\\n" + stackTrace + "\\n```");
        }
        
        builder.addSection(mainSection);
        
        // Add action button for logs if configured
        String logsUrl = properties.getNotification().getTeams().getTitle(); // Could be extended to include logs URL
        if (logsUrl != null && logsUrl.startsWith("http")) {
            builder.addAction(TeamsMessage.Action.openUri("View Logs", logsUrl));
        }
        
        return builder.build();
    }
    
    private void addFact(TeamsMessage.Section section, String name, String value) {
        TeamsMessage.Fact fact = new TeamsMessage.Fact();
        fact.setName(name);
        fact.setValue(value != null ? value : "N/A");
        section.getFacts().add(fact);
    }
    
    private void addRequestContextFacts(TeamsMessage.Section section, RequestContext context) {
        if (context.getUrl() != null) {
            addFact(section, "Request URL", context.getUrl());
        }
        
        if (context.getHttpMethod() != null) {
            addFact(section, "HTTP Method", context.getHttpMethod());
        }
        
        if (context.getClientIp() != null) {
            addFact(section, "Client IP", context.getClientIp());
        }
        
        if (context.getUserAgent() != null) {
            String userAgent = context.getUserAgent();
            if (userAgent.length() > 50) {
                userAgent = userAgent.substring(0, 50) + "...";
            }
            addFact(section, "User Agent", userAgent);
        }
        
        if (context.getStatusCode() > 0) {
            addFact(section, "Status Code", String.valueOf(context.getStatusCode()));
        }
    }
}