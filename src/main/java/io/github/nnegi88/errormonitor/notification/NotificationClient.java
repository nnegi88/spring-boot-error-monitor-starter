package io.github.nnegi88.errormonitor.notification;

import io.github.nnegi88.errormonitor.model.ErrorEvent;

public interface NotificationClient {
    void sendNotification(ErrorEvent errorEvent);
    boolean isEnabled();
    NotificationPlatform getPlatform();
    
    // Health check methods
    default boolean isHealthy() {
        return true;
    }
    
    default String getLastError() {
        return null;
    }
    
    default String getLastNotificationTime() {
        return null;
    }
}