# GitHub Actions Workflows

This directory contains the CI/CD workflows for the Spring Boot Error Monitor Starter project.

## Workflows

### CI Workflow (`ci.yml`)

Runs on every push and pull request to `main` and `develop` branches.

**Jobs:**
- **Test**: Runs tests with multiple Java versions (11, 17)
- **Build**: Compiles and packages the project

**Important:** GPG signing is disabled in CI builds using `-Dgpg.skip=true` flag.

### Release Workflow (`release.yml`)

Automated workflow for releasing to Maven Central with multiple trigger options.

**Triggers:**
1. **Automatic on push to main**: Releases if version is SNAPSHOT
2. **Manual dispatch**: Specify exact release and next versions
3. **GitHub Release creation**: Publishes existing release version

**Automated Release Process:**
When you push to `main` with a SNAPSHOT version (e.g., `1.0.0-SNAPSHOT`):
1. Automatically removes `-SNAPSHOT` and creates release `1.0.0`
2. Tags the release as `v1.0.0`
3. Deploys to Maven Central
4. Updates to next SNAPSHOT version (e.g., `1.0.1-SNAPSHOT`)
5. Creates GitHub Release with notes

**Manual Release Process:**
Use workflow dispatch to specify exact versions:
- Release Version: `1.2.0`
- Next Version: `1.3.0-SNAPSHOT`

**Required Secrets:**
- `OSSRH_USERNAME`: Sonatype OSSRH username
- `OSSRH_TOKEN`: Sonatype OSSRH token
- `MAVEN_GPG_PRIVATE_KEY`: GPG private key for signing artifacts
- `MAVEN_GPG_PASSPHRASE`: GPG key passphrase

### Snapshot Workflow (`snapshot.yml`)

Deploys SNAPSHOT versions to OSSRH Snapshots repository.

**Trigger:** Push to `develop` branch

**Process:**
1. Verifies version is SNAPSHOT
2. Runs tests
3. Deploys to OSSRH Snapshots
4. Comments on PR with snapshot usage instructions

## Setup Instructions

### For CI Workflow
No additional setup needed. The workflow runs automatically.

### For Automated Release Workflow

1. **Generate GPG Key:**
   ```bash
   gpg --gen-key
   gpg --list-secret-keys --keyid-format=long
   gpg --export-secret-keys -a "nnegi88@gmail.com" > private.key
   ```

2. **Upload GPG Key to Servers:**
   ```bash
   gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
   gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
   ```

3. **Add GitHub Secrets:**
   - Go to Settings → Secrets and variables → Actions
   - Add the following secrets:
     - `OSSRH_USERNAME`: Your Sonatype username
     - `OSSRH_TOKEN`: Your Sonatype token (not password)
     - `MAVEN_GPG_PRIVATE_KEY`: Content of private.key file
     - `MAVEN_GPG_PASSPHRASE`: Your GPG key passphrase

4. **Set Up Branch Protection (Optional but Recommended):**
   - Protect `main` branch
   - Require PR reviews before merging
   - Require status checks to pass

### Release Strategies

#### Strategy 1: Fully Automated (Recommended)
1. Develop on feature branches
2. Merge to `main` with SNAPSHOT version
3. Workflow automatically releases and updates version

#### Strategy 2: Manual Control
1. Use workflow dispatch from Actions tab
2. Specify exact versions
3. Useful for hotfixes or specific versioning needs

#### Strategy 3: Tag-Based
1. Create and push a tag (e.g., `v1.0.0`)
2. Create GitHub Release from tag
3. Workflow deploys the tagged version

## Version Management

### Version Conventions
- Release versions: `1.0.0`, `1.1.0`, `2.0.0`
- Snapshot versions: `1.0.0-SNAPSHOT`, `1.1.0-SNAPSHOT`
- Version bumps on release:
  - Patch: `1.0.0` → `1.0.1-SNAPSHOT`
  - Minor: `1.0.0` → `1.1.0-SNAPSHOT` (manual)
  - Major: `1.0.0` → `2.0.0-SNAPSHOT` (manual)

### Changing Versions Manually
```bash
# Set specific version
mvn versions:set -DnewVersion=1.2.0-SNAPSHOT

# Commit the change
git add pom.xml
git commit -m "Prepare version 1.2.0-SNAPSHOT"
git push origin main
```

## Troubleshooting

### Release Workflow Not Triggering
- Check if version in `pom.xml` is SNAPSHOT
- Verify you're pushing to `main` branch
- Check workflow permissions in Settings

### GPG Signing Failures
- Ensure GPG key is not expired
- Verify key email matches `<keyname>` in pom.xml
- Check GPG_PASSPHRASE secret is correct

### Maven Central Sync Delays
- Initial sync can take 2-4 hours
- Subsequent releases sync within 30 minutes
- Check: https://repo1.maven.org/maven2/io/github/nnegi88/

## Notes

- The CI workflow skips GPG signing to avoid requiring secrets for every build
- The release workflow handles all GPG signing and Maven Central deployment
- Snapshot deployments go to OSSRH snapshots repository
- Release commits are prefixed with `[maven-release]` to prevent loops