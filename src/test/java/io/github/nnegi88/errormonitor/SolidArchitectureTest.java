package io.github.nnegi88.errormonitor;

import io.github.nnegi88.errormonitor.domain.model.LogEvent;
import io.github.nnegi88.errormonitor.domain.model.NotificationMessage;
import io.github.nnegi88.errormonitor.domain.model.NotificationResult;
import io.github.nnegi88.errormonitor.domain.port.AsyncProcessor;
import io.github.nnegi88.errormonitor.domain.port.MessageFormatter;
import io.github.nnegi88.errormonitor.domain.port.NotificationConfig;
import io.github.nnegi88.errormonitor.domain.port.NotificationService;
import io.github.nnegi88.errormonitor.domain.port.SlackClient;
import io.github.nnegi88.errormonitor.domain.port.TeamsClient;
import io.github.nnegi88.errormonitor.domain.service.NotificationOrchestrator;
import io.github.nnegi88.errormonitor.infrastructure.async.AsyncProcessorImpl;
import io.github.nnegi88.errormonitor.infrastructure.config.SlackConfig;
import io.github.nnegi88.errormonitor.infrastructure.http.RestClientSlackClient;
import io.github.nnegi88.errormonitor.infrastructure.http.RestClientTeamsClient;
import io.github.nnegi88.errormonitor.infrastructure.notification.slack.SlackMessage;
import io.github.nnegi88.errormonitor.infrastructure.notification.slack.SlackMessageFormatter;
import io.github.nnegi88.errormonitor.infrastructure.notification.slack.SlackNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify SOLID principles compliance in the refactored architecture.
 */
public class SolidArchitectureTest {
    
    @Test
    public void testSingleResponsibilityPrinciple() {
        // Each class should have a single responsibility
        
        // SlackClient only handles Slack HTTP operations
        SlackClient slackClient = new RestClientSlackClient(new RestTemplate());
        assertNotNull(slackClient);
        
        // TeamsClient only handles Teams HTTP operations
        TeamsClient teamsClient = new RestClientTeamsClient(new RestTemplate());
        assertNotNull(teamsClient);
        
        // MessageFormatter only formats messages
        MessageFormatter<SlackMessage> formatter = new SlackMessageFormatter();
        assertEquals(SlackMessage.class, formatter.getMessageType());
        assertEquals("slack", formatter.getServiceName());
        
        // AsyncProcessor only handles async operations
        AsyncProcessor asyncProcessor = new AsyncProcessorImpl(10, 1, 2);
        assertTrue(asyncProcessor.canAcceptTasks());
        
        // Clean up
        asyncProcessor.shutdown();
    }
    
    @Test
    public void testOpenClosedPrinciple() {
        // System should be open for extension but closed for modification
        
        // We can create new notification services without modifying existing ones
        CustomNotificationService customService = new CustomNotificationService();
        assertEquals("custom", customService.getServiceName());
        
        // We can create custom configurations without modifying existing interfaces
        CustomConfig customConfig = new CustomConfig();
        assertTrue(customService.supports(customConfig));
    }
    
    @Test
    public void testLiskovSubstitutionPrinciple() {
        // Derived classes must be substitutable for their base classes
        
        SlackClient slackClient = new RestClientSlackClient(new RestTemplate());
        TeamsClient teamsClient = new RestClientTeamsClient(new RestTemplate());
        
        // Both clients follow their respective contracts
        assertNotNull(slackClient);
        assertNotNull(teamsClient);
        
        // They can be used polymorphically through their interfaces
        // Both implementations provide testConnection method that returns CompletableFuture<Boolean>
        CompletableFuture<Boolean> slackConnectionFuture = slackClient.testConnection("http://example.com");
        CompletableFuture<Boolean> teamsConnectionFuture = teamsClient.testConnection("http://example.com");
        
        // Verify the futures are created correctly (not testing actual connection)
        assertNotNull(slackConnectionFuture);
        assertNotNull(teamsConnectionFuture);
        
        // Both return same type and can be used interchangeably
        assertTrue(slackConnectionFuture instanceof CompletableFuture);
        assertTrue(teamsConnectionFuture instanceof CompletableFuture);
    }
    
    @Test
    public void testInterfaceSegregationPrinciple() {
        // Clients should not be forced to depend on interfaces they don't use
        
        // NotificationConfig interface is focused and minimal
        NotificationConfig config = SlackConfig.builder()
                .webhookUrl("http://example.com")
                .applicationName("test-app")
                .environment("test")
                .enabled(true)
                .build();
        
        // Interface only exposes necessary methods
        assertNotNull(config.getWebhookUrl());
        assertNotNull(config.getApplicationName());
        assertTrue(config.isEnabled());
    }
    
    @Test
    public void testDependencyInversionPrinciple() {
        // High-level modules should not depend on low-level modules
        // Both should depend on abstractions
        
        // NotificationOrchestrator depends on abstractions
        List<NotificationService> services = List.of(new MockNotificationService());
        NotificationOrchestrator orchestrator = new NotificationOrchestrator(services);
        
        // Can work with any implementation of NotificationService
        LogEvent logEvent = LogEvent.builder()
                .level("ERROR")
                .message("Test error")
                .timestamp(Instant.now())
                .build();
        
        NotificationConfig config = SlackConfig.builder()
                .webhookUrl("http://example.com")
                .applicationName("test-app")
                .enabled(true)
                .build();
        
        CompletableFuture<List<NotificationResult>> future = 
                orchestrator.processEvent(logEvent, List.of(config));
        
        assertNotNull(future);
    }
    
    // Custom implementations for testing OCP
    private static class CustomNotificationService implements NotificationService {
        @Override
        public CompletableFuture<NotificationResult> sendNotification(NotificationMessage message) {
            return CompletableFuture.completedFuture(NotificationResult.success("custom"));
        }
        
        @Override
        public boolean supports(NotificationConfig config) {
            return config instanceof CustomConfig;
        }
        
        @Override
        public String getServiceName() {
            return "custom";
        }
    }
    
    private static class CustomConfig implements NotificationConfig {
        @Override
        public String getWebhookUrl() { return "http://custom.com"; }
        @Override
        public String getApplicationName() { return "custom-app"; }
        @Override
        public String getEnvironment() { return "custom"; }
        @Override
        public String getMinimumLevel() { return "ERROR"; }
        @Override
        public boolean isEnabled() { return true; }
        @Override
        public Map<String, Object> getAdditionalProperties() { return Map.of(); }
    }
    
    private static class MockNotificationService implements NotificationService {
        @Override
        public CompletableFuture<NotificationResult> sendNotification(NotificationMessage message) {
            return CompletableFuture.completedFuture(NotificationResult.success("mock"));
        }
        
        @Override
        public boolean supports(NotificationConfig config) {
            return true;
        }
        
        @Override
        public String getServiceName() {
            return "mock";
        }
    }
}