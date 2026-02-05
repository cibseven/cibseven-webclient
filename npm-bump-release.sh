#!/bin/bash
#
# Strip -dev.x suffix to create a production version (e.g. 1.0.3-dev.8 → 1.0.3)
#
# Usage:
#   ./npm-bump-release.sh             # releases frontend (default)
#   ./npm-bump-release.sh bpm-sdk     # releases bpm-sdk
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

current_version=$(node -p "require('./package.json').version")
release_version=$(echo "$current_version" | sed 's/-dev\.[0-9]*$//')

npm version --no-git-tag-version "$release_version"

git add package.json
git commit -m "release: $package $release_version"

echo "Done: $package $current_version → $release_version"
