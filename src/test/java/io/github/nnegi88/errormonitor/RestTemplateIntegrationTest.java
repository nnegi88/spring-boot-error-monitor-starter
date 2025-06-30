package io.github.nnegi88.errormonitor;

import io.github.nnegi88.errormonitor.domain.model.NotificationResult;
import io.github.nnegi88.errormonitor.domain.port.SlackClient;
import io.github.nnegi88.errormonitor.domain.port.TeamsClient;
import io.github.nnegi88.errormonitor.infrastructure.http.RestClientSlackClient;
import io.github.nnegi88.errormonitor.infrastructure.http.RestClientTeamsClient;
import io.github.nnegi88.errormonitor.infrastructure.notification.slack.SlackMessage;
import io.github.nnegi88.errormonitor.infrastructure.notification.teams.TeamsMessage;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for RestTemplate-based notification clients.
 * Tests the HTTP communication with mock servers.
 */
public class RestTemplateIntegrationTest {
    
    private MockWebServer mockWebServer;
    private RestTemplate restTemplate;
    private SlackClient slackClient;
    private TeamsClient teamsClient;
    
    @BeforeEach
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        restTemplate = new RestTemplate();
        
        slackClient = new RestClientSlackClient(restTemplate);
        teamsClient = new RestClientTeamsClient(restTemplate);
    }
    
    @AfterEach
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    public void testSlackClientSuccessfulSend() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("ok"));
        
        SlackMessage message = SlackMessage.builder()
                .text("Test message")
                .blocks(List.of())
                .build();
        
        String webhookUrl = mockWebServer.url("/slack/webhook").toString();
        
        // When
        NotificationResult result = slackClient.sendMessage(message, webhookUrl).join();
        
        // Then
        assertTrue(result.isSuccessful());
        assertEquals("slack", result.getServiceName());
        assertEquals(200, result.getStatusCode());
        
        // Verify request was made
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/slack/webhook", request.getPath());
        assertEquals("application/json", request.getHeader("Content-Type"));
    }
    
    @Test
    public void testSlackClientErrorResponse() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("Bad Request"));
        
        SlackMessage message = SlackMessage.builder()
                .text("Test message")
                .build();
        
        String webhookUrl = mockWebServer.url("/slack/webhook").toString();
        
        // When
        NotificationResult result = slackClient.sendMessage(message, webhookUrl).join();
        
        // Then
        assertFalse(result.isSuccessful());
        assertEquals("slack", result.getServiceName());
        assertEquals(400, result.getStatusCode());
        assertTrue(result.getErrorMessage().contains("400"));
    }
    
    @Test
    public void testSlackClientServerError() {
        // Given - Server error without retry in RestTemplate
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));
        
        SlackMessage message = SlackMessage.builder()
                .text("Test message")
                .build();
        
        String webhookUrl = mockWebServer.url("/slack/webhook").toString();
        
        // When
        NotificationResult result = slackClient.sendMessage(message, webhookUrl).join();
        
        // Then
        assertFalse(result.isSuccessful());
        assertEquals("slack", result.getServiceName());
        assertEquals(500, result.getStatusCode());
        assertTrue(result.getErrorMessage().contains("500"));
    }
    
    @Test
    public void testTeamsClientSuccessfulSend() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("1"));
        
        TeamsMessage message = TeamsMessage.builder()
                .summary("Test summary")
                .title("Test title")
                .text("Test message")
                .themeColor("FF0000")
                .sections(List.of())
                .build();
        
        String webhookUrl = mockWebServer.url("/teams/webhook").toString();
        
        // When
        NotificationResult result = teamsClient.sendMessage(message, webhookUrl).join();
        
        // Then
        assertTrue(result.isSuccessful());
        assertEquals("teams", result.getServiceName());
        assertEquals(200, result.getStatusCode());
        
        // Verify request was made
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/teams/webhook", request.getPath());
        assertEquals("application/json", request.getHeader("Content-Type"));
        assertTrue(request.getBody().readUtf8().contains("Test summary"));
    }
    
    @Test
    public void testTeamsClientErrorResponse() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("Bad Request"));
        
        TeamsMessage message = TeamsMessage.builder()
                .summary("Test summary")
                .build();
        
        String webhookUrl = mockWebServer.url("/teams/webhook").toString();
        
        // When
        NotificationResult result = teamsClient.sendMessage(message, webhookUrl).join();
        
        // Then
        assertFalse(result.isSuccessful());
        assertEquals("teams", result.getServiceName());
        assertEquals(400, result.getStatusCode());
        assertTrue(result.getErrorMessage().contains("400"));
    }
    
    @Test
    public void testSlackClientConnectionTest() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("ok"));
        
        String webhookUrl = mockWebServer.url("/slack/webhook").toString();
        
        // When
        Boolean connected = slackClient.testConnection(webhookUrl).join();
        
        // Then
        assertTrue(connected);
    }
    
    @Test
    public void testTeamsClientConnectionTest() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("1"));
        
        String webhookUrl = mockWebServer.url("/teams/webhook").toString();
        
        // When
        Boolean connected = teamsClient.testConnection(webhookUrl).join();
        
        // Then
        assertTrue(connected);
    }
}