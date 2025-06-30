# Release Notes

## [1.0.2] - 2024-01-30

### 🎉 What's New
This patch release focuses on bug fixes and improving the reliability of error notifications. All users are encouraged to upgrade to this version for better stability.

### 🐛 Bug Fixes
- **Fixed Log Level Filtering**: Corrected inverted comparison logic that was causing INFO and DEBUG logs to trigger notifications even when minimum level was set to ERROR
- **Fixed Webhook URL Delivery**: Resolved issue where webhook URLs weren't being passed to notification services, causing "Target host is not specified" errors
- **Fixed Slack Message Format**: Corrected Slack Block Kit JSON structure to prevent "invalid_blocks" errors
- **Property Placeholder Resolution**: Spring property placeholders (like `${spring.application.name}`) now properly resolve to actual values instead of showing literal placeholder strings

### 📝 Documentation
- Updated README.md with complete and accurate configuration properties
- Added documentation for rate limiting configuration options
- Improved troubleshooting section with property placeholder resolution details

### 🔧 Technical Details
- Modified `NotificationOrchestrator.shouldSendNotification()` to use correct ordinal comparison
- Enhanced `SolidNotificationAutoConfiguration` and `LogbackAppenderConfiguration` with Spring Environment integration
- Updated `SlackMessage` class to include required `emoji` property in Text objects

### 💡 Upgrade Notes
No breaking changes. Simply update your dependency version to 1.0.2.

---

## [1.0.1] - 2024-01-29

### Fixed
- Various stability improvements and minor bug fixes
- Enhanced error handling in HTTP clients

---

## [1.0.0] - 2024-01-28

### 🚀 Initial Release

We're excited to announce the first stable release of Spring Boot Error Monitor Starter!

### ✨ Key Features

#### Core Functionality
- **Automatic Error Detection**: Seamlessly integrates with Logback to capture errors
- **Multi-Platform Support**: Send notifications to Slack and Microsoft Teams
- **Zero-Code Integration**: Works out of the box with Spring Boot applications
- **Smart Filtering**: Configure minimum log levels to reduce noise
- **Rich Context**: Includes stack traces, MDC properties, and application metadata

#### Architecture & Design
- **SOLID Principles**: Clean architecture with proper separation of concerns
- **Domain-Driven Design**: Ports and adapters pattern for extensibility
- **Spring Boot Native**: Full auto-configuration support
- **Async Processing**: Non-blocking notification delivery
- **Rate Limiting**: Built-in protection against notification flooding

#### Configuration Options
- Customizable application name and environment labels
- Configurable timeouts and queue sizes
- Optional stack trace inclusion
- Per-platform minimum log levels

### 📋 Requirements
- Java 11 or higher
- Spring Boot 2.3 or higher
- Valid Slack or Teams webhook URLs

### 🚀 Quick Start
Add the dependency and configure your webhook URL - that's it! Check the README for detailed configuration options.

### 🙏 Acknowledgments
Thanks to all contributors who helped shape this initial release with feedback and testing.