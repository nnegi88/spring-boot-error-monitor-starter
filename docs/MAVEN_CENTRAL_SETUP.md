# Maven Central Setup Guide (New Process)

This guide covers the new streamlined process for publishing to Maven Central via https://central.sonatype.com

## Overview

The old JIRA-based system at issues.sonatype.org has been replaced with a modern, automated system that's much faster and easier to use.

## Prerequisites

- [x] GPG key for signing (already configured)
- [ ] Sonatype Central account
- [ ] User token for authentication

## Step-by-Step Setup

### 1. Create Sonatype Central Account

1. Go to https://central.sonatype.com
2. Click "Sign Up"
3. **Important**: Choose "Sign up with GitHub" 
   - This automatically verifies your ownership of `io.github.nnegi88`
   - No manual verification needed!

### 2. Verify Namespace

Once logged in:
1. Go to your dashboard
2. Click "Add Namespace"
3. Enter: `io.github.nnegi88`
4. It should be automatically approved since you signed in with GitHub

### 3. Generate User Token

1. In your account, go to "Account" → "User Token"
2. Click "Generate User Token"
3. Copy the generated token (you won't see it again!)
4. This token is used instead of your password for deployments

### 4. Configure GitHub Secrets

Add these secrets to your repository:

```bash
# Your Sonatype username (usually your email)
gh secret set OSSRH_USERNAME

# The user token you just generated (NOT your password)
gh secret set OSSRH_TOKEN
```

### 5. Enable Deployment in Workflows

Once you have your credentials, uncomment the deployment sections in:
- `.github/workflows/release.yml` (lines 69-75)
- `.github/workflows/snapshot.yml` (lines 65-72)

### 6. Test Deployment

```bash
# Test with a snapshot first
gh workflow run "Deploy Snapshot"

# Then do a release
gh workflow run "Release to Maven Central"
```

## Benefits of the New System

- **Instant namespace verification** when using GitHub login
- **No waiting** for JIRA tickets
- **Modern UI** with better documentation
- **Faster sync** to Maven Central (usually < 30 minutes)
- **Better security** with user tokens instead of passwords

## Troubleshooting

### "401 Unauthorized" Error
- Make sure you're using the user token, not your password
- Regenerate the token if needed

### "Namespace not verified" Error
- Ensure you signed up with GitHub
- The namespace must match your GitHub username (io.github.nnegi88)

### GPG Signing Issues
- Your GPG key is already configured
- Public key uploaded to keyservers
- Using empty passphrase as configured

## Next Steps

After setup:
1. Your artifacts will be available at: https://central.sonatype.com/artifact/io.github.nnegi88/spring-boot-error-monitor-starter
2. They'll sync to Maven Central within 30 minutes
3. Maven Central URL: https://repo1.maven.org/maven2/io/github/nnegi88/spring-boot-error-monitor-starter/

## References

- [What happened to issues.sonatype.org?](https://central.sonatype.org/faq/what-happened-to-issues-sonatype-org/)
- [Central Portal Documentation](https://central.sonatype.org/publish/publish-portal-upload/)
- [Publishing via GitHub Actions](https://central.sonatype.org/publish/publish-github-actions/)