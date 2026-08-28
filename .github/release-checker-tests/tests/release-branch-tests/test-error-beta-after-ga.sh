#!/bin/bash
set -e

export GITHUB_BASE_REF="1.6.z"

cp release-checker-tests/mocks/release-branch/project-release-first-beta.yml project.yml
export RELEASES_JSON=$(cat release-checker-tests/data/release-branch/releases.json)
export LATEST_JSON=$(cat release-checker-tests/data/release-branch/latest.json)

echo "🔍 [TEST - 1.6.z] Beta release - Beta rejected when GA already exists on branch"

if RELEASES_JSON="$RELEASES_JSON" LATEST_JSON="$LATEST_JSON" bash check-release-version.sh 2>&1 | grep -q "Error: Beta releases are not allowed after the GA release exists"; then
    echo "✅ Test passed"
else
    echo "❌ Test failed"
    exit 1
fi