#!/bin/bash
set -e
export GITHUB_BASE_REF="main"

cp release-checker-tests/mocks/project-next-version-1.7.0-Beta1.yml project.yml
export RELEASES_JSON=$(cat release-checker-tests/data/releases-after-1.6.json)
export LATEST_JSON=$(cat release-checker-tests/data/releases-after-1.6-latest.json | jq -r .tag_name)

echo "🔍 [TEST] Starting new minor cycle after released 1.6.0 (expect success)"

if RELEASES_JSON="$RELEASES_JSON" LATEST_JSON="$LATEST_JSON" bash check-release-version.sh 2>&1 | grep "Starting new minor development cycle after released version"; then
  echo "✅ Test passed"
else
  echo "❌ Test failed"
  exit 1
fi
