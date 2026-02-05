#!/bin/bash
#
# Bump npm dev prerelease version (e.g. 1.0.3-dev.7 → 1.0.3-dev.8)
#
# Usage:
#   ./npm-bump-dev.sh             # bumps frontend (default)
#   ./npm-bump-dev.sh bpm-sdk     # bumps bpm-sdk
#
set -e

package="${1:-frontend}"
root="$(cd "$(dirname "$0")" && pwd)"
pkg_json="$root/$package/package.json"

if [ ! -f "$pkg_json" ]; then
  echo "Error: $pkg_json not found"
  exit 1
fi

cd "$root/$package"

npm version prerelease --preid=dev --no-git-tag-version
new_version=$(node -p "require('./package.json').version")

git add package.json
git commit -m "bump: increase $package version to $new_version"

echo "Done: $package $new_version"
