package io.github.nnegi88.errormonitor.infrastructure.notification.teams;

import io.github.nnegi88.errormonitor.domain.model.NotificationMessage;
import io.github.nnegi88.errormonitor.domain.port.MessageFormatter;
import io.github.nnegi88.errormonitor.domain.port.NotificationConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Formats notification messages into Microsoft Teams Adaptive Cards format.
 * Implements single responsibility principle by focusing only on message formatting.
 */
public class TeamsMessageFormatter implements MessageFormatter<TeamsMessage> {
    
    private static final String SERVICE_NAME = "teams";
    
    @Override
    public TeamsMessage formatMessage(NotificationMessage message, NotificationConfig config) {
        String themeColor = getThemeColor(message.getLevel());
        String title = formatTitle(message);
        String summary = formatSummary(message);
        
        List<TeamsMessage.Section> sections = new ArrayList<>();
        
        // Main message section
        List<TeamsMessage.Fact> mainFacts = createMainFacts(message, config);
        TeamsMessage.Section mainSection = TeamsMessage.Section.create(
                "Log Details",
                message.getContent(),
                mainFacts
        );
        sections.add(mainSection);
        
        // Stack trace section (if present)
        if (message.hasStackTrace()) {
            String stackTrace = formatStackTrace(message.getStackTrace());
            TeamsMessage.Section stackSection = TeamsMessage.Section.create(
                    "Stack Trace",
                    stackTrace,
                    null
            );
            sections.add(stackSection);
        }
        
        // MDC context section (if present)
        if (!message.getMetadata().isEmpty()) {
            List<TeamsMessage.Fact> mdcFacts = createMdcFacts(message.getMetadata());
            TeamsMessage.Section mdcSection = TeamsMessage.Section.create(
                    "Additional Context",
                    null,
                    mdcFacts
            );
            sections.add(mdcSection);
        }
        
        return TeamsMessage.builder()
                .summary(summary)
                .themeColor(themeColor)
                .title(title)
                .text(message.getContent())
                .sections(sections)
                .build();
    }
    
    @Override
    public Class<TeamsMessage> getMessageType() {
        return TeamsMessage.class;
    }
    
    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }
    
    private String getThemeColor(String level) {
        switch (level.toUpperCase()) {
            case "ERROR":
                return "FF0000"; // Red
            case "WARN":
                return "FF8C00"; // Orange
            case "INFO":
                return "0078D4"; // Blue
            case "DEBUG":
                return "6264A7"; // Purple
            default:
                return "808080"; // Gray
        }
    }
    
    private String formatTitle(NotificationMessage message) {
        String emoji = getLevelEmoji(message.getLevel());
        return String.format("%s %s Alert - %s", 
                emoji, message.getLevel(), message.getApplicationName());
    }
    
    private String getLevelEmoji(String level) {
        switch (level.toUpperCase()) {
            case "ERROR":
                return "🚨";
            case "WARN":
                return "⚠️";
            case "INFO":
                return "ℹ️";
            case "DEBUG":
                return "🔍";
            default:
                return "📝";
        }
    }
    
    private String formatSummary(NotificationMessage message) {
        return String.format("Log Alert from %s - %s", 
                message.getApplicationName(), message.getLevel());
    }
    
    private List<TeamsMessage.Fact> createMainFacts(NotificationMessage message, NotificationConfig config) {
        List<TeamsMessage.Fact> facts = new ArrayList<>();
        
        facts.add(new TeamsMessage.Fact("Application", message.getApplicationName()));
        
        if (message.getEnvironment() != null && !message.getEnvironment().isEmpty()) {
            facts.add(new TeamsMessage.Fact("Environment", message.getEnvironment()));
        }
        
        facts.add(new TeamsMessage.Fact("Level", message.getLevel()));
        
        if (message.getTitle() != null && !message.getTitle().equals(message.getContent())) {
            facts.add(new TeamsMessage.Fact("Title", message.getTitle()));
        }
        
        return facts;
    }
    
    private String formatStackTrace(String stackTrace) {
        // Limit stack trace length to avoid message size limits
        String limitedStackTrace = stackTrace.length() > 3000 
                ? stackTrace.substring(0, 3000) + "... (truncated)"
                : stackTrace;
        
        // Format as code block
        return "```\n" + limitedStackTrace + "\n```";
    }
    
    private List<TeamsMessage.Fact> createMdcFacts(Map<String, Object> metadata) {
        List<TeamsMessage.Fact> facts = new ArrayList<>();
        
        metadata.forEach((key, value) -> {
            if (value != null) {
                String displayKey = formatMdcKey(key);
                String displayValue = String.valueOf(value);
                facts.add(new TeamsMessage.Fact(displayKey, displayValue));
            }
        });
        
        return facts;
    }
    
    private String formatMdcKey(String key) {
        // Convert camelCase or snake_case to Title Case
        String result = key.replaceAll("([a-z])([A-Z])", "$1 $2")
                          .replaceAll("_", " ");
        
        // Convert to title case
        StringBuilder titleCase = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : result.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                titleCase.append(c);
            } else if (capitalizeNext) {
                titleCase.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                titleCase.append(Character.toLowerCase(c));
            }
        }
        return titleCase.toString();
    }
}