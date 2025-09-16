#!/bin/bash
# Universal script to bump dev version for bpm-sdk, cib-common-components, or frontend
# Usage: 
#   ./bump-npm-dev.sh bpm-sdk
#   ./bump-npm-dev.sh cib-common-components
#   ./bump-npm-dev.sh frontend

set -e

# Check if package name is provided
if [ $# -eq 0 ]; then
    echo "Please specify package: bpm-sdk, cib-common-components, or frontend"
    echo "Usage: $0 [package-name]"
    exit 1
fi

PACKAGE=$1

# Validate package name
case $PACKAGE in
    "bpm-sdk"|"cib-common-components"|"frontend")
        ;;
    *)
        echo "Invalid package: $PACKAGE"
        echo "Valid packages: bpm-sdk, cib-common-components, frontend"
        exit 1
        ;;
esac

echo "Bumping $PACKAGE dev version..."

# Navigate to project root if not already there
if [ ! -f "pom.xml" ]; then
    cd ..
fi

# Ensure we're on main and up to date
git checkout main
git fetch origin
git pull origin main

# Navigate to package directory
cd "$PACKAGE"

# Check if package.json exists
if [ ! -f "package.json" ]; then
    echo "Error: package.json not found in $PACKAGE directory"
    exit 1
fi

# Create new branch
BRANCH_NAME="bump-$PACKAGE-version-$(date +%Y%m%d-%H%M%S)"
git checkout -b "$BRANCH_NAME"

# Bump prerelease version
npm version prerelease --preid=dev --no-git-tag-version

# Get the new version
NEW_VERSION=$(node -p "require('./package.json').version")

# Commit changes
git add package.json
git commit -m "bump: increase $PACKAGE version to $NEW_VERSION"

# Push branch
git push origin "$BRANCH_NAME"

echo "Package: $PACKAGE"
echo "Created branch: $BRANCH_NAME"
echo "Bumped to version: $NEW_VERSION"
echo "Ready to create PR!"