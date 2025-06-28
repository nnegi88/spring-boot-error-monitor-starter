package io.github.nnegi88.errormonitor.health;

import io.github.nnegi88.errormonitor.config.ErrorMonitorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationHealthIndicatorTest {
    
    @Mock
    private ErrorMonitorProperties properties;
    
    @Mock
    private WebClient.Builder webClientBuilder;
    
    private NotificationHealthIndicator healthIndicator;
    
    @BeforeEach
    void setUp() {
        healthIndicator = new NotificationHealthIndicator(properties, webClientBuilder);
    }
    
    @Test
    void testHealth_DisabledMonitor() {
        // Given
        when(properties.isEnabled()).thenReturn(false);
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("status", "notifications disabled");
    }
    
    @Test
    void testHealth_NullNotificationConfig() {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getNotification()).thenReturn(null);
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("status", "notifications disabled");
    }
    
    @Test
    void testHealth_SlackWebhookConfigured() {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        
        ErrorMonitorProperties.NotificationProperties notificationProps = 
                new ErrorMonitorProperties.NotificationProperties();
        notificationProps.setPlatform("slack");
        
        ErrorMonitorProperties.SlackProperties slackProps = new ErrorMonitorProperties.SlackProperties();
        slackProps.setWebhookUrl("https://hooks.slack.com/services/test");
        notificationProps.setSlack(slackProps);
        
        when(properties.getNotification()).thenReturn(notificationProps);
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        // The health check will attempt to connect, so status depends on network
        assertThat(health.getStatus()).isIn(Status.UP, Status.DOWN);
        assertThat(health.getDetails()).containsKey("slack");
    }
    
    @Test
    void testHealth_TeamsWebhookConfigured() {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        
        ErrorMonitorProperties.NotificationProperties notificationProps = 
                new ErrorMonitorProperties.NotificationProperties();
        notificationProps.setPlatform("teams");
        
        ErrorMonitorProperties.TeamsProperties teamsProps = new ErrorMonitorProperties.TeamsProperties();
        teamsProps.setWebhookUrl("https://outlook.office.com/webhook/test");
        notificationProps.setTeams(teamsProps);
        
        when(properties.getNotification()).thenReturn(notificationProps);
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isIn(Status.UP, Status.DOWN);
        assertThat(health.getDetails()).containsKey("teams");
    }
    
    @Test
    void testHealth_BothPlatformsConfigured() {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        
        ErrorMonitorProperties.NotificationProperties notificationProps = 
                new ErrorMonitorProperties.NotificationProperties();
        notificationProps.setPlatform("both");
        
        ErrorMonitorProperties.SlackProperties slackProps = new ErrorMonitorProperties.SlackProperties();
        slackProps.setWebhookUrl("https://hooks.slack.com/services/test");
        notificationProps.setSlack(slackProps);
        
        ErrorMonitorProperties.TeamsProperties teamsProps = new ErrorMonitorProperties.TeamsProperties();
        teamsProps.setWebhookUrl("https://outlook.office.com/webhook/test");
        notificationProps.setTeams(teamsProps);
        
        when(properties.getNotification()).thenReturn(notificationProps);
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isIn(Status.UP, Status.DOWN);
        assertThat(health.getDetails()).containsKey("slack");
        assertThat(health.getDetails()).containsKey("teams");
    }
    
    @Test
    void testRecordSuccessfulNotification() {
        // When
        healthIndicator.recordSuccessfulNotification("slack");
        
        // Then - method should complete without error
        assertThat(healthIndicator).isNotNull();
    }
    
    @Test
    void testHealth_SlackNoWebhookUrl() {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        
        ErrorMonitorProperties.NotificationProperties notificationProps = 
                new ErrorMonitorProperties.NotificationProperties();
        notificationProps.setPlatform("slack");
        
        ErrorMonitorProperties.SlackProperties slackProps = new ErrorMonitorProperties.SlackProperties();
        // No webhook URL set
        notificationProps.setSlack(slackProps);
        
        when(properties.getNotification()).thenReturn(notificationProps);
        
        // When
        Health health = healthIndicator.health();
        
        // Then - Should be up because no webhook is configured to check
        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }
    
    @Test
    void testHealth_InvalidWebhookUrl() {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        
        ErrorMonitorProperties.NotificationProperties notificationProps = 
                new ErrorMonitorProperties.NotificationProperties();
        notificationProps.setPlatform("slack");
        
        ErrorMonitorProperties.SlackProperties slackProps = new ErrorMonitorProperties.SlackProperties();
        slackProps.setWebhookUrl("invalid-url");
        notificationProps.setSlack(slackProps);
        
        when(properties.getNotification()).thenReturn(notificationProps);
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("slack");
        
        @SuppressWarnings("unchecked")
        var slackDetails = (java.util.Map<String, Object>) health.getDetails().get("slack");
        assertThat(slackDetails).containsEntry("healthy", false);
    }
}