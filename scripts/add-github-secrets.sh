#!/bin/bash

# Script to add GitHub secrets using gh CLI

echo "🔐 Adding GitHub Secrets using gh CLI"
echo "====================================="
echo ""

# Check if gh is installed
if ! command -v gh &> /dev/null; then
    echo "❌ Error: GitHub CLI (gh) is not installed"
    echo "Install it with: brew install gh"
    exit 1
fi

# Check if authenticated
if ! gh auth status &> /dev/null; then
    echo "❌ Error: Not authenticated with GitHub CLI"
    echo "Run: gh auth login"
    exit 1
fi

# Set the repository
REPO="nnegi88/spring-boot-error-monitor-starter"
echo "📦 Repository: $REPO"
echo ""

# Add secrets
echo "1️⃣ Adding OSSRH_USERNAME..."
echo "q7fTXcIX" | gh secret set OSSRH_USERNAME --repo="$REPO"

echo "2️⃣ Adding OSSRH_TOKEN..."
echo "1pbgk7e2N/obg5quklUzG8d7LcJD/+mNtx0L50blEyAv" | gh secret set OSSRH_TOKEN --repo="$REPO"

echo "3️⃣ Adding MAVEN_GPG_PASSPHRASE (empty)..."
echo "" | gh secret set MAVEN_GPG_PASSPHRASE --repo="$REPO"

echo "4️⃣ Adding MAVEN_GPG_PRIVATE_KEY..."
if [ -f "./github-secrets/MAVEN_GPG_PRIVATE_KEY.txt" ]; then
    gh secret set MAVEN_GPG_PRIVATE_KEY --repo="$REPO" < "./github-secrets/MAVEN_GPG_PRIVATE_KEY.txt"
else
    echo "❌ Error: ./github-secrets/MAVEN_GPG_PRIVATE_KEY.txt not found"
    echo "Run ./scripts/export-gpg-for-github.sh first"
    exit 1
fi

echo ""
echo "✅ All secrets added successfully!"
echo ""

# List secrets to verify
echo "📋 Verifying secrets:"
gh secret list --repo="$REPO"

echo ""
echo "🧹 Cleaning up sensitive files..."
rm -rf ./github-secrets

echo "✅ Done! Sensitive files removed."
echo ""
echo "Next steps:"
echo "1. The secrets are now configured in GitHub"
echo "2. You can re-enable Maven Central deployment in pom.xml and release.yml"