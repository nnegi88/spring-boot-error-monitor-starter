# Spring Boot Error Monitor Starter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.nnegi88/spring-boot-error-monitor-starter.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.nnegi88%22%20AND%20a:%22spring-boot-error-monitor-starter%22)
[![CI Status](https://github.com/nnegi88/spring-boot-error-monitor-starter/workflows/CI/badge.svg)](https://github.com/nnegi88/spring-boot-error-monitor-starter/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-11%2B-blue.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.5%2B-green.svg)](https://spring.io/projects/spring-boot)

A Spring Boot starter library that provides centralized error monitoring with flexible notification capabilities for any Spring Boot application. Automatically detects and reports errors via Slack or Microsoft Teams alerts.

## Why Use Spring Boot Error Monitor Starter?

- **Zero-Code Integration**: Just add the dependency and configure webhooks - no code changes required
- **Instant Visibility**: Get notified about production errors in real-time via Slack/Teams
- **Performance First**: Minimal overhead (<2ms per request) with async processing
- **Enterprise Ready**: Production-tested with rate limiting, metrics, and health checks
- **Smart Filtering**: Reduce noise with configurable error filtering and aggregation
- **Full Context**: Rich error details including stack traces, request info, and custom context

## Features

- 🚨 Automatic error detection and reporting
- 📱 Multi-platform support (Slack & Microsoft Teams)
- 🔧 Zero-code integration with Spring Boot applications
- 🎯 Configurable error filtering and rate limiting
- 📊 Rich error context including stack traces and request details
- ⚡ Asynchronous processing for minimal performance impact
- 🛡️ Built-in security features for sensitive data masking
- 📈 Metrics collection with Micrometer integration
- 🏥 Health check endpoints for monitoring
- 📊 Advanced error analytics and trend detection
- 🎛️ Management endpoints via Spring Boot Actuator

## Requirements

- Java 11 or higher
- Spring Boot 2.5 or higher
- Spring WebFlux (for async HTTP clients)

## Quick Start

### 1. Add Dependency

#### Maven
```xml
<dependency>
    <groupId>io.github.nnegi88</groupId>
    <artifactId>spring-boot-error-monitor-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Gradle
```gradle
implementation 'io.github.nnegi88:spring-boot-error-monitor-starter:1.0.0'
```

### 2. Configure Your Application

Add the following to your `application.yml`:

```yaml
spring:
  error-monitor:
    enabled: true
    notification:
      platform: "slack" # Options: "slack", "teams", "both"
      
      # Slack Configuration
      slack:
        webhook-url: ${SLACK_WEBHOOK_URL}
        channel: "#alerts"
        username: "Error Monitor"
        icon-emoji: ":warning:"
        
      # Microsoft Teams Configuration  
      teams:
        webhook-url: ${TEAMS_WEBHOOK_URL}
        title: "Application Error Alert"
        theme-color: "FF0000"
```

### 3. That's It!

The library will automatically start monitoring your application for errors and send notifications to your configured platform.

## Configuration Reference

### Basic Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `spring.error-monitor.enabled` | Enable/disable error monitoring | `true` |
| `spring.error-monitor.notification.platform` | Notification platform | `slack` |

### Filtering Configuration

```yaml
spring:
  error-monitor:
    filtering:
      enabled-packages: ["com.yourcompany"]
      excluded-exceptions: ["java.lang.IllegalArgumentException"]
      minimum-severity: "ERROR"
```

### Rate Limiting

```yaml
spring:
  error-monitor:
    rate-limiting:
      max-errors-per-minute: 10
      burst-limit: 5
```

### Context Configuration

```yaml
spring:
  error-monitor:
    context:
      include-request-details: true
      include-stack-trace: true
      max-stack-trace-lines: 20
      mask-sensitive-data: true
```

### Metrics Configuration

```yaml
spring:
  error-monitor:
    metrics:
      enabled: true
      tags: ["service:my-app", "team:backend"]
```

### Analytics Configuration

```yaml
spring:
  error-monitor:
    analytics:
      enabled: true
      retention-period: "7d"
      aggregation-enabled: true
      trend-analysis-enabled: true
```

## Programmatic Usage

You can also report errors programmatically:

```java
@Autowired
private ErrorMonitor errorMonitor;

public void processData() {
    try {
        // your business logic
    } catch (Exception e) {
        errorMonitor.reportError("Data processing failed", e)
            .withContext("userId", getCurrentUserId())
            .withSeverity(ErrorSeverity.HIGH)
            .send();
    }
}
```

## Custom Configuration

### Custom Error Filter

```java
@Bean
public ErrorFilter customErrorFilter() {
    return new ErrorFilter() {
        @Override
        public boolean shouldReport(ErrorEvent event) {
            return !event.getException().getMessage().contains("benign");
        }
    };
}
```

### Custom Message Templates

#### Custom Slack Template
```java
@Bean
public SlackMessageTemplate customSlackTemplate() {
    return new SlackMessageTemplate() {
        @Override
        public SlackMessage buildMessage(ErrorEvent event) {
            return SlackMessage.builder()
                .text("🚨 Critical Error in " + event.getApplicationName())
                .channel("#critical-alerts")
                .build();
        }
    };
}
```

#### Custom Teams Template
```java
@Bean
public TeamsMessageTemplate customTeamsTemplate() {
    return new TeamsMessageTemplate() {
        @Override
        public TeamsMessage buildMessage(ErrorEvent event) {
            return TeamsMessage.builder()
                .title("🚨 " + event.getEnvironment() + " Error Alert")
                .themeColor("FF0000")
                .addSection(section -> section
                    .activityTitle(event.getException().getClass().getSimpleName())
                    .activitySubtitle(event.getApplicationName())
                    .addFact("Environment", event.getEnvironment())
                    .addFact("Severity", event.getSeverity().toString())
                    .addFact("Error Count", String.valueOf(event.getErrorCount()))
                    .text(event.getMessage()))
                .addAction("View Logs", "https://logs.company.com/" + event.getApplicationName())
                .build();
        }
    };
}
```

## Advanced Features

### Metrics with Micrometer

The library automatically collects metrics that can be exported to any monitoring system supported by Micrometer:

```java
// Access metrics via MeterRegistry
@Autowired
private MeterRegistry meterRegistry;

// Available metrics:
// - error.monitor.errors.total (Counter) - Total number of errors by type and severity
// - error.monitor.notifications (Counter) - Notification attempts by platform and status
// - error.monitor.processing.time (Timer) - Time taken to process errors
// - error.monitor.rate.limited (Counter) - Number of rate-limited errors
```

Example Prometheus queries:
```promql
# Error rate by type
rate(error_monitor_errors_total[5m])

# Notification success rate
rate(error_monitor_notifications_total{status="success"}[5m]) /
rate(error_monitor_notifications_total[5m])
```

### Health Checks

Health endpoints are automatically available via Spring Boot Actuator:

```bash
# Check error monitor health
GET /actuator/health/errorMonitor

# Check webhook connectivity
GET /actuator/health/notificationWebhooks
```

### Management Endpoints

Control and monitor the error monitor at runtime:

```bash
# View current status and statistics
GET /actuator/errorMonitor

# Temporarily disable/enable monitoring
POST /actuator/errorMonitor
{
  "enable": false
}

# Reset statistics
DELETE /actuator/errorMonitor

# View detailed error statistics
GET /actuator/errorStatistics

# Get statistics for specific error type
GET /actuator/errorStatistics/NullPointerException
```

### Error Analytics

Access advanced analytics programmatically:

```java
@Autowired
private ErrorAnalytics errorAnalytics;

// Get top error groups
List<ErrorGroup> topErrors = errorAnalytics.getTopErrorGroups(10);

// Analyze error trends
ErrorTrend trend = errorAnalytics.getErrorTrend(
    Instant.now().minus(1, ChronoUnit.HOURS),
    Instant.now()
);

if (trend.isSpike()) {
    // Handle error spike
}

// Get analytics summary
Map<String, Object> summary = errorAnalytics.getAnalyticsSummary();
```

## Platform-Specific Configuration

### Slack Integration

#### Setting Up Slack Webhook
1. Go to your Slack workspace settings
2. Navigate to "Apps" → "Custom Integrations" → "Incoming Webhooks"
3. Add a new webhook and copy the URL
4. Set it as `SLACK_WEBHOOK_URL` environment variable

#### Slack-Specific Configuration
```yaml
spring:
  error-monitor:
    notification:
      platform: "slack"
      slack:
        webhook-url: ${SLACK_WEBHOOK_URL}
        channel: "#alerts"          # Optional: Override default channel
        username: "Error Monitor"   # Optional: Custom bot name
        icon-emoji: ":warning:"     # Optional: Custom emoji icon
```

### Microsoft Teams Integration

#### Setting Up Teams Webhook
1. Open Microsoft Teams and navigate to the channel where you want to receive alerts
2. Click the "..." menu next to the channel name
3. Select "Connectors" → "Incoming Webhook"
4. Configure the webhook:
   - Give it a name (e.g., "Error Monitor")
   - Upload an icon (optional)
   - Click "Create"
5. Copy the webhook URL
6. Set it as `TEAMS_WEBHOOK_URL` environment variable

#### Teams-Specific Configuration
```yaml
spring:
  error-monitor:
    notification:
      platform: "teams"
      teams:
        webhook-url: ${TEAMS_WEBHOOK_URL}
        title: "Application Error Alert"    # Optional: Custom card title
        theme-color: "FF0000"              # Optional: Card accent color (hex)
```

### Using Both Platforms

You can send notifications to both Slack and Teams simultaneously:

```yaml
spring:
  error-monitor:
    notification:
      platform: "both"
      slack:
        webhook-url: ${SLACK_WEBHOOK_URL}
        channel: "#alerts"
      teams:
        webhook-url: ${TEAMS_WEBHOOK_URL}
        title: "Production Error"
```

## Message Format Examples

### Slack Message Format
Error notifications in Slack appear as rich formatted messages with:
- Header with error type and timestamp
- Contextual fields (application, environment, error type)
- Formatted stack trace in code blocks
- Request details (URL, method, user agent)

### Teams Message Format
Error notifications in Teams appear as adaptive cards with:
- Color-coded header (red for errors)
- Structured facts section with error details
- Expandable stack trace section
- Action buttons (optional, for viewing logs)

## Architecture & Design

### Key Design Decisions

- **Async Processing**: All notifications are sent asynchronously to avoid blocking application threads
- **Rate Limiting**: Built-in rate limiting prevents notification spam with configurable thresholds
- **Platform Detection**: Automatically detects Slack vs Teams from webhook URL format
- **Metrics Optional**: Micrometer integration is optional via conditional beans
- **Health Checks**: Provides detailed health status including webhook connectivity
- **Analytics Storage**: In-memory storage with configurable retention period

### Project Structure

```
io.github.nnegi88.errormonitor/
├── analytics/          # Error aggregation and trend analysis
├── config/            # Auto-configuration and properties
├── core/              # Core error monitoring logic
├── filter/            # Error filtering and rate limiting
├── health/            # Spring Boot health indicators
├── interceptor/       # Error interception mechanisms
├── management/        # Actuator endpoints
├── metrics/           # Micrometer metrics integration
├── model/             # Domain models
├── notification/      # Slack/Teams notification clients
│   ├── slack/
│   ├── teams/
│   └── template/
└── util/              # Utility classes
```

## Demo Application

Try out the error monitor with our interactive demo application:

```bash
# Clone and run the demo
cd spring-boot-error-monitor-demo
mvn spring-boot:run
```

Visit `http://localhost:8080` to:
- Trigger different types of errors
- See real-time error notifications
- Explore the monitoring dashboard
- Test various error scenarios

## Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/nnegi88/spring-boot-error-monitor-starter.git
cd spring-boot-error-monitor-starter

# Build the project
mvn clean install

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=ErrorMonitorHealthIndicatorTest

# Build without tests
mvn clean install -DskipTests
```

### Performance Benchmarks

Run the included performance benchmarks to verify minimal overhead:

```bash
cd spring-boot-error-monitor-benchmark
mvn exec:java -Dexec.mainClass="io.github.nnegi88.errormonitor.benchmark.BenchmarkRunner"
```

Key performance metrics:
- **Request overhead**: < 2ms (avg: 1.2ms)
- **Memory footprint**: ~35MB heap usage
- **Throughput**: Handles 10,000+ errors/minute
- **Non-blocking**: Zero impact on main request thread

### Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Troubleshooting

### Common Issues

1. **Notifications not being sent**
   - Check webhook URL is correctly configured
   - Verify rate limiting settings aren't too restrictive
   - Check application logs for error details
   - Use health endpoints to verify webhook connectivity

2. **High memory usage**
   - Adjust analytics retention period
   - Reduce error aggregation if not needed
   - Check for error loops causing excessive notifications

3. **Metrics not appearing**
   - Ensure Micrometer is on the classpath
   - Check metrics.enabled is true
   - Verify your metrics backend is configured

### Debug Mode

Enable debug logging to troubleshoot issues:

```yaml
logging:
  level:
    io.github.nnegi88.errormonitor: DEBUG
```

## Publishing to Maven Central

This project is configured for publication to Maven Central. For maintainers:

### Prerequisites
1. **GPG Setup**: Install GPG and generate key for `nnegi88@gmail.com`
2. **Central Portal Account**: Create account at https://central.sonatype.com
3. **Namespace Verification**: GitHub account `nnegi88` automatically grants `io.github.nnegi88` namespace
4. **Maven Configuration**: Configure `~/.m2/settings.xml` with Central Portal credentials

### Publication Commands
```bash
# Verify project is ready
mvn clean test
mvn clean verify -P release

# Deploy to Maven Central
mvn clean deploy -P release
```

### Usage After Publication
Once published, users can include the dependency:

```xml
<dependency>
    <groupId>io.github.nnegi88</groupId>
    <artifactId>spring-boot-error-monitor-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Verification
After publication, verify at:
- Maven Central: https://search.maven.org/artifact/io.github.nnegi88/spring-boot-error-monitor-starter
- Repository: https://repo1.maven.org/maven2/io/github/nnegi88/spring-boot-error-monitor-starter/

## Troubleshooting

### Common Issues

#### Teams Notifications Not Working
1. **Verify webhook URL**: Ensure the Teams webhook URL is correctly formatted and active
2. **Check firewall**: Ensure your application can reach Microsoft Teams endpoints
3. **Test webhook**: Try sending a test message directly to the webhook:
   ```bash
   curl -H "Content-Type: application/json" -d '{"text": "Test message"}' YOUR_TEAMS_WEBHOOK_URL
   ```
4. **Enable debug logging**:
   ```yaml
   logging:
     level:
       io.github.nnegi88.errormonitor: DEBUG
   ```

#### Slack Notifications Not Working
1. **Verify webhook URL**: Ensure the Slack webhook URL is valid
2. **Check channel**: Verify the channel exists and the webhook has access
3. **Test webhook**: Send a test message:
   ```bash
   curl -X POST -H 'Content-type: application/json' --data '{"text":"Test message"}' YOUR_SLACK_WEBHOOK_URL
   ```

#### Rate Limiting Issues
- If notifications are being dropped, check your rate limiting configuration
- Use the `/actuator/errorStatistics` endpoint to monitor rate limiting

#### Platform Auto-Detection
The library auto-detects the platform from webhook URL:
- Slack: URLs containing `hooks.slack.com`
- Teams: URLs containing `webhook.office.com` or `outlook.office.com`

If auto-detection fails, explicitly set the platform:
```yaml
spring:
  error-monitor:
    notification:
      platform: "teams"  # or "slack" or "both"
```

## Security

For security issues, please see our [Security Policy](SECURITY.md). Never expose webhook URLs in public repositories or logs.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built with ❤️ for the Spring Boot community
- Inspired by the need for better error visibility in production
- Thanks to all [contributors](https://github.com/nnegi88/spring-boot-error-monitor-starter/graphs/contributors)