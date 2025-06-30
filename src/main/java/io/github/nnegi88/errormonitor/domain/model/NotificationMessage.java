package io.github.nnegi88.errormonitor.domain.model;

import java.util.Map;

/**
 * Domain model representing a notification message to be sent to external services.
 * This is a generic representation that can be transformed into service-specific formats.
 */
public class NotificationMessage {
    private final String title;
    private final String content;
    private final String level;
    private final String applicationName;
    private final String environment;
    private final Map<String, Object> metadata;
    private final String stackTrace;

    private NotificationMessage(Builder builder) {
        this.title = builder.title;
        this.content = builder.content;
        this.level = builder.level;
        this.applicationName = builder.applicationName;
        this.environment = builder.environment;
        this.metadata = Map.copyOf(builder.metadata);
        this.stackTrace = builder.stackTrace;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getLevel() {
        return level;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getEnvironment() {
        return environment;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public boolean hasStackTrace() {
        return stackTrace != null && !stackTrace.trim().isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String content;
        private String level;
        private String applicationName;
        private String environment;
        private Map<String, Object> metadata = Map.of();
        private String stackTrace;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder level(String level) {
            this.level = level;
            return this;
        }

        public Builder applicationName(String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? metadata : Map.of();
            return this;
        }

        public Builder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public NotificationMessage build() {
            return new NotificationMessage(this);
        }
    }
}