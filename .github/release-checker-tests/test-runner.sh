#!/bin/bash
set -e
echo "🧪 Running all tests..."

find release-checker-tests/tests -type f -name "*.sh" | while read -r test; do
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "▶️  Running $test"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  bash "$test"
done

echo ""
echo "✅ All tests passed"
