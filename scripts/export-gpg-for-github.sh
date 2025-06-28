#!/bin/bash

# Script to export GPG key for GitHub Actions
# This will help you get the GPG private key in the format needed for GitHub Secrets

echo "🔐 GPG Key Export for GitHub Actions"
echo "===================================="
echo ""

# Check if gpg is installed
if ! command -v gpg &> /dev/null; then
    echo "❌ Error: gpg is not installed"
    exit 1
fi

# Email from settings.xml
GPG_EMAIL="nnegi88@gmail.com"

echo "📧 Looking for GPG key for: $GPG_EMAIL"
echo ""

# List keys and find the key ID
KEY_INFO=$(gpg --list-secret-keys --keyid-format=long "$GPG_EMAIL" 2>/dev/null)

if [ -z "$KEY_INFO" ]; then
    echo "❌ Error: No GPG key found for $GPG_EMAIL"
    echo ""
    echo "Available keys:"
    gpg --list-secret-keys --keyid-format=long
    exit 1
fi

# Extract key ID
KEY_ID=$(echo "$KEY_INFO" | grep -E "^sec" | awk '{print $2}' | cut -d'/' -f2 | head -1)

if [ -z "$KEY_ID" ]; then
    echo "❌ Error: Could not extract key ID"
    exit 1
fi

echo "🔑 Found GPG key: $KEY_ID"
echo ""

# Export the private key
echo "📤 Exporting GPG private key..."
GPG_PRIVATE_KEY=$(gpg --armor --export-secret-keys "$KEY_ID" 2>/dev/null)

if [ -z "$GPG_PRIVATE_KEY" ]; then
    echo "❌ Error: Failed to export GPG private key"
    exit 1
fi

# Create output directory
OUTPUT_DIR="./github-secrets"
mkdir -p "$OUTPUT_DIR"

# Save the private key to a file
echo "$GPG_PRIVATE_KEY" > "$OUTPUT_DIR/MAVEN_GPG_PRIVATE_KEY.txt"

# Create a summary file with all secrets
cat > "$OUTPUT_DIR/github-secrets-summary.txt" << EOF
GitHub Secrets Configuration for Maven Central
==============================================

Add these secrets to your GitHub repository:
https://github.com/nnegi88/spring-boot-error-monitor-starter/settings/secrets/actions

1. OSSRH_USERNAME
   Value: q7fTXcIX

2. OSSRH_TOKEN
   Value: 1pbgk7e2N/obg5quklUzG8d7LcJD/+mNtx0L50blEyAv

3. MAVEN_GPG_PASSPHRASE
   Value: (leave empty - just create the secret with no value)

4. MAVEN_GPG_PRIVATE_KEY
   Value: Copy the entire content of MAVEN_GPG_PRIVATE_KEY.txt (including BEGIN/END lines)

Steps to add secrets:
1. Go to Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Add each secret with the name and value above
4. For MAVEN_GPG_PRIVATE_KEY, paste the entire content including:
   -----BEGIN PGP PRIVATE KEY BLOCK-----
   ...
   -----END PGP PRIVATE KEY BLOCK-----

IMPORTANT: 
- The GPG private key is sensitive! Delete these files after adding to GitHub
- Make sure to paste the ENTIRE GPG key including the BEGIN/END lines
- The MAVEN_GPG_PASSPHRASE should be an empty secret (no value)
EOF

echo "✅ Success! Files created in $OUTPUT_DIR/"
echo ""
echo "📁 Files created:"
echo "   - $OUTPUT_DIR/MAVEN_GPG_PRIVATE_KEY.txt (contains your GPG private key)"
echo "   - $OUTPUT_DIR/github-secrets-summary.txt (instructions for GitHub)"
echo ""
echo "⚠️  IMPORTANT SECURITY NOTES:"
echo "   1. These files contain sensitive credentials!"
echo "   2. Add the secrets to GitHub following the instructions in github-secrets-summary.txt"
echo "   3. DELETE the $OUTPUT_DIR directory after adding secrets to GitHub"
echo ""
echo "🔗 Quick link to add secrets:"
echo "   https://github.com/nnegi88/spring-boot-error-monitor-starter/settings/secrets/actions/new"
echo ""
echo "After adding all secrets, run:"
echo "   rm -rf $OUTPUT_DIR"