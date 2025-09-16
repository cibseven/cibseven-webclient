#!/bin/bash
# Universal script to create production release for bpm-sdk, cib-common-components, or frontend
# Usage: 
#   ./bump-npm-release.sh bpm-sdk
#   ./bump-npm-release.sh cib-common-components
#   ./bump-npm-release.sh frontend

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

echo "Creating production release for $PACKAGE..."

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

# Get current version and extract production version (remove -dev.x)
CURRENT_VERSION=$(node -p "require('./package.json').version")
PROD_VERSION=$(echo $CURRENT_VERSION | sed 's/-dev\.[0-9]*$//')

echo "Current version: $CURRENT_VERSION"
echo "Production version: $PROD_VERSION"

# Create release branch
BRANCH_NAME="release-$PACKAGE-$PROD_VERSION"
git checkout -b "$BRANCH_NAME"

# 1. Bump to production version (removes -dev.x)
npm version --no-git-tag-version $PROD_VERSION
git add package.json
git commit -m "release: $PACKAGE $PROD_VERSION"

# 3. Push commits and tags
git push origin "$BRANCH_NAME"

echo "Package: $PACKAGE"
echo "Released version: $PROD_VERSION"
echo "Created branch: $BRANCH_NAME"
echo "Ready to create PR for release!"