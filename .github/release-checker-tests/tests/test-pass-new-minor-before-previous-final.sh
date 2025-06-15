#!/bin/bash
set -e

export GITHUB_BASE_REF="main"

cp release-checker-tests/mocks/project-early-minor-bump.yml project.yml
export RELEASES_JSON=$(cat release-checker-tests/data/releases.json)
export LATEST_JSON=$(cat release-checker-tests/data/releases-latest.json | jq -r .tag_name)

echo "🔍 Running test: early minor version bump before new release (expect success)"

if RELEASES_JSON="$RELEASES_JSON" LATEST_JSON="$LATEST_JSON" bash check-release-version.sh 2>&1 | grep "Error"; then
  echo "❌ Test failed"
  exit 1
else
  echo "✅ Test passed"
  exit 0
fi
