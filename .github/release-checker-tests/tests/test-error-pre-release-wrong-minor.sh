#!/bin/bash
set -e

export GITHUB_BASE_REF="main"
cp release-checker-tests/mocks/project-wrong-minor.yml project.yml
export RELEASES_JSON=$(cat release-checker-tests/data/releases.json)
export LATEST_JSON=$(cat release-checker-tests/data/releases-latest.json | jq -r .tag_name)

echo "🔍 [TEST] Invalid pre-release minor version"

if RELEASES_JSON="$RELEASES_JSON" LATEST_JSON="$LATEST_JSON" bash check-release-version.sh 2>&1 | grep "Error: cannot skip minor versions. Must be based on latest prerelease or release"; then
  echo "✅ Test passed"
  exit 0
else
  echo "❌ Test failed"
  exit 1
fi
