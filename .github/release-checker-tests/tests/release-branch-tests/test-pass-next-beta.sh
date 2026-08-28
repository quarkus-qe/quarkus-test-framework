#!/bin/bash
set -e

export GITHUB_BASE_REF="1.6.z"

cp release-checker-tests/mocks/release-branch/project-release-next-beta.yml project.yml
export RELEASES_JSON=$(cat release-checker-tests/data/release-branch/releases-with-beta1.json)
export LATEST_JSON=$(cat release-checker-tests/data/release-branch/latest.json)

echo "🔍 [TEST - 1.6.z] Beta release - Beta2 accepted when Beta1 already exists"

if RELEASES_JSON="$RELEASES_JSON" LATEST_JSON="$LATEST_JSON" bash check-release-version.sh 2>&1; then
    echo "✅ Test passed"
else
    echo "❌ Test failed"
    exit 1
fi

