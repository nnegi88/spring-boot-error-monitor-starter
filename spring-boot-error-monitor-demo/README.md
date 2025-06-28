# Spring Boot Error Monitor Demo Application

This demo application showcases all features of the Spring Boot Error Monitor library, including error detection, notification delivery to Slack/Teams, metrics collection, and health monitoring.

## Features Demonstrated

### 1. Error Detection & Notification
- Runtime exceptions (NullPointer, ArrayIndex, Arithmetic)
- Business exceptions (ProductNotFound, InsufficientStock)
- External service failures
- Async operation errors
- Scheduled task failures

### 2. Multi-Platform Support
- Slack notifications with formatted messages
- Microsoft Teams notifications with adaptive cards
- Platform auto-detection from webhook URL

### 3. Advanced Features
- Rate limiting to prevent notification spam
- Error aggregation and analytics
- Metrics collection with Micrometer
- Health indicators and monitoring
- Management endpoints via Spring Actuator

### 4. Interactive Demo UI
- Web interface to trigger various error scenarios
- Real-time response display
- Product management operations
- Monitoring dashboard

## Quick Start

### Prerequisites
- Java 11 or higher
- Maven 3.6+
- Slack webhook URL (optional)
- Microsoft Teams webhook URL (optional)

### Installation

1. **Build the main library first:**
   ```bash
   cd ../
   mvn clean install -DskipTests
   ```

2. **Run the demo application:**
   ```bash
   cd spring-boot-error-monitor-demo
   mvn spring-boot:run
   ```

3. **Access the demo UI:**
   - Main Demo: http://localhost:8080
   - H2 Console: http://localhost:8080/h2-console
   - Dashboard: http://localhost:8080/dashboard

### Configuration

#### Option 1: Using Environment Variables
```bash
export ERROR_MONITOR_PLATFORM=slack
export SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
export TEAMS_WEBHOOK_URL=https://outlook.office.com/webhook/YOUR/WEBHOOK/URL

mvn spring-boot:run
```

#### Option 2: Using application.yml
Edit `src/main/resources/application.yml`:
```yaml
spring:
  error-monitor:
    notification:
      platform: "slack"  # or "teams" or "both"
      slack:
        webhook-url: "YOUR_SLACK_WEBHOOK_URL"
      teams:
        webhook-url: "YOUR_TEAMS_WEBHOOK_URL"
```

#### Option 3: Using Command Line Arguments
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.error-monitor.notification.slack.webhook-url=YOUR_URL"
```

## Demo Scenarios

### 1. Basic Error Testing

Navigate to http://localhost:8080 and try:

- **Runtime Exceptions**: Click buttons to trigger NullPointer, ArrayIndex, etc.
- **Business Errors**: Test product operations that fail with business logic errors
- **Async Errors**: Trigger errors in async operations and scheduled tasks

### 2. Product API Testing

Use the Product Operations section or direct API calls:

```bash
# List all products
curl http://localhost:8080/api/products

# Get non-existent product (triggers error)
curl http://localhost:8080/api/products/999

# Create product with validation error
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"","price":-100}'

# Purchase with insufficient stock
curl -X POST http://localhost:8080/api/products/1/purchase?quantity=1000
```

### 3. Error Scenarios API

Direct error triggering endpoints:

```bash
# Null Pointer Exception
curl http://localhost:8080/api/demo/errors/null-pointer

# Random error
curl http://localhost:8080/api/demo/errors/random

# Validation error
curl -X POST http://localhost:8080/api/demo/errors/validation-error \
  -H "Content-Type: application/json" \
  -d '{}'

# Slow operation timeout
curl http://localhost:8080/api/demo/errors/slow-operation?delayMs=5000
```

### 4. Scheduled Tasks

The application runs several scheduled tasks that randomly fail:
- **Data Sync**: Every 30 seconds (20% failure rate)
- **Health Check**: Every minute (10% critical failure)
- **Report Generation**: Every 5 minutes (15% failure rate)
- **Cleanup**: Every 45 seconds (various error types)

### 5. Monitoring Endpoints

Access Spring Boot Actuator endpoints:

- **Health Status**: http://localhost:8080/actuator/health
- **Error Monitor Status**: http://localhost:8080/actuator/errorMonitor
- **Error Statistics**: http://localhost:8080/actuator/errorStatistics
- **Application Metrics**: http://localhost:8080/actuator/metrics

### 6. Rate Limiting Demo

The demo is configured with:
- Max 30 errors per minute
- Burst limit of 10 errors

To test rate limiting:
```bash
# Trigger many errors quickly
for i in {1..50}; do
  curl http://localhost:8080/api/demo/errors/random &
done
```

Watch the notifications - after the burst limit, errors will be filtered.

## Expected Notifications

### Slack Message Format
```
🚨 Application Error Alert
━━━━━━━━━━━━━━━━━━━━━
Application: error-monitor-demo
Environment: default
Error Type: NullPointerException
Timestamp: 2025-12-28 10:30:45

Request Details:
• URL: /api/demo/errors/null-pointer
• Method: GET
• IP: 127.0.0.1

Stack Trace:
java.lang.NullPointerException
  at DemoErrorController.triggerNullPointer
```

### Teams Message Format
```
🚨 Demo Application Error
━━━━━━━━━━━━━━━━━━━━
Application: error-monitor-demo
Error Type: NullPointerException
Timestamp: 2025-12-28 10:30:45
Request URL: /api/demo/errors/null-pointer

[View Logs] button
```

## Troubleshooting

### No Notifications Received
1. Check webhook URL is correct
2. Verify platform configuration matches webhook type
3. Check application logs for webhook errors
4. Ensure rate limiting isn't filtering errors

### Memory Issues
The demo includes a memory leak simulation endpoint. Use carefully and restart if needed.

### Database Connection
H2 in-memory database is used. Access console at /h2-console with:
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave empty)

## Architecture Overview

```
Demo Application
├── Controllers
│   ├── DemoErrorController (Error scenarios)
│   ├── ProductController (Business operations)
│   └── DemoUIController (Web interface)
├── Services
│   ├── ProductService (Business logic)
│   └── ScheduledTaskService (Async errors)
├── Models
│   └── Product (JPA entity)
└── Configuration
    └── application.yml (Error monitor config)
```

## Customization

### Adding New Error Scenarios

1. Add endpoint to `DemoErrorController`:
```java
@GetMapping("/custom-error")
public String triggerCustomError() {
    throw new CustomException("Your error message");
}
```

2. Add button to `index.html`:
```html
<button class="btn btn-danger" onclick="triggerError('custom-error')">
    Custom Error
</button>
```

### Modifying Notification Templates

Create custom templates in your configuration:

```java
@Bean
public SlackMessageTemplate customSlackTemplate() {
    return new SlackMessageTemplate() {
        @Override
        public SlackMessage buildMessage(ErrorEvent event) {
            // Custom formatting
        }
    };
}
```

## Load Testing

Use the included endpoints for load testing:

```bash
# Generate sustained load
while true; do
  curl http://localhost:8080/api/demo/errors/random
  sleep 0.1
done

# Burst test
seq 1 100 | xargs -P 10 -I {} curl http://localhost:8080/api/demo/errors/random
```

## Next Steps

1. **Configure Real Webhooks**: Replace placeholder URLs with actual Slack/Teams webhooks
2. **Monitor Metrics**: Use Prometheus or other tools to scrape `/actuator/metrics`
3. **Customize Error Filters**: Add custom filtering logic for your use case
4. **Production Deployment**: Adjust rate limits and retention periods for production

## Support

For issues or questions about the Error Monitor library:
- GitHub: [spring-boot-error-monitor-starter](https://github.com/your-org/spring-boot-error-monitor-starter)
- Documentation: See main project README.md