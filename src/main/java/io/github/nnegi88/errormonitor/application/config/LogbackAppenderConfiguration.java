package io.github.nnegi88.errormonitor.application.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Logger;
import io.github.nnegi88.errormonitor.config.LogbackSlackProperties;
import io.github.nnegi88.errormonitor.config.LogbackTeamsProperties;
import io.github.nnegi88.errormonitor.domain.port.AsyncProcessor;
import io.github.nnegi88.errormonitor.domain.port.NotificationConfig;
import io.github.nnegi88.errormonitor.domain.service.NotificationOrchestrator;
import io.github.nnegi88.errormonitor.infrastructure.config.SlackConfig;
import io.github.nnegi88.errormonitor.infrastructure.config.TeamsConfig;
import io.github.nnegi88.errormonitor.logback.UnifiedNotificationAppender;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Configures Logback appenders for Slack and Teams notifications.
 * This configuration runs after the main notification auto-configuration
 * and sets up the Logback integration.
 */
@Configuration
@ConditionalOnClass(ch.qos.logback.classic.LoggerContext.class)
@AutoConfigureAfter(SolidNotificationAutoConfiguration.class)
@EnableConfigurationProperties({LogbackSlackProperties.class, LogbackTeamsProperties.class})
@ConditionalOnBean(NotificationOrchestrator.class)
public class LogbackAppenderConfiguration {
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LogbackAppenderConfiguration.class);
    
    private final LogbackSlackProperties slackProperties;
    private final LogbackTeamsProperties teamsProperties;
    private final NotificationOrchestrator orchestrator;
    private final AsyncProcessor asyncProcessor;
    private final Environment environment;
    
    public LogbackAppenderConfiguration(
            LogbackSlackProperties slackProperties,
            LogbackTeamsProperties teamsProperties,
            NotificationOrchestrator orchestrator,
            AsyncProcessor asyncProcessor,
            Environment environment) {
        this.slackProperties = slackProperties;
        this.teamsProperties = teamsProperties;
        this.orchestrator = orchestrator;
        this.asyncProcessor = asyncProcessor;
        this.environment = environment;
    }
    
    @PostConstruct
    public void configureLogbackAppenders() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // Create configurations list
        List<NotificationConfig> configurations = new ArrayList<>();
        
        // Add Slack configuration if enabled
        if (slackProperties.isEnabled() && slackProperties.getWebhookUrl() != null) {
            logger.info("Configuring Slack notifications - webhook URL: {}", 
                    slackProperties.getWebhookUrl().replaceAll("([^/]+)$", "***"));
            SlackConfig slackConfig = SlackConfig.builder()
                    .webhookUrl(slackProperties.getWebhookUrl())
                    .applicationName(environment.resolvePlaceholders(slackProperties.getApplicationName()))
                    .environment(environment.resolvePlaceholders(slackProperties.getEnvironment()))
                    .minimumLevel(slackProperties.getMinimumLevel())
                    .additionalProperties(Map.of("includeStackTrace", slackProperties.isIncludeStackTrace()))
                    .enabled(true)
                    .build();
            configurations.add(slackConfig);
            logger.info("Slack notification configuration added to Logback appender");
        } else {
            logger.info("Slack notifications disabled or webhook URL not configured");
        }
        
        // Add Teams configuration if enabled
        if (teamsProperties.isEnabled() && teamsProperties.getWebhookUrl() != null) {
            TeamsConfig teamsConfig = TeamsConfig.builder()
                    .webhookUrl(teamsProperties.getWebhookUrl())
                    .applicationName(environment.resolvePlaceholders(teamsProperties.getApplicationName()))
                    .environment(environment.resolvePlaceholders(teamsProperties.getEnvironment()))
                    .minimumLevel(teamsProperties.getMinimumLevel())
                    .enabled(true)
                    .additionalProperties(Map.of(
                            "themeColor", teamsProperties.getThemeColor(),
                            "includeStackTrace", teamsProperties.isIncludeStackTrace()
                    ))
                    .build();
            configurations.add(teamsConfig);
            logger.info("Teams notification configuration added to Logback appender");
        }
        
        if (configurations.isEmpty()) {
            logger.warn("No notification configurations enabled for Logback appender");
            return;
        }
        
        // Create and configure the appender
        UnifiedNotificationAppender appender = new UnifiedNotificationAppender();
        appender.setName("UNIFIED_NOTIFICATION");
        appender.setContext(loggerContext);
        appender.setOrchestrator(orchestrator);
        appender.setAsyncProcessor(asyncProcessor);
        appender.setConfigurations(configurations);
        
        // Set async mode based on properties (prefer Slack's setting if both are configured)
        boolean asyncMode = slackProperties.isEnabled() ? slackProperties.isAsync() : teamsProperties.isAsync();
        appender.setAsync(asyncMode);
        
        // Start the appender
        appender.start();
        
        // Add appender to root logger
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(appender);
        
        logger.info("UnifiedNotificationAppender configured and attached to root logger");
    }
    
    private String determineMinimumLevel() {
        // Use the lowest (most verbose) level from enabled configurations
        String slackLevel = slackProperties.isEnabled() ? slackProperties.getMinimumLevel() : "ERROR";
        String teamsLevel = teamsProperties.isEnabled() ? teamsProperties.getMinimumLevel() : "ERROR";
        
        Level slackLevelObj = Level.toLevel(slackLevel, Level.ERROR);
        Level teamsLevelObj = Level.toLevel(teamsLevel, Level.ERROR);
        
        return slackLevelObj.isGreaterOrEqual(teamsLevelObj) ? teamsLevel : slackLevel;
    }
}