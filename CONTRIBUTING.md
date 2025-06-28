# Contributing to Spring Boot Error Monitor Starter

First off, thank you for considering contributing to Spring Boot Error Monitor Starter! It's people like you that make this project better for everyone.

## Code of Conduct

This project and everyone participating in it is governed by our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the [existing issues](https://github.com/nnegi88/spring-boot-error-monitor-starter/issues) to see if the problem has already been reported. When you are creating a bug report, please include as many details as possible using our [bug report template](.github/ISSUE_TEMPLATE/bug_report.yml).

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. Use our [feature request template](.github/ISSUE_TEMPLATE/feature_request.yml) to suggest new features.

### Your First Code Contribution

Unsure where to begin contributing? You can start by looking through these `good-first-issue` and `help-wanted` issues:

- [Good first issues](https://github.com/nnegi88/spring-boot-error-monitor-starter/labels/good%20first%20issue) - issues which should only require a few lines of code
- [Help wanted issues](https://github.com/nnegi88/spring-boot-error-monitor-starter/labels/help%20wanted) - issues which should be a bit more involved

## Development Environment Setup

### Prerequisites

- **Java 17 or higher** (Java 21 recommended)
- **Maven 3.8+**
- **Git**
- **IDE** (IntelliJ IDEA, Eclipse, or VS Code recommended)

### Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/spring-boot-error-monitor-starter.git
   cd spring-boot-error-monitor-starter
   ```

3. **Set up the upstream remote**:
   ```bash
   git remote add upstream https://github.com/nnegi88/spring-boot-error-monitor-starter.git
   ```

4. **Build the project**:
   ```bash
   mvn clean install
   ```

5. **Run the tests**:
   ```bash
   mvn test
   ```

### Project Structure

```
spring-boot-error-monitor-starter/
├── src/main/java/io/github/nnegi88/errormonitor/
│   ├── analytics/          # Error aggregation and trend analysis
│   ├── config/            # Auto-configuration and properties
│   ├── core/              # Core error monitoring logic
│   ├── filter/            # Error filtering and rate limiting
│   ├── health/            # Spring Boot health indicators
│   ├── interceptor/       # Error interception mechanisms
│   ├── management/        # Actuator endpoints
│   ├── metrics/           # Micrometer metrics integration
│   ├── model/             # Domain models
│   ├── notification/      # Slack/Teams notification clients
│   └── util/              # Utility classes
├── src/test/java/         # Unit and integration tests
├── spring-boot-error-monitor-demo/     # Demo application
└── spring-boot-error-monitor-benchmark/ # Performance benchmarks
```

## Development Guidelines

### Code Style

We follow these coding standards:

1. **Java Code Style**:
   - Use Java 17+ features where appropriate
   - Follow standard Java naming conventions
   - Use meaningful variable and method names
   - Keep methods small and focused
   - Add Javadoc for public APIs

2. **Spring Boot Conventions**:
   - Use `@ConfigurationProperties` for configuration
   - Implement proper auto-configuration with conditions
   - Follow Spring Boot's conditional bean registration patterns
   - Use appropriate Spring annotations

3. **Testing**:
   - Write unit tests for all new functionality
   - Use meaningful test method names
   - Follow AAA pattern (Arrange, Act, Assert)
   - Mock external dependencies
   - Achieve good test coverage

### Making Changes

1. **Create a branch** for your feature or bug fix:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/issue-number-description
   ```

2. **Make your changes** following the coding guidelines

3. **Write or update tests** for your changes

4. **Run the test suite**:
   ```bash
   mvn test
   ```

5. **Run integration tests**:
   ```bash
   mvn verify
   ```

6. **Check code formatting** (if available):
   ```bash
   mvn spring-javaformat:apply
   ```

### Commit Messages

Write clear, descriptive commit messages:

```
feat: add support for custom error templates

- Add template engine for customizing error notifications
- Support for both Slack and Teams custom templates
- Include template validation and fallback mechanisms

Fixes #123
```

Use conventional commit format:
- `feat:` for new features
- `fix:` for bug fixes
- `docs:` for documentation changes
- `test:` for test improvements
- `refactor:` for code refactoring
- `perf:` for performance improvements

### Testing

#### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ErrorMonitorHealthIndicatorTest

# Run specific test method
mvn test -Dtest=ErrorTrendAnalyzerTest#testSpikeDetection

# Run with coverage
mvn test jacoco:report
```

#### Test Categories

1. **Unit Tests**: Fast, isolated tests for individual classes
2. **Integration Tests**: Tests that verify component integration
3. **Configuration Tests**: Tests for Spring Boot auto-configuration

#### Test Guidelines

- Use `@MockBean` for Spring context tests
- Use `@Mock` for pure unit tests
- Create test fixtures in `src/test/resources`
- Use meaningful assertions with descriptive messages
- Test both success and failure scenarios

### Performance Testing

Run performance benchmarks:

```bash
cd spring-boot-error-monitor-benchmark
mvn exec:java -Dexec.mainClass="io.github.nnegi88.errormonitor.benchmark.BenchmarkRunner"
```

### Demo Application

Test your changes with the demo application:

```bash
cd spring-boot-error-monitor-demo
mvn spring-boot:run
```

Visit `http://localhost:8080` to interact with the demo interface.

## Pull Request Process

1. **Update documentation** if needed (README.md, CHANGELOG.md)
2. **Ensure all tests pass** and maintain good coverage
3. **Update the CHANGELOG.md** with your changes
4. **Submit a pull request** using our [PR template](.github/pull_request_template.md)

### Pull Request Checklist

- [ ] Code follows the project's style guidelines
- [ ] Self-review of code has been performed
- [ ] Code is commented, particularly hard-to-understand areas
- [ ] Corresponding changes to documentation have been made
- [ ] Tests have been added that prove the fix is effective or feature works
- [ ] All new and existing tests pass
- [ ] Any dependent changes have been merged and published

## Issue and Pull Request Labels

We use these labels to organize work:

- `bug` - Something isn't working
- `enhancement` - New feature or request
- `documentation` - Improvements or additions to documentation
- `good first issue` - Good for newcomers
- `help wanted` - Extra attention is needed
- `question` - Further information is requested
- `wontfix` - This will not be worked on

## Getting Help

If you need help, you can:

1. **Create a question issue** using our [question template](.github/ISSUE_TEMPLATE/question.yml)
2. **Check the documentation** in README.md and demo application
3. **Look at existing code** for patterns and examples
4. **Review test cases** for usage examples

## Recognition

Contributors will be recognized in:
- Project README.md
- Release notes for their contributions
- GitHub contributors page

Thank you for contributing to Spring Boot Error Monitor Starter! 🚀