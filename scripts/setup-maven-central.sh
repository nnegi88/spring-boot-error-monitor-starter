#!/bin/bash

# Script to set up Maven Central deployment
# This script helps generate GPG keys and configure GitHub secrets

echo "============================================"
echo "Maven Central Deployment Setup"
echo "============================================"
echo ""

# Check if GPG is installed
if ! command -v gpg &> /dev/null; then
    echo "❌ GPG is not installed. Please install GPG first:"
    echo "   - macOS: brew install gnupg"
    echo "   - Ubuntu/Debian: sudo apt-get install gnupg"
    echo "   - RHEL/CentOS: sudo yum install gnupg"
    exit 1
fi

echo "✅ GPG is installed"
echo ""

# Function to generate GPG key
generate_gpg_key() {
    echo "📝 Generating GPG key for Maven Central signing..."
    echo ""
    
    # Create GPG batch file for automated key generation
    cat > gpg-batch.txt << EOF
%echo Generating GPG key for Maven Central
Key-Type: RSA
Key-Length: 4096
Subkey-Type: RSA
Subkey-Length: 4096
Name-Real: Naveen Negi
Name-Email: nnegi88@gmail.com
Expire-Date: 2y
%commit
%echo done
EOF
    
    # Generate the key
    gpg --batch --generate-key gpg-batch.txt
    
    # Clean up
    rm gpg-batch.txt
    
    echo "✅ GPG key generated successfully"
}

# Function to export GPG key
export_gpg_key() {
    echo ""
    echo "📤 Exporting GPG keys..."
    
    # Get the key ID
    KEY_ID=$(gpg --list-secret-keys --keyid-format=long nnegi88@gmail.com | grep sec | awk '{print $2}' | cut -d'/' -f2)
    
    if [ -z "$KEY_ID" ]; then
        echo "❌ No GPG key found for nnegi88@gmail.com"
        return 1
    fi
    
    echo "Found GPG key: $KEY_ID"
    
    # Export private key
    gpg --armor --export-secret-keys nnegi88@gmail.com > private-key.asc
    
    # Upload to key servers
    echo ""
    echo "📤 Uploading public key to key servers..."
    gpg --keyserver keyserver.ubuntu.com --send-keys $KEY_ID
    gpg --keyserver keys.openpgp.org --send-keys $KEY_ID
    gpg --keyserver pgp.mit.edu --send-keys $KEY_ID
    
    echo "✅ GPG key exported and uploaded"
    echo ""
    echo "🔑 Your GPG Key ID: $KEY_ID"
}

# Function to set up GitHub secrets
setup_github_secrets() {
    echo ""
    echo "🔐 Setting up GitHub Secrets..."
    echo ""
    
    # Check if gh CLI is installed
    if ! command -v gh &> /dev/null; then
        echo "❌ GitHub CLI (gh) is not installed. Please install it first:"
        echo "   - macOS: brew install gh"
        echo "   - Ubuntu/Debian: See https://github.com/cli/cli/blob/trunk/docs/install_linux.md"
        echo ""
        echo "Alternatively, you can manually add secrets at:"
        echo "https://github.com/nnegi88/spring-boot-error-monitor-starter/settings/secrets/actions"
        return 1
    fi
    
    # Check if authenticated
    if ! gh auth status &> /dev/null; then
        echo "❌ Not authenticated with GitHub CLI. Please run: gh auth login"
        return 1
    fi
    
    echo "Adding secrets to GitHub repository..."
    
    # Add GPG private key
    if [ -f "private-key.asc" ]; then
        gh secret set MAVEN_GPG_PRIVATE_KEY < private-key.asc
        echo "✅ Added MAVEN_GPG_PRIVATE_KEY"
    else
        echo "❌ private-key.asc not found"
        return 1
    fi
    
    # Prompt for GPG passphrase
    echo ""
    read -sp "Enter your GPG passphrase: " GPG_PASSPHRASE
    echo ""
    echo "$GPG_PASSPHRASE" | gh secret set MAVEN_GPG_PASSPHRASE
    echo "✅ Added MAVEN_GPG_PASSPHRASE"
    
    # OSSRH credentials
    echo ""
    echo "📝 Sonatype OSSRH Credentials"
    echo "If you don't have an account, create one at: https://issues.sonatype.org"
    echo ""
    read -p "Enter your OSSRH username: " OSSRH_USERNAME
    read -sp "Enter your OSSRH token (not password): " OSSRH_TOKEN
    echo ""
    
    echo "$OSSRH_USERNAME" | gh secret set OSSRH_USERNAME
    echo "✅ Added OSSRH_USERNAME"
    
    echo "$OSSRH_TOKEN" | gh secret set OSSRH_TOKEN
    echo "✅ Added OSSRH_TOKEN"
    
    echo ""
    echo "✅ All GitHub secrets configured successfully!"
}

# Main menu
echo "Choose an option:"
echo "1. Generate new GPG key (if you don't have one)"
echo "2. Use existing GPG key"
echo "3. Configure GitHub secrets only"
echo "4. Complete setup (generate key + configure secrets)"
echo ""
read -p "Enter your choice (1-4): " choice

case $choice in
    1)
        generate_gpg_key
        export_gpg_key
        ;;
    2)
        export_gpg_key
        ;;
    3)
        setup_github_secrets
        ;;
    4)
        # Check if key already exists
        if gpg --list-secret-keys nnegi88@gmail.com &> /dev/null; then
            echo "⚠️  GPG key already exists for nnegi88@gmail.com"
            read -p "Do you want to use the existing key? (y/n): " use_existing
            if [ "$use_existing" != "y" ]; then
                echo "❌ Setup cancelled"
                exit 1
            fi
        else
            generate_gpg_key
        fi
        export_gpg_key
        setup_github_secrets
        ;;
    *)
        echo "❌ Invalid choice"
        exit 1
        ;;
esac

# Cleanup and final instructions
if [ -f "private-key.asc" ]; then
    echo ""
    echo "⚠️  IMPORTANT: Your private key has been exported to 'private-key.asc'"
    echo "   - Keep this file secure!"
    echo "   - Back it up in a safe location"
    echo "   - Delete it after setup is complete"
    echo ""
    read -p "Delete private-key.asc now? (y/n): " delete_key
    if [ "$delete_key" = "y" ]; then
        rm private-key.asc
        echo "✅ Private key file deleted"
    fi
fi

echo ""
echo "📋 Next Steps:"
echo "1. If you haven't already, create a JIRA ticket at https://issues.sonatype.org"
echo "   - Project: Community Support - Open Source Project Repository Hosting"
echo "   - Issue Type: New Project"
echo "   - Group Id: io.github.nnegi88"
echo "   - Project URL: https://github.com/nnegi88/spring-boot-error-monitor-starter"
echo "   - SCM URL: https://github.com/nnegi88/spring-boot-error-monitor-starter.git"
echo ""
echo "2. Wait for approval (usually 1-2 business days)"
echo ""
echo "3. Once approved, you can trigger the release workflow:"
echo "   gh workflow run 'Release to Maven Central'"
echo ""
echo "✅ Setup complete!"