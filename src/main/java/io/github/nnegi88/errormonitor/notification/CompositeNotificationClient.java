package io.github.nnegi88.errormonitor.notification;

import io.github.nnegi88.errormonitor.model.ErrorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CompositeNotificationClient implements NotificationClient {
    
    private static final Logger logger = LoggerFactory.getLogger(CompositeNotificationClient.class);
    
    private final List<NotificationClient> clients = new ArrayList<>();
    
    public void addClient(NotificationClient client) {
        clients.add(client);
    }
    
    @Override
    public void sendNotification(ErrorEvent errorEvent) {
        for (NotificationClient client : clients) {
            try {
                if (client.isEnabled()) {
                    client.sendNotification(errorEvent);
                }
            } catch (Exception e) {
                logger.error("Failed to send notification via client: {}", client.getPlatform(), e);
            }
        }
    }
    
    @Override
    public boolean isEnabled() {
        return clients.stream().anyMatch(NotificationClient::isEnabled);
    }
    
    @Override
    public NotificationPlatform getPlatform() {
        return NotificationPlatform.BOTH;
    }
}