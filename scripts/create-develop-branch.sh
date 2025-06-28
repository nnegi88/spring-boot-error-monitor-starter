#!/bin/bash

# Script to create and set up develop branch for snapshot deployments

echo "Setting up develop branch for snapshot deployments..."

# Check if develop branch exists locally
if git show-ref --verify --quiet refs/heads/develop; then
    echo "Develop branch already exists locally"
    git checkout develop
else
    # Check if develop exists on remote
    if git ls-remote --heads origin develop | grep develop > /dev/null; then
        echo "Develop branch exists on remote, checking out..."
        git checkout -b develop origin/develop
    else
        echo "Creating new develop branch..."
        git checkout -b develop
    fi
fi

# Ensure we're on develop
current_branch=$(git branch --show-current)
if [ "$current_branch" != "develop" ]; then
    echo "Failed to switch to develop branch"
    exit 1
fi

echo "Successfully on develop branch"

# Push develop branch to remote if it doesn't exist
if ! git ls-remote --heads origin develop | grep develop > /dev/null; then
    echo "Pushing develop branch to remote..."
    git push -u origin develop
fi

echo "Develop branch is ready for snapshot deployments!"
echo ""
echo "Next steps:"
echo "1. Develop features on feature branches"
echo "2. Merge feature branches to develop"
echo "3. Snapshots will automatically deploy to OSSRH"
echo "4. When ready for release, merge develop to main"