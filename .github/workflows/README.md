# GitHub Actions Workflows

This directory contains the CI/CD workflows for the Spring Boot Error Monitor Starter project.

## Workflows

### CI Workflow (`ci.yml`)

Runs on every push and pull request to `main` and `develop` branches.

**Jobs:**
- **Test**: Runs tests with multiple Java versions (17, 21) and Spring Boot versions (3.1.x, 3.2.x)
- **Build**: Compiles and packages the project
- **Code Quality**: Runs code quality checks (Checkstyle, SpotBugs, formatting)

**Important:** GPG signing is disabled in CI builds using `-Dgpg.skip=true` flag.

### Release Workflow (`release.yml`)

Manual workflow for releasing to Maven Central.

**Trigger:** Manual dispatch with release version and next development version inputs.

**Steps:**
1. Updates POM to release version
2. Creates git tag
3. Builds and signs artifacts with GPG
4. Deploys to Maven Central
5. Updates POM to next development version
6. Creates GitHub release

**Required Secrets:**
- `OSSRH_USERNAME`: Sonatype OSSRH username
- `OSSRH_TOKEN`: Sonatype OSSRH token
- `MAVEN_GPG_PRIVATE_KEY`: GPG private key for signing artifacts
- `MAVEN_GPG_PASSPHRASE`: GPG key passphrase

## Setup Instructions

### For CI Workflow
No additional setup needed. The workflow runs automatically.

### For Release Workflow

1. **Generate GPG Key:**
   ```bash
   gpg --gen-key
   gpg --list-secret-keys --keyid-format=long
   gpg --export-secret-keys -a "your-email@example.com" > private.key
   ```

2. **Add GitHub Secrets:**
   - Go to Settings → Secrets and variables → Actions
   - Add the following secrets:
     - `OSSRH_USERNAME`: Your Sonatype username
     - `OSSRH_TOKEN`: Your Sonatype token (not password)
     - `MAVEN_GPG_PRIVATE_KEY`: Content of private.key file
     - `MAVEN_GPG_PASSPHRASE`: Your GPG key passphrase

3. **Trigger Release:**
   - Go to Actions → Release workflow
   - Click "Run workflow"
   - Enter release version (e.g., "1.0.0")
   - Enter next development version (e.g., "1.1.0-SNAPSHOT")

## Notes

- The CI workflow skips GPG signing to avoid requiring secrets for every build
- The release workflow handles all GPG signing and Maven Central deployment
- Code quality checks are set to not fail the build (`|| true`) as they're informational
- Test reports are generated even if tests fail for better debugging