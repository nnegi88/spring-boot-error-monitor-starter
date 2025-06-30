package io.github.nnegi88.errormonitor.domain.model;

import java.time.Instant;

/**
 * Domain model representing the result of a notification attempt.
 */
public class NotificationResult {
    private final boolean successful;
    private final String serviceName;
    private final String errorMessage;
    private final Instant timestamp;
    private final int statusCode;

    private NotificationResult(Builder builder) {
        this.successful = builder.successful;
        this.serviceName = builder.serviceName;
        this.errorMessage = builder.errorMessage;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.statusCode = builder.statusCode;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public static NotificationResult success(String serviceName) {
        return builder()
                .successful(true)
                .serviceName(serviceName)
                .statusCode(200)
                .build();
    }

    public static NotificationResult success(String serviceName, int statusCode) {
        return builder()
                .successful(true)
                .serviceName(serviceName)
                .statusCode(statusCode)
                .build();
    }

    public static NotificationResult failure(String serviceName, String errorMessage) {
        return builder()
                .successful(false)
                .serviceName(serviceName)
                .errorMessage(errorMessage)
                .build();
    }

    public static NotificationResult failure(String serviceName, String errorMessage, int statusCode) {
        return builder()
                .successful(false)
                .serviceName(serviceName)
                .errorMessage(errorMessage)
                .statusCode(statusCode)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean successful;
        private String serviceName;
        private String errorMessage;
        private Instant timestamp;
        private int statusCode;

        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public NotificationResult build() {
            return new NotificationResult(this);
        }
    }
}