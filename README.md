# Spring Boot Logback Alerting Starter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.nnegi88/spring-boot-error-monitor-starter.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.nnegi88%22%20AND%20a:%22spring-boot-error-monitor-starter%22)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-11%2B-blue.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.3%2B-green.svg)](https://spring.io/projects/spring-boot)

A SOLID-compliant Spring Boot starter providing extensible Logback appenders for sending log alerts to Slack and Microsoft Teams. Built with clean architecture principles for maximum maintainability and extensibility.

## Features

- 🏗️ **SOLID Architecture** - Clean, maintainable, and extensible design
- 📢 **Slack Integration** - Rich message formatting with Block Kit
- 📧 **Teams Integration** - Adaptive Cards with color-coded alerts  
- ⚡ **Async Processing** - Non-blocking delivery with configurable thread pools
- 🚦 **Rate Limiting** - Built-in protection against notification flooding
- 🔍 **MDC Support** - Contextual information from Mapped Diagnostic Context
- 🎯 **Dependency Injection** - Full Spring integration with interface-based design
- 🔌 **Extensible** - Easy to add new notification platforms
- 🚀 **High Performance** - Optimized for low-latency, high-throughput scenarios

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>io.github.nnegi88</groupId>
    <artifactId>spring-boot-error-monitor-starter</artifactId>
    <version>1.0.2</version>
</dependency>
```

### 2. Configure Slack

```properties
# Enable Slack alerting
logback.slack.enabled=true
logback.slack.webhook-url=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
logback.slack.minimum-level=ERROR
```

### 3. Configure Teams (Optional)

```properties
# Enable Teams alerting
logback.teams.enabled=true
logback.teams.webhook-url=https://outlook.office.com/webhook/YOUR/WEBHOOK/URL
logback.teams.minimum-level=ERROR
```

### 4. Start Logging

```java
@Component
public class MyService {
    private static final Logger logger = LoggerFactory.getLogger(MyService.class);
    
    public void doSomething() {
        try {
            // Your code here
        } catch (Exception e) {
            logger.error("Something went wrong!", e);
            // This will automatically send to Slack/Teams
        }
    }
}
```

## Configuration Reference

### Slack Properties

| Property | Description | Default |
|----------|-------------|---------|
| `logback.slack.enabled` | Enable Slack appender | `false` |
| `logback.slack.webhook-url` | Slack webhook URL (required) | - |
| `logback.slack.application-name` | Application name in messages | `${spring.application.name}` |
| `logback.slack.environment` | Environment name in messages | `${spring.profiles.active}` |
| `logback.slack.minimum-level` | Minimum log level to send | `ERROR` |
| `logback.slack.include-stack-trace` | Include stack traces | `true` |
| `logback.slack.connection-timeout` | Connection timeout (ms) | `5000` |
| `logback.slack.read-timeout` | Read timeout (ms) | `5000` |
| `logback.slack.async` | Enable async processing | `true` |
| `logback.slack.queue-size` | Async queue size | `256` |
| `logback.slack.rate-limit-enabled` | Enable rate limiting | `true` |
| `logback.slack.max-messages-per-minute` | Max messages per minute | `10` |

### Teams Properties

| Property | Description | Default |
|----------|-------------|---------|
| `logback.teams.enabled` | Enable Teams appender | `false` |
| `logback.teams.webhook-url` | Teams webhook URL (required) | - |
| `logback.teams.application-name` | Application name in messages | `${spring.application.name}` |
| `logback.teams.environment` | Environment name in messages | `${spring.profiles.active}` |
| `logback.teams.minimum-level` | Minimum log level to send | `ERROR` |
| `logback.teams.include-stack-trace` | Include stack traces | `true` |
| `logback.teams.theme-color` | Card theme color (hex) | `FF0000` |
| `logback.teams.connection-timeout` | Connection timeout (ms) | `5000` |
| `logback.teams.read-timeout` | Read timeout (ms) | `5000` |
| `logback.teams.async` | Enable async processing | `true` |
| `logback.teams.queue-size` | Async queue size | `256` |
| `logback.teams.rate-limit-enabled` | Enable rate limiting | `true` |
| `logback.teams.max-messages-per-minute` | Max messages per minute | `10` |

## Advanced Usage

### Using with logback-spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="UNIFIED_NOTIFICATIONS" class="io.github.nnegi88.errormonitor.logback.UnifiedNotificationAppender">
        <!-- Configuration is handled via Spring properties -->
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="UNIFIED_NOTIFICATIONS" />
    </root>
</configuration>
```

### Custom Notification Service

```java
@Component
public class DiscordNotificationService implements NotificationService {
    
    @Override
    public CompletableFuture<NotificationResult> sendNotification(NotificationMessage message) {
        // Custom Discord implementation
        return CompletableFuture.completedFuture(NotificationResult.success("discord"));
    }
    
    @Override
    public boolean supports(NotificationConfig config) {
        return config.getWebhookUrl().contains("discord.com");
    }
    
    @Override
    public String getServiceName() {
        return "discord";
    }
}
```

### Using MDC for Context

```java
import org.slf4j.MDC;

public void processUser(String userId) {
    MDC.put("userId", userId);
    MDC.put("operation", "processUser");
    
    try {
        // Your code here
    } catch (Exception e) {
        logger.error("Failed to process user", e);
        // MDC context will be included in the alert
    } finally {
        MDC.clear();
    }
}
```

## Message Formats

### Slack Format
Messages are sent using Slack's Block Kit format with:
- Header with log level
- Application and environment info
- Log message
- Stack trace (if enabled)
- MDC context fields

### Teams Format
Messages are sent as Adaptive Cards with:
- Color-coded by log level (Red=ERROR, Orange=WARN, Blue=INFO)
- Structured facts for easy reading
- Collapsible stack traces
- MDC context as additional facts

## Setting Up Webhooks

### Slack Webhook Setup
1. Go to your Slack workspace settings
2. Navigate to "Apps" → "Custom Integrations" → "Incoming Webhooks"
3. Add a new webhook and select the channel
4. Copy the webhook URL
5. Set it in your application properties

### Teams Webhook Setup
1. Open Microsoft Teams and navigate to your channel
2. Click the "..." menu → "Connectors"
3. Search for "Incoming Webhook" and add it
4. Give it a name and optional icon
5. Copy the webhook URL
6. Set it in your application properties

## Best Practices

1. **Set Appropriate Log Levels** - Only send actionable alerts (ERROR/WARN)
2. **Use MDC for Context** - Add relevant business context to all logs
3. **Implement Rate Limiting** - Prevent channel flooding in error loops
4. **Secure Your Webhooks** - Store webhook URLs in environment variables
5. **Test Before Production** - Verify webhook connectivity in lower environments

## Troubleshooting

### Messages Not Appearing

1. Verify webhook URL is correct and active
2. Check application logs for appender errors
3. Ensure log level meets the configured minimum threshold
4. Verify network connectivity to Slack/Teams

### Performance Issues

1. Ensure async mode is enabled (default)
2. Increase queue size if seeing dropped messages
3. Monitor memory usage with large queue sizes
4. Consider rate limiting for high-volume applications

### Debug Logging

Enable debug logging for the appenders:

```yaml
logging:
  level:
    io.github.nnegi88.errormonitor.logback: DEBUG
```

### Spring Property Placeholder Resolution

The starter automatically resolves Spring property placeholders in configuration values. For example:

- `${spring.application.name:Unknown}` resolves to your application name or "Unknown" if not set
- `${spring.profiles.active:default}` resolves to the active profile or "default" if none

This ensures that notifications display actual values instead of placeholder strings:

```
# Before resolution
Alert from ${spring.application.name:Unknown} in ${spring.profiles.active:default}

# After resolution  
Alert from my-app in production
```

The resolution happens automatically using Spring's `Environment.resolvePlaceholders()` method during configuration initialization.

## Architecture

This starter follows SOLID principles and clean architecture:

```
Domain Layer (Business Logic)
├── Models: LogEvent, NotificationMessage, NotificationResult
├── Ports: NotificationService, HttpClient, MessageFormatter
└── Services: NotificationOrchestrator

Infrastructure Layer (Implementation Details)  
├── HTTP: HttpClientImpl, RetryableHttpClient
├── Async: AsyncProcessorImpl
├── Notifications: SlackNotificationService, TeamsNotificationService
├── Formatters: SlackMessageFormatter, TeamsMessageFormatter
└── Configuration: SlackConfig, TeamsConfig

Application Layer (Spring Integration)
└── AutoConfiguration: SolidNotificationAutoConfiguration
```

### SOLID Principles Applied

- **S**ingle Responsibility: Each class has one reason to change
- **O**pen/Closed: Easy to extend with new notification services
- **L**iskov Substitution: All implementations are interchangeable
- **I**nterface Segregation: Small, focused interfaces
- **D**ependency Inversion: Depends on abstractions, not concretions

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and feature requests, please use the [GitHub issue tracker](https://github.com/nnegi88/spring-boot-error-monitor-starter/issues).