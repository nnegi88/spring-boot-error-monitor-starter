package io.github.nnegi88.errormonitor.config;

import io.github.nnegi88.errormonitor.filter.ErrorFilter;
import io.github.nnegi88.errormonitor.filter.PackageErrorFilter;
import io.github.nnegi88.errormonitor.filter.RateLimitingErrorFilter;
import io.github.nnegi88.errormonitor.interceptor.GlobalExceptionHandler;
import io.github.nnegi88.errormonitor.notification.CompositeNotificationClient;
import io.github.nnegi88.errormonitor.notification.NotificationClient;
import io.github.nnegi88.errormonitor.notification.slack.SlackClient;
import io.github.nnegi88.errormonitor.notification.teams.TeamsClient;
import io.github.nnegi88.errormonitor.notification.template.SlackMessageTemplate;
import io.github.nnegi88.errormonitor.notification.template.TeamsMessageTemplate;
import io.github.nnegi88.errormonitor.core.ErrorProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorMonitorAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ErrorMonitorAutoConfiguration.class,
                    AsyncConfiguration.class
            ));
            
    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ErrorMonitorAutoConfiguration.class,
                    AsyncConfiguration.class
            ));

    @Test
    void testAutoConfigurationEnabled() {
        contextRunner
                .withPropertyValues(
                        "spring.error-monitor.enabled=true",
                        "spring.error-monitor.notification.platform=slack",
                        "spring.error-monitor.notification.slack.webhook-url=https://hooks.slack.com/test"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ErrorMonitorProperties.class);
                    assertThat(context).hasSingleBean(ErrorProcessor.class);
                    assertThat(context).hasSingleBean(NotificationClient.class);
                    
                    // Verify the NotificationClient is configured for Slack
                    NotificationClient client = context.getBean(NotificationClient.class);
                    assertThat(client).isInstanceOf(CompositeNotificationClient.class);
                });
    }

    @Test
    void testAutoConfigurationDisabled() {
        contextRunner
                .withPropertyValues("spring.error-monitor.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ErrorProcessor.class);
                    assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(NotificationClient.class);
                });
    }

    @Test
    void testSlackConfiguration() {
        contextRunner
                .withPropertyValues(
                        "spring.error-monitor.enabled=true",
                        "spring.error-monitor.notification.platform=slack",
                        "spring.error-monitor.notification.slack.webhook-url=https://hooks.slack.com/test",
                        "spring.error-monitor.notification.slack.channel=#alerts"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(NotificationClient.class);
                    assertThat(context).hasSingleBean(SlackMessageTemplate.class);
                    assertThat(context).hasSingleBean(TeamsMessageTemplate.class); // Template beans are always created
                    
                    NotificationClient client = context.getBean(NotificationClient.class);
                    assertThat(client).isInstanceOf(CompositeNotificationClient.class);
                });
    }

    @Test
    void testTeamsConfiguration() {
        contextRunner
                .withPropertyValues(
                        "spring.error-monitor.enabled=true",
                        "spring.error-monitor.notification.platform=teams",
                        "spring.error-monitor.notification.teams.webhook-url=https://outlook.office.com/webhook/test"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(NotificationClient.class);
                    assertThat(context).hasSingleBean(TeamsMessageTemplate.class);
                    assertThat(context).hasSingleBean(SlackMessageTemplate.class); // Template beans are always created
                    
                    NotificationClient client = context.getBean(NotificationClient.class);
                    assertThat(client).isInstanceOf(CompositeNotificationClient.class);
                });
    }

    @Test
    void testBothPlatformsConfiguration() {
        contextRunner
                .withPropertyValues(
                        "spring.error-monitor.enabled=true",
                        "spring.error-monitor.notification.platform=both",
                        "spring.error-monitor.notification.slack.webhook-url=https://hooks.slack.com/test",
                        "spring.error-monitor.notification.teams.webhook-url=https://outlook.office.com/webhook/test"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(NotificationClient.class);
                    assertThat(context).hasSingleBean(SlackMessageTemplate.class);
                    assertThat(context).hasSingleBean(TeamsMessageTemplate.class);
                    
                    // The composite client should be created
                    NotificationClient client = context.getBean(NotificationClient.class);
                    assertThat(client).isInstanceOf(CompositeNotificationClient.class);
                });
    }

    @Test
    void testFilterConfiguration() {
        contextRunner
                .withPropertyValues(
                        "spring.error-monitor.enabled=true",
                        "spring.error-monitor.notification.platform=slack",
                        "spring.error-monitor.notification.slack.webhook-url=https://hooks.slack.com/test",
                        "spring.error-monitor.filtering.enabled-packages=com.example,com.test",
                        "spring.error-monitor.filtering.excluded-exceptions=java.lang.IllegalArgumentException",
                        "spring.error-monitor.filtering.minimum-severity=HIGH",
                        "spring.error-monitor.rate-limiting.max-errors-per-minute=10",
                        "spring.error-monitor.rate-limiting.burst-limit=3"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(PackageErrorFilter.class);
                    assertThat(context).hasSingleBean(RateLimitingErrorFilter.class);
                    // Should have 3 ErrorFilter beans: PackageErrorFilter, RateLimitingErrorFilter, and the composite
                    assertThat(context.getBeanNamesForType(ErrorFilter.class)).hasSize(3);
                });
    }

    @Test
    void testCustomBeans() {
        contextRunner
                .withUserConfiguration(CustomConfiguration.class)
                .withPropertyValues(
                        "spring.error-monitor.enabled=true",
                        "spring.error-monitor.notification.platform=slack",
                        "spring.error-monitor.notification.slack.webhook-url=https://hooks.slack.com/test"
                )
                .run(context -> {
                    // Custom template should be used
                    assertThat(context).hasSingleBean(SlackMessageTemplate.class);
                    assertThat(context.getBean(SlackMessageTemplate.class))
                            .isInstanceOf(CustomSlackMessageTemplate.class);
                    
                    // Custom filter should be added
                    // Should have 4 ErrorFilter beans: PackageErrorFilter, RateLimitingErrorFilter, customErrorFilter, and composite
                    assertThat(context.getBeanNamesForType(ErrorFilter.class)).hasSize(4);
                });
    }

    @Test
    void testMissingWebhookUrl() {
        contextRunner
                .withPropertyValues(
                        "spring.error-monitor.enabled=true",
                        "spring.error-monitor.notification.platform=slack"
                        // Missing webhook URL
                )
                .run(context -> {
                    // Should still create beans but client won't work without URL
                    assertThat(context).hasSingleBean(ErrorMonitorProperties.class);
                    assertThat(context).hasSingleBean(NotificationClient.class);
                });
    }

    @Test
    void testApplicationNameAndEnvironment() {
        contextRunner
                .withPropertyValues(
                        "spring.error-monitor.enabled=true",
                        "spring.error-monitor.notification.platform=slack",
                        "spring.error-monitor.notification.slack.webhook-url=https://hooks.slack.com/test",
                        "spring.application.name=test-app",
                        "spring.profiles.active=production"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(SlackMessageTemplate.class);
                    // The template should be created with application name and environment
                });
    }
    
    @Test
    void testWebApplicationConfiguration() {
        webContextRunner
                .withPropertyValues(
                        "spring.error-monitor.enabled=true",
                        "spring.error-monitor.notification.platform=slack",
                        "spring.error-monitor.notification.slack.webhook-url=https://hooks.slack.com/test"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
                    assertThat(context).hasBean("errorMonitorFilterRegistration");
                });
    }

    @Configuration
    static class CustomConfiguration {
        
        @Bean
        public SlackMessageTemplate customSlackTemplate() {
            return new CustomSlackMessageTemplate();
        }
        
        @Bean
        public ErrorFilter customErrorFilter() {
            return event -> true; // Allow all
        }
    }
    
    static class CustomSlackMessageTemplate implements SlackMessageTemplate {
        @Override
        public io.github.nnegi88.errormonitor.notification.slack.SlackMessage buildMessage(
                io.github.nnegi88.errormonitor.model.ErrorEvent event) {
            return io.github.nnegi88.errormonitor.notification.slack.SlackMessage.builder()
                    .text("Custom message")
                    .build();
        }
    }
}