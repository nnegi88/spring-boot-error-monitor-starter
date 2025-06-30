package io.github.nnegi88.errormonitor.application.config;

import io.github.nnegi88.errormonitor.config.LogbackSlackProperties;
import io.github.nnegi88.errormonitor.config.LogbackTeamsProperties;
import io.github.nnegi88.errormonitor.domain.port.AsyncProcessor;
import io.github.nnegi88.errormonitor.domain.port.MessageFormatter;
import io.github.nnegi88.errormonitor.domain.port.NotificationConfig;
import io.github.nnegi88.errormonitor.domain.port.NotificationService;
import io.github.nnegi88.errormonitor.domain.port.SlackClient;
import io.github.nnegi88.errormonitor.domain.port.TeamsClient;
import io.github.nnegi88.errormonitor.domain.service.NotificationOrchestrator;
import io.github.nnegi88.errormonitor.infrastructure.async.AsyncProcessorImpl;
import io.github.nnegi88.errormonitor.infrastructure.config.SlackConfig;
import io.github.nnegi88.errormonitor.infrastructure.config.TeamsConfig;
import io.github.nnegi88.errormonitor.infrastructure.http.RestClientSlackClient;
import io.github.nnegi88.errormonitor.infrastructure.http.RestClientTeamsClient;
import io.github.nnegi88.errormonitor.infrastructure.notification.slack.SlackMessage;
import io.github.nnegi88.errormonitor.infrastructure.notification.slack.SlackMessageFormatter;
import io.github.nnegi88.errormonitor.infrastructure.notification.slack.SlackNotificationService;
import io.github.nnegi88.errormonitor.infrastructure.notification.teams.TeamsMessage;
import io.github.nnegi88.errormonitor.infrastructure.notification.teams.TeamsMessageFormatter;
import io.github.nnegi88.errormonitor.infrastructure.notification.teams.TeamsNotificationService;
import io.github.nnegi88.errormonitor.logback.UnifiedNotificationAppender;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Auto-configuration for the new SOLID-compliant notification system.
 * Follows dependency inversion principle by depending on abstractions.
 */
@AutoConfiguration
@EnableConfigurationProperties({LogbackSlackProperties.class, LogbackTeamsProperties.class})
public class SolidNotificationAutoConfiguration {
    
    // Infrastructure layer beans
    
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate notificationRestTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public AsyncProcessor asyncProcessor() {
        return new AsyncProcessorImpl(256, 1, 4);
    }
    
    // HTTP client implementations
    
    @Bean
    @ConditionalOnMissingBean
    public SlackClient slackClient(RestTemplate notificationRestTemplate) {
        return new RestClientSlackClient(notificationRestTemplate);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public TeamsClient teamsClient(RestTemplate notificationRestTemplate) {
        return new RestClientTeamsClient(notificationRestTemplate);
    }
    
    // Message formatters
    
    @Bean
    @ConditionalOnMissingBean(name = "slackMessageFormatter")
    public MessageFormatter<SlackMessage> slackMessageFormatter() {
        return new SlackMessageFormatter();
    }
    
    @Bean
    @ConditionalOnMissingBean(name = "teamsMessageFormatter")
    public MessageFormatter<TeamsMessage> teamsMessageFormatter() {
        return new TeamsMessageFormatter();
    }
    
    // Notification services
    
    @Bean
    @ConditionalOnProperty(prefix = "logback.slack", name = "enabled", havingValue = "true")
    public NotificationService slackNotificationService(
            SlackClient slackClient,
            MessageFormatter<SlackMessage> slackMessageFormatter) {
        return new SlackNotificationService(slackClient, slackMessageFormatter);
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "logback.teams", name = "enabled", havingValue = "true")
    public NotificationService teamsNotificationService(
            TeamsClient teamsClient,
            MessageFormatter<TeamsMessage> teamsMessageFormatter) {
        return new TeamsNotificationService(teamsClient, teamsMessageFormatter);
    }
    
    // Configuration objects
    
    @Bean
    @ConditionalOnProperty(prefix = "logback.slack", name = "enabled", havingValue = "true")
    public NotificationConfig slackConfig(LogbackSlackProperties slackProperties, Environment environment) {
        return SlackConfig.builder()
                .webhookUrl(slackProperties.getWebhookUrl())
                .applicationName(environment.resolvePlaceholders(slackProperties.getApplicationName()))
                .environment(environment.resolvePlaceholders(slackProperties.getEnvironment()))
                .minimumLevel(slackProperties.getMinimumLevel())
                .enabled(slackProperties.isEnabled())
                .additionalProperties(Map.of(
                        "webhookUrl", slackProperties.getWebhookUrl(),
                        "includeStackTrace", slackProperties.isIncludeStackTrace(),
                        "connectionTimeout", slackProperties.getConnectionTimeout(),
                        "readTimeout", slackProperties.getReadTimeout()
                ))
                .build();
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "logback.teams", name = "enabled", havingValue = "true")
    public NotificationConfig teamsConfig(LogbackTeamsProperties teamsProperties, Environment environment) {
        return TeamsConfig.builder()
                .webhookUrl(teamsProperties.getWebhookUrl())
                .applicationName(environment.resolvePlaceholders(teamsProperties.getApplicationName()))
                .environment(environment.resolvePlaceholders(teamsProperties.getEnvironment()))
                .minimumLevel(teamsProperties.getMinimumLevel())
                .enabled(teamsProperties.isEnabled())
                .additionalProperties(Map.of(
                        "webhookUrl", teamsProperties.getWebhookUrl(),
                        "includeStackTrace", teamsProperties.isIncludeStackTrace(),
                        "themeColor", teamsProperties.getThemeColor(),
                        "connectionTimeout", teamsProperties.getConnectionTimeout(),
                        "readTimeout", teamsProperties.getReadTimeout()
                ))
                .build();
    }
    
    // Domain service
    
    @Bean
    @ConditionalOnMissingBean
    public NotificationOrchestrator notificationOrchestrator(List<NotificationService> notificationServices) {
        return new NotificationOrchestrator(notificationServices);
    }
    
    // Unified appender
    
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "unifiedNotificationAppender")
    public UnifiedNotificationAppender unifiedNotificationAppender(
            NotificationOrchestrator orchestrator,
            AsyncProcessor asyncProcessor,
            List<NotificationConfig> configurations) {
        
        UnifiedNotificationAppender appender = new UnifiedNotificationAppender();
        appender.setOrchestrator(orchestrator);
        appender.setAsyncProcessor(asyncProcessor);
        appender.setConfigurations(configurations);
        appender.setAsync(true);
        
        return appender;
    }
}