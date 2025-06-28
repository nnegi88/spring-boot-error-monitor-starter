# Security Policy

## Supported Versions

We actively support the following versions of Spring Boot Error Monitor Starter with security updates:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

The Spring Boot Error Monitor Starter team takes security vulnerabilities seriously. We appreciate your efforts to responsibly disclose your findings.

### How to Report

**Please do NOT create a public GitHub issue for security vulnerabilities.**

Instead, please report security vulnerabilities through one of these methods:

1. **GitHub Security Advisories (Preferred)**
   - Go to [Security Advisories](https://github.com/nnegi88/spring-boot-error-monitor-starter/security/advisories/new)
   - Click "New draft security advisory"
   - Fill in the details of the vulnerability

2. **Email**
   - Send an email to: [nnegi88@gmail.com](mailto:nnegi88@gmail.com)
   - Include "SECURITY" in the subject line
   - Provide detailed information about the vulnerability

### What to Include

Please include the following information in your security report:

- **Description**: A clear description of the vulnerability
- **Impact**: What an attacker could potentially do with this vulnerability
- **Steps to Reproduce**: Detailed steps to reproduce the issue
- **Affected Versions**: Which versions are affected
- **Environment**: Java version, Spring Boot version, platform details
- **Proof of Concept**: If available, include a minimal example demonstrating the issue

### Response Timeline

- **Initial Response**: We will acknowledge receipt of your report within 48 hours
- **Investigation**: We will investigate and validate the report within 5 business days
- **Fix Development**: Critical vulnerabilities will be prioritized for immediate fixes
- **Disclosure**: We will coordinate with you on responsible disclosure timing

### Security Update Process

1. **Assessment**: We evaluate the severity and impact
2. **Fix Development**: We develop and test a security fix
3. **Release**: We release a patched version
4. **Advisory**: We publish a security advisory with details
5. **Recognition**: We credit the reporter (if desired)

## Security Best Practices

When using Spring Boot Error Monitor Starter, please follow these security best practices:

### Webhook URL Security
- **Never expose webhook URLs** in public repositories or logs
- Use environment variables or secure configuration management
- Rotate webhook URLs periodically
- Monitor webhook usage for suspicious activity

### Configuration Security
```yaml
# ✅ Good - Use environment variables
spring:
  error-monitor:
    notification:
      slack:
        webhook-url: "${SLACK_WEBHOOK_URL}"
      teams:
        webhook-url: "${TEAMS_WEBHOOK_URL}"

# ❌ Bad - Never hardcode webhook URLs
spring:
  error-monitor:
    notification:
      slack:
        webhook-url: "https://hooks.slack.com/services/..." # Don't do this!
```

### Error Information Security
- Review error templates to avoid leaking sensitive data
- Configure appropriate error filtering to prevent PII exposure
- Use rate limiting to prevent information disclosure attacks
- Monitor error patterns for potential security events

### Network Security
- Use HTTPS webhook URLs only
- Implement proper firewall rules for outbound connections
- Consider using webhook validation where supported
- Monitor network traffic to notification endpoints

## Known Security Considerations

### Information Disclosure
- Error messages may contain sensitive information
- Stack traces can reveal internal application structure
- Request parameters might include sensitive data

**Mitigation**: Configure error filtering and customize templates to exclude sensitive information.

### Rate Limiting Bypass
- High-frequency errors could potentially bypass rate limiting
- Malicious actors might trigger error conditions intentionally

**Mitigation**: Implement appropriate rate limiting and monitoring.

### Webhook Security
- Webhook URLs act as authentication tokens
- Compromised webhooks can lead to spam or information disclosure

**Mitigation**: Secure webhook URL storage and implement webhook rotation.

## Security Updates

Security updates will be released as patch versions (e.g., 1.0.1, 1.0.2) and will be clearly marked in the release notes. 

Subscribe to our releases to stay informed about security updates:
- Watch this repository for releases
- Monitor our [CHANGELOG.md](CHANGELOG.md) for security-related changes

## Contact

For general security questions or concerns, please contact:
- Email: [nnegi88@gmail.com](mailto:nnegi88@gmail.com)
- Security Advisory: [GitHub Security Advisories](https://github.com/nnegi88/spring-boot-error-monitor-starter/security/advisories)

Thank you for helping keep Spring Boot Error Monitor Starter secure!