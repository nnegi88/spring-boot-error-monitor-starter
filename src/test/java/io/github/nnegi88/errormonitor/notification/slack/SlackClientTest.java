package io.github.nnegi88.errormonitor.notification.slack;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.model.ErrorSeverity;
import io.github.nnegi88.errormonitor.notification.NotificationPlatform;
import io.github.nnegi88.errormonitor.notification.template.SlackMessageTemplate;
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
class SlackClientTest {

    private MockWebServer mockWebServer;
    private SlackClient slackClient;
    private ErrorMonitorProperties properties;
    
    @Mock
    private SlackMessageTemplate messageTemplate;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        properties = new ErrorMonitorProperties();
        properties.setEnabled(true);
        
        ErrorMonitorProperties.NotificationProperties notificationProperties = new ErrorMonitorProperties.NotificationProperties();
        notificationProperties.setPlatform("slack");
        
        ErrorMonitorProperties.SlackProperties slackProperties = new ErrorMonitorProperties.SlackProperties();
        slackProperties.setWebhookUrl(mockWebServer.url("/webhook").toString());
        slackProperties.setChannel("#test-channel");
        slackProperties.setUsername("Test Bot");
        slackProperties.setIconEmoji(":robot:");
        
        notificationProperties.setSlack(slackProperties);
        properties.setNotification(notificationProperties);
        
        WebClient.Builder webClientBuilder = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString());
            
        slackClient = new SlackClient(webClientBuilder, properties, messageTemplate);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void testSendNotificationSuccess() throws InterruptedException {
        // Given
        ErrorEvent errorEvent = createErrorEvent();
        SlackMessage slackMessage = SlackMessage.builder()
            .text("Test message")
            .channel("#alerts")
            .build();
            
        when(messageTemplate.buildMessage(any(ErrorEvent.class))).thenReturn(slackMessage);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("ok"));
            
        // When
        slackClient.sendNotification(errorEvent);
        
        // Allow async processing
        Thread.sleep(500);
            
        // Then
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).contains("/webhook");
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
        
        String body = request.getBody().readUtf8();
        assertThat(body).contains("\"text\":\"Test message\"");
        assertThat(body).contains("\"channel\":\"#alerts\"");
    }
    
    @Test
    void testSendNotificationWithRetry() throws InterruptedException {
        // Given
        ErrorEvent errorEvent = createErrorEvent();
        SlackMessage slackMessage = SlackMessage.builder()
            .text("Test message")
            .build();
            
        when(messageTemplate.buildMessage(any(ErrorEvent.class))).thenReturn(slackMessage);
        
        // First attempt fails
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"));
            
        // Second attempt succeeds
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("ok"));
            
        // When
        slackClient.sendNotification(errorEvent);
        
        // Allow async processing and retry
        Thread.sleep(5000);
            
        // Then - Verify two requests were made
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }
    
    @Test
    void testSendNotificationFailureAfterRetries() throws InterruptedException {
        // Given
        ErrorEvent errorEvent = createErrorEvent();
        SlackMessage slackMessage = SlackMessage.builder()
            .text("Test message")
            .build();
            
        when(messageTemplate.buildMessage(any(ErrorEvent.class))).thenReturn(slackMessage);
        
        // All attempts fail (initial + 3 retries = 4 total)
        for (int i = 0; i < 4; i++) {
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));
        }
        
        // When
        slackClient.sendNotification(errorEvent);
        
        // Allow async processing and retries
        Thread.sleep(10000);
            
        // Then - Verify four requests were made (initial + 3 retries)
        assertThat(mockWebServer.getRequestCount()).isEqualTo(4);
    }
    
    @Test
    void testIsEnabled() {
        assertThat(slackClient.isEnabled()).isTrue();
        
        // Disable and test
        properties.setEnabled(false);
        assertThat(slackClient.isEnabled()).isFalse();
    }
    
    @Test
    void testGetPlatform() {
        assertThat(slackClient.getPlatform()).isEqualTo(NotificationPlatform.SLACK);
    }
    
    @Test
    void testInvalidWebhookUrl() throws InterruptedException {
        // Given
        properties.getNotification().getSlack().setWebhookUrl(null);
        ErrorEvent errorEvent = createErrorEvent();
        
        // When
        slackClient.sendNotification(errorEvent);
        
        // Allow async processing
        Thread.sleep(500);
        
        // Then - No request should be made
        assertThat(mockWebServer.getRequestCount()).isEqualTo(0);
    }
    
    @Test
    void testMessageBuilderWithBlocks() {
        SlackMessage.Block headerBlock = new SlackMessage.Block();
        headerBlock.setType("header");
        SlackMessage.Text headerText = new SlackMessage.Text();
        headerText.setType("plain_text");
        headerText.setText("Error Alert");
        headerBlock.setText(headerText);
        
        SlackMessage message = SlackMessage.builder()
            .text("Fallback text")
            .channel("#alerts")
            .username("Error Bot")
            .iconEmoji(":warning:")
            .addBlock(headerBlock)
            .build();
            
        assertThat(message.getText()).isEqualTo("Fallback text");
        assertThat(message.getChannel()).isEqualTo("#alerts");
        assertThat(message.getUsername()).isEqualTo("Error Bot");
        assertThat(message.getIconEmoji()).isEqualTo(":warning:");
        assertThat(message.getBlocks()).hasSize(1);
        assertThat(message.getBlocks().get(0).getType()).isEqualTo("header");
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