# Release Notes

## [1.0.1] - 2025-06-28

### Fixed
- Maven Central deployment configuration
- Authentication issues with Sonatype Central Portal
- Removed GitHub Packages distribution
- CI/CD workflow optimizations

### Changed
- Upgraded central-publishing-maven-plugin from v0.4.0 to v0.8.0
- Simplified deployment process to use only Maven Central
- Updated documentation to reflect latest version

### Removed
- Snapshot deployment to public repositories (not supported by Central Portal)
- GitHub Packages configuration
- Unnecessary scripts directory

## [1.0.0] - 2025-06-27

### Initial Release
- Core error monitoring functionality for Spring Boot applications
- Multi-platform notification support (Slack & Microsoft Teams)
- Zero-code integration with automatic error detection
- Configurable error filtering and rate limiting
- Rich error context with stack traces and request details
- Asynchronous processing for minimal performance impact
- Built-in security features for sensitive data masking
- Metrics collection with Micrometer integration
- Health check endpoints for monitoring
- Advanced error analytics and trend detection
- Management endpoints via Spring Boot Actuator

### Features
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