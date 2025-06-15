#!/bin/bash
set -e

export GITHUB_BASE_REF="1.6.z"

cp release-checker-tests/mocks/release-branch/project-release-current-version-wrong-patch.yml project.yml
export RELEASES_JSON=$(cat release-checker-tests/data/release-branch/releases.json)
export LATEST_JSON=$(cat release-checker-tests/data/release-branch/latest.json)

echo "🔍 [TEST - 1.6.z] Release branch - invalid patch bump"

if RELEASES_JSON="$RELEASES_JSON" LATEST_JSON="$LATEST_JSON" bash check-release-version.sh 2>&1 | grep "Error: release patch versions should be bumped one by one as sequence"; then
  echo "✅ Test passed"
else
  echo "❌ Test failed"
  exit 1
fi
