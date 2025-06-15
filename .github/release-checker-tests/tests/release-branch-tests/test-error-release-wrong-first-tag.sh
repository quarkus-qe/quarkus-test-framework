#!/bin/bash
set -e

export GITHUB_BASE_REF="1.6.z"

cp release-checker-tests/mocks/release-branch/project-release-first-tag-wrong.yml project.yml
export RELEASES_JSON=$(cat release-checker-tests/data/release-branch/first-release-missing.json)
export LATEST_JSON=$(cat release-checker-tests/data/release-branch/first-release-missing.json)

echo "🔍 [TEST - 1.6.z] Release - Invalid first release tag version"

if RELEASES_JSON="$RELEASES_JSON" LATEST_JSON="$LATEST_JSON" bash check-release-version.sh 2>&1 | grep "Error: wrong tag name for the first release in new branch"; then
  echo "✅ Test passed"
else
  echo "❌ Test failed"
  exit 1
fi
