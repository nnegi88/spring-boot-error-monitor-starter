package io.github.nnegi88.errormonitor.notification.teams;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import io.github.nnegi88.errormonitor.notification.NotificationPlatform;
import io.github.nnegi88.errormonitor.notification.template.TeamsMessageTemplate;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamsClientTest {

    private MockWebServer mockWebServer;
    private TeamsClient teamsClient;
    private ErrorMonitorProperties properties;
    
    @Mock
    private TeamsMessageTemplate messageTemplate;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        properties = new ErrorMonitorProperties();
        properties.setEnabled(true);
        
        ErrorMonitorProperties.NotificationProperties notificationProperties = new ErrorMonitorProperties.NotificationProperties();
        notificationProperties.setPlatform("teams");
        
        ErrorMonitorProperties.TeamsProperties teamsProperties = new ErrorMonitorProperties.TeamsProperties();
        teamsProperties.setWebhookUrl(mockWebServer.url("/webhook").toString());
        teamsProperties.setTitle("Test Alert");
        teamsProperties.setThemeColor("FF0000");
        
        notificationProperties.setTeams(teamsProperties);
        properties.setNotification(notificationProperties);
        
        WebClient.Builder webClientBuilder = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString());
            
        teamsClient = new TeamsClient(webClientBuilder, properties, messageTemplate);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void testSendNotificationSuccess() throws InterruptedException {
        // Given
        ErrorEvent errorEvent = createErrorEvent();
        TeamsMessage teamsMessage = TeamsMessage.builder()
            .title("Error Alert")
            .themeColor("FF0000")
            .build();
            
        when(messageTemplate.buildMessage(any(ErrorEvent.class))).thenReturn(teamsMessage);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("1"));
            
        // When
        teamsClient.sendNotification(errorEvent);
        
        // Allow async processing
        Thread.sleep(500);
            
        // Then
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).contains("/webhook");
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
        
        String body = request.getBody().readUtf8();
        assertThat(body).contains("\"@type\":\"MessageCard\"");
        assertThat(body).contains("\"summary\":\"Error Alert\"");
        assertThat(body).contains("\"themeColor\":\"FF0000\"");
    }
    
    @Test
    void testSendNotificationWithSections() throws InterruptedException {
        // Given
        ErrorEvent errorEvent = createErrorEvent();
        
        TeamsMessage.Section section = new TeamsMessage.Section();
        section.setActivityTitle("Error Details");
        section.setActivitySubtitle("Application Error");
        
        TeamsMessage.Fact fact1 = new TeamsMessage.Fact();
        fact1.setName("Error Type");
        fact1.setValue("NullPointerException");
        
        TeamsMessage.Fact fact2 = new TeamsMessage.Fact();
        fact2.setName("Timestamp");
        fact2.setValue("2023-06-26 10:30:00");
        
        section.setFacts(java.util.Arrays.asList(fact1, fact2));
        section.setText("Stack trace details here");
        
        TeamsMessage teamsMessage = TeamsMessage.builder()
            .title("Error Alert")
            .themeColor("FF0000")
            .addSection(section)
            .build();
            
        when(messageTemplate.buildMessage(any(ErrorEvent.class))).thenReturn(teamsMessage);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("1"));
            
        // When
        teamsClient.sendNotification(errorEvent);
        
        // Allow async processing
        Thread.sleep(500);
            
        // Then
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        String body = request.getBody().readUtf8();
        assertThat(body).contains("\"activityTitle\":\"Error Details\"");
        assertThat(body).contains("\"activitySubtitle\":\"Application Error\"");
        assertThat(body).contains("\"name\":\"Error Type\"");
        assertThat(body).contains("\"value\":\"NullPointerException\"");
    }
    
    @Test
    void testSendNotificationWithActions() throws InterruptedException {
        // Given
        ErrorEvent errorEvent = createErrorEvent();
        
        TeamsMessage.Action action = TeamsMessage.Action.openUri("View Logs", "https://logs.example.com");
        
        TeamsMessage teamsMessage = TeamsMessage.builder()
            .title("Error Alert")
            .themeColor("FF0000")
            .addAction(action)
            .build();
            
        when(messageTemplate.buildMessage(any(ErrorEvent.class))).thenReturn(teamsMessage);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("1"));
            
        // When
        teamsClient.sendNotification(errorEvent);
        
        // Allow async processing
        Thread.sleep(500);
            
        // Then
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        String body = request.getBody().readUtf8();
        assertThat(body).contains("\"@type\":\"OpenUri\"");
        assertThat(body).contains("\"name\":\"View Logs\"");
        assertThat(body).contains("\"uri\":\"https://logs.example.com\"");
    }
    
    @Test
    void testSendNotificationWithRetry() throws InterruptedException {
        // Given
        ErrorEvent errorEvent = createErrorEvent();
        TeamsMessage teamsMessage = TeamsMessage.builder()
            .title("Error Alert")
            .build();
            
        when(messageTemplate.buildMessage(any(ErrorEvent.class))).thenReturn(teamsMessage);
        
        // First attempt fails
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"));
            
        // Second attempt succeeds
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("1"));
            
        // When
        teamsClient.sendNotification(errorEvent);
        
        // Allow async processing and retry
        Thread.sleep(2500);
            
        // Then - Verify two requests were made
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }
    
    @Test
    void testSendNotificationFailureAfterRetries() throws InterruptedException {
        // Given
        ErrorEvent errorEvent = createErrorEvent();
        TeamsMessage teamsMessage = TeamsMessage.builder()
            .title("Error Alert")
            .build();
            
        when(messageTemplate.buildMessage(any(ErrorEvent.class))).thenReturn(teamsMessage);
        
        // All attempts fail - The retry behavior depends on the WebClient configuration
        // Enqueue enough responses to handle all retry attempts
        for (int i = 0; i < 5; i++) {
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));
        }
        
        // When
        teamsClient.sendNotification(errorEvent);
        
        // Allow async processing and retries with exponential backoff
        Thread.sleep(5000);
            
        // Then - Verify at least initial request + some retries were made
        // The exact count can vary based on the retry configuration and timing
        int requestCount = mockWebServer.getRequestCount();
        assertThat(requestCount).isGreaterThanOrEqualTo(2).isLessThanOrEqualTo(4);
    }
    
    @Test
    void testIsEnabled() {
        assertThat(teamsClient.isEnabled()).isTrue();
        
        // Disable and test
        properties.setEnabled(false);
        assertThat(teamsClient.isEnabled()).isFalse();
    }
    
    @Test
    void testGetPlatform() {
        assertThat(teamsClient.getPlatform()).isEqualTo(NotificationPlatform.TEAMS);
    }
    
    @Test
    void testInvalidWebhookUrl() throws InterruptedException {
        // Given
        properties.getNotification().getTeams().setWebhookUrl(null);
        ErrorEvent errorEvent = createErrorEvent();
        
        // When
        teamsClient.sendNotification(errorEvent);
        
        // Allow async processing
        Thread.sleep(500);
        
        // Then - No request should be made
        assertThat(mockWebServer.getRequestCount()).isEqualTo(0);
    }
    
    @Test
    void testMessageBuilder() {
        TeamsMessage message = TeamsMessage.builder()
            .title("Test Title")
            .themeColor("00FF00")
            // Summary is set via title method
            .build();
            
        assertThat(message.getType()).isEqualTo("MessageCard");
        assertThat(message.getContext()).isEqualTo("https://schema.org/extensions");
        assertThat(message.getSummary()).isEqualTo("Test Title");
        assertThat(message.getThemeColor()).isEqualTo("00FF00");
        // Note: Summary is set to the same value as title by the builder
    }
    
    private ErrorEvent createErrorEvent() {
        ErrorEvent event = new ErrorEvent();
        event.setApplicationName("test-app");
        event.setEnvironment("test");
        event.setException(new RuntimeException("Test error"));
        event.setSeverity(ErrorSeverity.HIGH);
        return event;
    }
}