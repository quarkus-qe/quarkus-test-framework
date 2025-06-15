#!/bin/bash
set -e

export GITHUB_BASE_REF="1.6.z"

cp release-checker-tests/mocks/release-branch/project-release-next-version-correct.yml project.yml
export RELEASES_JSON=$(cat release-checker-tests/data/release-branch/releases.json)
export LATEST_JSON=$(cat release-checker-tests/data/release-branch/latest.json)

echo "🔍 [TEST - 1.6.z] standard sequence release - (expect success)"

if RELEASES_JSON="$RELEASES_JSON" LATEST_JSON="$LATEST_JSON" bash check-release-version.sh 2>&1 | grep -q "Error"; then
  echo "❌ Test failed"
  exit 1
else
  echo "✅ Test passed"
fi
