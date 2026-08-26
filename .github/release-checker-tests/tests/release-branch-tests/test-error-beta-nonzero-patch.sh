#!/bin/bash
set -e

export GITHUB_BASE_REF="1.6.z"

cp release-checker-tests/mocks/release-branch/project-release-next-version-beta-nonzero-patch.yml project.yml
export RELEASES_JSON=$(cat release-checker-tests/data/release-branch/first-release-missing.json)
export LATEST_JSON=$(cat release-checker-tests/data/release-branch/latest.json)

echo "🔍 [TEST - 1.6.z] Beta release - Patch version must be 0"

if RELEASES_JSON="$RELEASES_JSON" LATEST_JSON="$LATEST_JSON" bash check-release-version.sh 2>&1 | grep -q "Error: pre-release patch version must be 0"; then
  echo "✅ Test passed"
else
  echo "❌ Test failed"
  exit 1
fi

