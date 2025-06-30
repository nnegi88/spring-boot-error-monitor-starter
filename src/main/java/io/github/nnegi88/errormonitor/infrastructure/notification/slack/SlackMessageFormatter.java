package io.github.nnegi88.errormonitor.infrastructure.notification.slack;

import io.github.nnegi88.errormonitor.domain.model.NotificationMessage;
import io.github.nnegi88.errormonitor.domain.port.MessageFormatter;
import io.github.nnegi88.errormonitor.domain.port.NotificationConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Formats notification messages into Slack Block Kit format.
 * Implements single responsibility principle by focusing only on message formatting.
 */
public class SlackMessageFormatter implements MessageFormatter<SlackMessage> {
    
    private static final String SERVICE_NAME = "slack";
    
    @Override
    public SlackMessage formatMessage(NotificationMessage message, NotificationConfig config) {
        List<SlackMessage.Block> blocks = new ArrayList<>();
        
        // Header block with log level
        String headerText = getHeaderText(message.getLevel(), message.getApplicationName());
        blocks.add(SlackMessage.Block.header(headerText));
        
        // Main message section
        String mainText = formatMainText(message);
        blocks.add(SlackMessage.Block.section(mainText));
        
        // Application info fields
        List<SlackMessage.Text> infoFields = createInfoFields(message, config);
        if (!infoFields.isEmpty()) {
            // Slack requires section blocks to have either text or fields, not null text with fields
            SlackMessage.Block fieldsBlock = new SlackMessage.Block();
            fieldsBlock.setType("section");
            fieldsBlock.setFields(infoFields);
            blocks.add(fieldsBlock);
        }
        
        // Add divider before stack trace
        if (message.hasStackTrace()) {
            blocks.add(SlackMessage.Block.divider());
        }
        
        // Stack trace section (if present)
        if (message.hasStackTrace()) {
            String stackTraceText = formatStackTrace(message.getStackTrace());
            blocks.add(SlackMessage.Block.section(stackTraceText));
        }
        
        // MDC context (if present)
        if (!message.getMetadata().isEmpty()) {
            blocks.add(SlackMessage.Block.divider());
            List<SlackMessage.Text> mdcFields = createMdcFields(message.getMetadata());
            blocks.add(SlackMessage.Block.section("*Additional Context:*", mdcFields));
        }
        
        // Build the final message
        String fallbackText = String.format("Log Alert from %s - %s", 
                message.getApplicationName(), message.getContent());
        
        return SlackMessage.builder()
                .text(fallbackText)
                .blocks(blocks)
                .build();
    }
    
    @Override
    public Class<SlackMessage> getMessageType() {
        return SlackMessage.class;
    }
    
    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }
    
    private String getHeaderText(String level, String applicationName) {
        String emoji = getLevelEmoji(level);
        return String.format("%s %s Alert - %s", emoji, level, applicationName);
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
    
    private String formatMainText(NotificationMessage message) {
        StringBuilder text = new StringBuilder();
        text.append("*Message:* ").append(message.getContent());
        
        if (message.getTitle() != null && !message.getTitle().equals(message.getContent())) {
            text.insert(0, "*Title:* " + message.getTitle() + "\n");
        }
        
        return text.toString();
    }
    
    private List<SlackMessage.Text> createInfoFields(NotificationMessage message, NotificationConfig config) {
        List<SlackMessage.Text> fields = new ArrayList<>();
        
        fields.add(SlackMessage.Text.markdown("*Application:*\n" + message.getApplicationName()));
        
        if (message.getEnvironment() != null && !message.getEnvironment().isEmpty()) {
            fields.add(SlackMessage.Text.markdown("*Environment:*\n" + message.getEnvironment()));
        }
        
        fields.add(SlackMessage.Text.markdown("*Level:*\n" + message.getLevel()));
        
        return fields;
    }
    
    private String formatStackTrace(String stackTrace) {
        // Limit stack trace length to avoid message size limits
        String limitedStackTrace = stackTrace.length() > 2000 
                ? stackTrace.substring(0, 2000) + "... (truncated)"
                : stackTrace;
        
        return "*Stack Trace:*\n```\n" + limitedStackTrace + "\n```";
    }
    
    private List<SlackMessage.Text> createMdcFields(Map<String, Object> metadata) {
        List<SlackMessage.Text> fields = new ArrayList<>();
        
        metadata.forEach((key, value) -> {
            if (value != null) {
                String displayKey = formatMdcKey(key);
                String displayValue = String.valueOf(value);
                fields.add(SlackMessage.Text.markdown(String.format("*%s:*\n%s", displayKey, displayValue)));
            }
        });
        
        return fields;
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