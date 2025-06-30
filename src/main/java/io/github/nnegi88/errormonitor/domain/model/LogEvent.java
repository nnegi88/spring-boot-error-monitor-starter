package io.github.nnegi88.errormonitor.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * Domain model representing a log event to be processed for notifications.
 * This is a clean domain object independent of any logging framework specifics.
 */
public class LogEvent {
    private final String level;
    private final String message;
    private final String loggerName;
    private final Instant timestamp;
    private final String threadName;
    private final Throwable throwable;
    private final Map<String, String> mdcProperties;
    private final String formattedMessage;

    private LogEvent(Builder builder) {
        this.level = builder.level;
        this.message = builder.message;
        this.loggerName = builder.loggerName;
        this.timestamp = builder.timestamp;
        this.threadName = builder.threadName;
        this.throwable = builder.throwable;
        this.mdcProperties = Map.copyOf(builder.mdcProperties);
        this.formattedMessage = builder.formattedMessage;
    }

    public String getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getThreadName() {
        return threadName;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public Map<String, String> getMdcProperties() {
        return mdcProperties;
    }

    public String getFormattedMessage() {
        return formattedMessage;
    }

    public boolean hasThrowable() {
        return throwable != null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String level;
        private String message;
        private String loggerName;
        private Instant timestamp;
        private String threadName;
        private Throwable throwable;
        private Map<String, String> mdcProperties = Map.of();
        private String formattedMessage;

        public Builder level(String level) {
            this.level = level;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder loggerName(String loggerName) {
            this.loggerName = loggerName;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder throwable(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public Builder mdcProperties(Map<String, String> mdcProperties) {
            this.mdcProperties = mdcProperties != null ? mdcProperties : Map.of();
            return this;
        }

        public Builder formattedMessage(String formattedMessage) {
            this.formattedMessage = formattedMessage;
            return this;
        }

        public LogEvent build() {
            return new LogEvent(this);
        }
    }
}