#!/bin/bash

# Script to check if version is appropriate for current branch

set -e

# Get current branch
BRANCH=$(git rev-parse --abbrev-ref HEAD)

# Get current version from pom.xml
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

echo "Current branch: $BRANCH"
echo "Current version: $VERSION"
echo ""

# Check version format based on branch
if [[ "$BRANCH" == "main" ]]; then
    if [[ $VERSION == *"-SNAPSHOT" ]]; then
        echo "❌ ERROR: Main branch should only contain release versions!"
        echo "Current version $VERSION contains -SNAPSHOT"
        echo ""
        echo "To fix: Remove -SNAPSHOT from version in pom.xml"
        exit 1
    else
        echo "✅ Version $VERSION is valid for main branch"
    fi
elif [[ "$BRANCH" == "develop" ]]; then
    if [[ $VERSION != *"-SNAPSHOT" ]]; then
        echo "❌ ERROR: Develop branch should only contain SNAPSHOT versions!"
        echo "Current version $VERSION is missing -SNAPSHOT"
        echo ""
        echo "To fix: Add -SNAPSHOT to version in pom.xml"
        echo "Example: $VERSION-SNAPSHOT"
        exit 1
    else
        echo "✅ Version $VERSION is valid for develop branch"
    fi
else
    echo "ℹ️  Branch $BRANCH has no version requirements"
fi