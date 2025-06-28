# Using Spring Boot Error Monitor Starter

## Installation

### Maven Central (Primary Distribution)

This library is available on Maven Central. Simply add the dependency to your project:

#### Maven
```xml
<dependency>
    <groupId>io.github.nnegi88</groupId>
    <artifactId>spring-boot-error-monitor-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

#### Gradle
```gradle
implementation 'io.github.nnegi88:spring-boot-error-monitor-starter:1.0.1'
```

No additional repository configuration is needed as Maven Central is included by default in most build tools.

### Alternative: JitPack

If you need to use a specific commit or branch:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.nnegi88</groupId>
    <artifactId>spring-boot-error-monitor-starter</artifactId>
    <version>v1.0.1</version> <!-- or use commit hash -->
</dependency>
```

### Alternative: Local JAR

For offline usage, download the JAR from [GitHub Releases](https://github.com/nnegi88/spring-boot-error-monitor-starter/releases):

```xml
<dependency>
    <groupId>io.github.nnegi88</groupId>
    <artifactId>spring-boot-error-monitor-starter</artifactId>
    <version>1.0.1</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/spring-boot-error-monitor-starter-1.0.1.jar</systemPath>
</dependency>
```

## When to Build

You only need to build when:
1. **Making changes** to the library
2. **Testing locally** before publishing
3. **Contributing** to the project

## Automated Publishing

Every push to `main` automatically:
1. Builds the library
2. Runs tests
3. Creates GitHub Release
4. Publishes to Maven Central

No manual building required!

### Snapshot Versions

For development snapshots, use:

```xml
<dependency>
    <groupId>io.github.nnegi88</groupId>
    <artifactId>spring-boot-error-monitor-starter</artifactId>
    <version>1.0.2-SNAPSHOT</version>
</dependency>
```

Note: You'll need to add the OSSRH snapshot repository:

```xml
<repositories>
    <repository>
        <id>ossrh-snapshots</id>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

## For Development

If you're actively developing:

```bash
# One-time setup
mvn clean install

# Use file watching for auto-rebuild
mvn spring-boot:run -Dspring-boot.run.fork=false

# Or use your IDE's auto-build feature
```

## Performance Tips

1. **Use Dependency Caching**: Your IDE caches dependencies
2. **Incremental Builds**: Only changed files are recompiled
3. **Multi-module Projects**: Set up as a module dependency
4. **CI/CD**: Let GitHub Actions do the building

## Summary

- **End Users**: Never need to build - just add dependency
- **Your Team**: Use Maven Central - automatic updates
- **Public Users**: Use Maven Central or JitPack
- **You**: Only build when developing new features