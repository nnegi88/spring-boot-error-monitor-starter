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

Simple workflow that publishes the current version from `pom.xml` to Maven Central.

**Triggers:**
1. **Automatic on push to main**: Deploys whatever version is in pom.xml
2. **Manual dispatch**: Run the workflow manually from Actions tab

**How it works:**
1. Reads the current version from `pom.xml`
2. If version is `X.Y.Z-SNAPSHOT`: Deploys to OSSRH Snapshots
3. If version is `X.Y.Z`: Deploys to Maven Central and creates GitHub Release
4. No version changes are made - you control versions manually in pom.xml

**Version Management:**
You manually update the version in `pom.xml`:
```bash
# For a release
mvn versions:set -DnewVersion=1.0.0
git add pom.xml
git commit -m "Release version 1.0.0"
git push origin main

# For next development version
mvn versions:set -DnewVersion=1.0.1-SNAPSHOT
git add pom.xml
git commit -m "Prepare next development version 1.0.1-SNAPSHOT"
git push origin main
```

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

3. **Set up Sonatype Account:**
   - Create account at https://central.sonatype.com
   - Sign up with GitHub (automatically verifies io.github.nnegi88)
   - Generate User Token from Account → User Token

4. **Add GitHub Secrets:**
   - Go to Settings → Secrets and variables → Actions
   - Add the following secrets:
     - `OSSRH_USERNAME`: Your Sonatype username
     - `OSSRH_TOKEN`: Your Sonatype user token (not password)
     - `MAVEN_GPG_PRIVATE_KEY`: Content of private.key file
     - `MAVEN_GPG_PASSPHRASE`: Your GPG key passphrase

4. **Set Up Branch Protection (Optional but Recommended):**
   - Protect `main` branch
   - Require PR reviews before merging
   - Require status checks to pass

### Release Process

#### For Releases:
1. Update version in pom.xml to release version (remove -SNAPSHOT)
2. Commit and push to main
3. Workflow automatically:
   - Deploys to Maven Central
   - Creates GitHub Release with tag
4. Update version to next SNAPSHOT
5. Commit and push

#### For Snapshots:
- Simply push to `develop` or `main` with SNAPSHOT version
- Automatically deployed to OSSRH Snapshots

#### Example Release Flow:
```bash
# Currently on 1.0.0-SNAPSHOT
# Ready to release 1.0.0

# Step 1: Set release version
mvn versions:set -DnewVersion=1.0.0
git add pom.xml
git commit -m "Release version 1.0.0"
git push origin main

# Wait for workflow to complete (deploys to Maven Central)

# Step 2: Prepare next version
mvn versions:set -DnewVersion=1.0.1-SNAPSHOT
git add pom.xml
git commit -m "Prepare next development version 1.0.1-SNAPSHOT"
git push origin main
```

## Version Management

### Version Conventions
- Release versions: `1.0.0`, `1.1.0`, `2.0.0`
- Snapshot versions: `1.0.0-SNAPSHOT`, `1.1.0-SNAPSHOT`

### Version Update Commands
```bash
# Set specific version
mvn versions:set -DnewVersion=1.2.0

# Set snapshot version
mvn versions:set -DnewVersion=1.2.0-SNAPSHOT

# Revert version change (if not committed)
mvn versions:revert
```

### Version Strategy
- **Patch releases** (1.0.0 → 1.0.1): Bug fixes
- **Minor releases** (1.0.0 → 1.1.0): New features, backward compatible
- **Major releases** (1.0.0 → 2.0.0): Breaking changes

## Troubleshooting

### Release Workflow Not Triggering
- Verify you're pushing to `main` branch
- Check workflow permissions in Settings
- Ensure you have configured the required secrets

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
- Version management is fully manual - you control when and how versions change
- GitHub Releases are automatically created for non-SNAPSHOT versions