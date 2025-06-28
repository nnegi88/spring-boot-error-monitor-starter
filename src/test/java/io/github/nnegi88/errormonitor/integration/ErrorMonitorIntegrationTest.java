package io.github.nnegi88.errormonitor.integration;

import io.github.nnegi88.errormonitor.config.ErrorMonitorAutoConfiguration;
import io.github.nnegi88.errormonitor.interceptor.GlobalExceptionHandler;
import io.github.nnegi88.errormonitor.model.ErrorEvent;
import io.github.nnegi88.errormonitor.notification.NotificationClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErrorMonitorIntegrationTest {

    private MockWebServer mockWebServer;
    private WebApplicationContextRunner contextRunner;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        contextRunner = new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ErrorMonitorAutoConfiguration.class))
                .withUserConfiguration(TestWebConfiguration.class);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void testSlackIntegration() throws Exception {
        String webhookUrl = mockWebServer.url("/slack-webhook").toString();
        
        contextRunner
                .withPropertyValues(
                        "spring.error-monitor.enabled=true",
                        "spring.error-monitor.notification.platform=slack",
                        "spring.error-monitor.notification.slack.webhook-url=" + webhookUrl,
                        "spring.error-monitor.notification.slack.channel=#test-alerts",
                        "spring.application.name=integration-test-app",
                        "spring.profiles.active=test"
                )
                .run(context -> {
                    MockMvc mockMvc = MockMvcBuilders
                            .standaloneSetup(context.getBean(TestController.class))
                            .setControllerAdvice(context.getBean(GlobalExceptionHandler.class))
                            .build();
                    
                    // Queue successful response
                    mockWebServer.enqueue(new MockResponse()
                            .setResponseCode(200)
                            .setBody("ok"));
                    
                    // Trigger an error
                    mockMvc.perform(get("/test/error"))
                            .andExpect(status().isInternalServerError());
                    
                    // Wait for async notification
                    RecordedRequest request = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
                    assertThat(request).isNotNull();
                    assertThat(request.getMethod()).isEqualTo("POST");
                    
                    String body = request.getBody().readUtf8();
                    assertThat(body).contains("\"text\":");
                    assertThat(body).contains("Application Error Detected");
                    assertThat(body).contains("integration-test-app");
                    assertThat(body).contains("RuntimeException");
                    assertThat(body).contains("Test error from controller");
                    assertThat(body).contains("\"channel\":\"#test-alerts\"");
                });
    }
    
    @Test
    void testTeamsIntegration() throws Exception {
        String webhookUrl = mockWebServer.url("/teams-webhook").toString();
        
        contextRunner
                .withPropertyValues(
                        "spring.error-monitor.enabled=true",
                        "spring.error-monitor.notification.platform=teams",
                        "spring.error-monitor.notification.teams.webhook-url=" + webhookUrl,
                        "spring.error-monitor.notification.teams.theme-color=FF0000",
                        "spring.application.name=teams-test-app"
                )
                .run(context -> {
                    MockMvc mockMvc = MockMvcBuilders
                            .standaloneSetup(context.getBean(TestController.class))
                            .setControllerAdvice(context.getBean(GlobalExceptionHandler.class))
                            .build();
                    
                    // Queue successful response
                    mockWebServer.enqueue(new MockResponse()
                            .setResponseCode(200)
                            .setBody("1"));
                    
                    // Trigger an error
                    mockMvc.perform(get("/test/error"))
                            .andExpect(status().isInternalServerError());
                    
                    // Wait for async notification
                    RecordedRequest request = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
                    assertThat(request).isNotNull();
                    
                    String body = request.getBody().readUtf8();
                    assertThat(body).contains("\"@type\":\"MessageCard\"");
                    assertThat(body).contains("\"themeColor\":\"FF0000\"");
                    assertThat(body).contains("teams-test-app");
                    assertThat(body).contains("RuntimeException");
                });
    }
    
    @Test
    void testBothPlatformsIntegration() throws Exception {
        String slackUrl = mockWebServer.url("/slack").toString();
        String teamsUrl = mockWebServer.url("/teams").toString();
        
        contextRunner
                .withPropertyValues(
                        "spring.error-monitor.enabled=true",
                        "spring.error-monitor.notification.platform=both",
                        "spring.error-monitor.notification.slack.webhook-url=" + slackUrl,
                        "spring.error-monitor.notification.teams.webhook-url=" + teamsUrl
                )
                .run(context -> {
                    MockMvc mockMvc = MockMvcBuilders
                            .standaloneSetup(context.getBean(TestController.class))
                            .setControllerAdvice(context.getBean(GlobalExceptionHandler.class))
                            .build();
                    
                    // Queue responses for both platforms
                    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
                    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("1"));
                    
                    // Trigger an error
                    mockMvc.perform(get("/test/error"))
                            .andExpect(status().isInternalServerError());
                    
                    // Should receive two requests
                    RecordedRequest request1 = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
                    RecordedRequest request2 = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
                    
                    assertThat(request1).isNotNull();
                    assertThat(request2).isNotNull();
                    
                    // One should be Slack, one should be Teams
                    String body1 = request1.getBody().readUtf8();
                    String body2 = request2.getBody().readUtf8();
                    
                    boolean hasSlack = body1.contains("\"blocks\"") || body2.contains("\"blocks\"");
                    boolean hasTeams = body1.contains("MessageCard") || body2.contains("MessageCard");
                    
                    assertThat(hasSlack).isTrue();
                    assertThat(hasTeams).isTrue();
                });
    }
    
    @Test
    void testFilteringIntegration() throws Exception {
        String webhookUrl = mockWebServer.url("/webhook").toString();
        
        contextRunner
                .withPropertyValues(
                        "spring.error-monitor.enabled=true",
                        "spring.error-monitor.notification.platform=slack",
                        "spring.error-monitor.notification.slack.webhook-url=" + webhookUrl,
                        "spring.error-monitor.filtering.excluded-exceptions=io.github.nnegi88.errormonitor.integration.ErrorMonitorIntegrationTest$BadRequestException"
                )
                .run(context -> {
                    MockMvc mockMvc = MockMvcBuilders
                            .standaloneSetup(context.getBean(TestController.class))
                            .setControllerAdvice(context.getBean(GlobalExceptionHandler.class))
                            .build();
                    
                    // Trigger a filtered error
                    mockMvc.perform(get("/test/filtered-error"))
                            .andExpect(status().isBadRequest());
                    
                    // Should not receive any notification
                    RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
                    assertThat(request).isNull();
                });
    }
    
    @Test
    void testRateLimitingIntegration() throws Exception {
        String webhookUrl = mockWebServer.url("/webhook").toString();
        
        contextRunner
                .withPropertyValues(
                        "spring.error-monitor.enabled=true",
                        "spring.error-monitor.notification.platform=slack",
                        "spring.error-monitor.notification.slack.webhook-url=" + webhookUrl,
                        "spring.error-monitor.rate-limiting.burst-limit=2"
                )
                .run(context -> {
                    MockMvc mockMvc = MockMvcBuilders
                            .standaloneSetup(context.getBean(TestController.class))
                            .setControllerAdvice(context.getBean(GlobalExceptionHandler.class))
                            .build();
                    
                    // Queue responses
                    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
                    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
                    
                    // First two errors should be notified
                    mockMvc.perform(get("/test/error")).andExpect(status().isInternalServerError());
                    mockMvc.perform(get("/test/error")).andExpect(status().isInternalServerError());
                    
                    // Third error should be rate limited
                    mockMvc.perform(get("/test/error")).andExpect(status().isInternalServerError());
                    
                    // Should only receive 2 requests
                    assertThat(mockWebServer.takeRequest(1, TimeUnit.SECONDS)).isNotNull();
                    assertThat(mockWebServer.takeRequest(1, TimeUnit.SECONDS)).isNotNull();
                    assertThat(mockWebServer.takeRequest(1, TimeUnit.SECONDS)).isNull();
                });
    }
    
    @Test
    void testRequestContextCapture() throws Exception {
        String webhookUrl = mockWebServer.url("/webhook").toString();
        
        contextRunner
                .withPropertyValues(
                        "spring.error-monitor.enabled=true",
                        "spring.error-monitor.notification.platform=slack",
                        "spring.error-monitor.notification.slack.webhook-url=" + webhookUrl,
                        "spring.error-monitor.context.include-request-details=true"
                )
                .run(context -> {
                    MockMvc mockMvc = MockMvcBuilders
                            .standaloneSetup(context.getBean(TestController.class))
                            .setControllerAdvice(context.getBean(GlobalExceptionHandler.class))
                            .build();
                    
                    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
                    
                    // Make request with specific details
                    mockMvc.perform(get("/test/users/123")
                            .header("User-Agent", "Test-Browser/1.0")
                            .header("Referer", "https://example.com")
                            .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isNotFound());
                    
                    RecordedRequest request = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
                    String body = request.getBody().readUtf8();
                    
                    assertThat(body).contains("/test/users/123");
                    assertThat(body).contains("Test-Browser/1.0");
                    assertThat(body).contains("GET");
                });
    }
    
    @Configuration
    @EnableWebMvc
    static class TestWebConfiguration {
        
        @Bean
        public TestController testController() {
            return new TestController();
        }
    }
    
    @Controller
    static class TestController {
        
        @GetMapping("/test/error")
        @ResponseBody
        public String triggerError() {
            throw new RuntimeException("Test error from controller");
        }
        
        @GetMapping("/test/filtered-error")
        @ResponseBody
        public String triggerFilteredError() {
            throw new BadRequestException("This should be filtered");
        }
        
        @GetMapping("/test/users/{id}")
        @ResponseBody
        public String getUser(@PathVariable String id) {
            throw new UserNotFoundException("User not found: " + id);
        }
    }
    
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    static class BadRequestException extends IllegalArgumentException {
        public BadRequestException(String message) {
            super(message);
        }
    }
    
    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }
}