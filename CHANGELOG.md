# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.2] - 2024-01-30

### Added
- 🔧 Spring property placeholder resolution support
  - Automatically resolves placeholders like `${spring.application.name:Unknown}`
  - Uses Spring's `Environment.resolvePlaceholders()` for proper value resolution

### Fixed
- 🐛 Fixed inverted log level comparison logic in `NotificationOrchestrator`
  - Changed from `>` to `<` for proper filtering (e.g., ERROR level now correctly filters out INFO, WARN, DEBUG)
- 🐛 Fixed webhook URL not being passed to notification services
  - Added webhook URL to metadata in `NotificationOrchestrator.enrichMessage()`
- 🐛 Fixed Slack Block Kit JSON format issues
  - Added missing `emoji` property to Text class
  - Fixed null text handling in section blocks

### Changed
- 📝 Updated README.md with accurate feature list and configuration properties
- 📝 Added documentation for rate limiting properties

## [1.0.1] - 2024-01-29

### Fixed
- 🐛 Various bug fixes and stability improvements

## [1.0.0] - 2024-01-28

### Added
- 🚨 Automatic error detection and reporting via Logback integration
- 📱 Multi-platform support (Slack & Microsoft Teams)
- 🔧 Zero-code integration with Spring Boot applications
- 🎯 Configurable error filtering by log level
- 📊 Rich error context including stack traces and MDC properties
- ⚡ Asynchronous processing for minimal performance impact
- 🛡️ Rate limiting to prevent notification flooding
- 🏗️ SOLID architecture with clean separation of concerns
- 📦 Spring Boot auto-configuration support
- 🔌 Extensible design for adding new notification platforms

### Technical Details
- Java 11+ compatibility
- Spring Boot 2.3+ support
- RestTemplate for HTTP communication (no WebFlux dependency)
- Domain-driven design with ports and adapters
- Comprehensive unit and integration tests

### Configuration
- Slack webhook integration with Block Kit formatting
- Teams webhook integration with Adaptive Cards
- Configurable log levels, timeouts, and queue sizes
- Environment and application name support
- Optional stack trace inclusion

### Documentation
- Complete README with examples
- Architecture documentation
- Configuration reference
- Troubleshooting guide