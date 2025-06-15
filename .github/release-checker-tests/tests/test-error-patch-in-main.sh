#!/bin/bash
set -e

export GITHUB_BASE_REF="main"

echo "🔍 Running from: $(pwd)"

cp release-checker-tests/mocks/project-invalid-patch.yml project.yml
export RELEASES_JSON=$(cat release-checker-tests/data/releases.json)
export LATEST_JSON=$(cat release-checker-tests/data/releases-latest.json | jq -r .tag_name)

echo "🔍 [TEST] Invalid Patch sequence"

if RELEASES_JSON="$RELEASES_JSON" LATEST_JSON="$LATEST_JSON" bash check-release-version.sh 2>&1 | grep "Error: pre-release patch version must be 0 when starting new minor version"; then
  echo "✅ Test passed"
  exit 0
else
  echo "❌ Test failed"
  exit 1
fi
