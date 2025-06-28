# Using Spring Boot Error Monitor Starter

## Quick Usage Options

### Option 1: GitHub Packages (Recommended)

Once published, your team can use it without building:

#### 1. Add to your `~/.m2/settings.xml`:
```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

#### 2. Add repository to your project's `pom.xml`:
```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/nnegi88/spring-boot-error-monitor-starter</url>
  </repository>
</repositories>
```

#### 3. Add dependency:
```xml
<dependency>
    <groupId>io.github.nnegi88</groupId>
    <artifactId>spring-boot-error-monitor-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Option 2: JitPack (Public, No Auth Required)

Users can use your library directly from GitHub without any authentication:

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
    <version>v1.0.0</version> <!-- or use commit hash -->
</dependency>
```

### Option 3: Local JAR (Quick & Dirty)

Download the JAR from GitHub Releases and add directly:

```xml
<dependency>
    <groupId>io.github.nnegi88</groupId>
    <artifactId>spring-boot-error-monitor-starter</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/spring-boot-error-monitor-starter-1.0.0.jar</systemPath>
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
4. Publishes to GitHub Packages

No manual building required!

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
- **Your Team**: Use GitHub Packages - automatic updates
- **Public Users**: Use JitPack - zero configuration
- **You**: Only build when developing new features