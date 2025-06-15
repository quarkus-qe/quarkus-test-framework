#!/bin/bash
set -e

export GITHUB_BASE_REF="main"

cp release-checker-tests/mocks/project-beta21.yml project.yml
export RELEASES_JSON=$(cat release-checker-tests/data/releases.json)
export LATEST_JSON=$(cat release-checker-tests/data/releases-latest.json | jq -r .tag_name)

echo "🔍 [TEST] Invalid Beta sequence"

if RELEASES_JSON="$RELEASES_JSON" LATEST_JSON="$LATEST_JSON" bash check-release-version.sh 2>&1 | grep "Error: pre-release version should go one by one as sequence"; then
  echo "✅ Test passed"
  exit 0
else
  echo "❌ Test failed"
  exit 1
fi
