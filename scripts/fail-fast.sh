#!/usr/bin/env bash
# Runs the same gates as CI, stopping at the first failure:
#   1. python syntax  2. javac lint  3. unit tests  4. functional tests
set -u
cd "$(dirname "$0")/.."

PYTHON="${PYTHON:-$(command -v python3 || command -v python)}"

step() {
  local name="$1"; shift
  echo "==> $name"
  if ! "$@"; then
    echo "FAIL-FAST: '$name' failed." >&2
    case "$name" in
      python-syntax)    echo "Hint: fix the syntax error reported above." >&2 ;;
      java-lint)        echo "Hint: fix the javac [lint] warnings above (they are errors). Report: build/reports/problems/problems-report.html" >&2 ;;
      unit-tests)       echo "Hint: open build/reports/tests/unitTest/index.html. Rerun one class: ./gradlew unitTest --tests '<fqcn>'" >&2 ;;
      functional-tests) echo "Hint: open build/reports/tests/functionalTest/index.html. Rerun one class: ./gradlew functionalTest --tests '<fqcn>'" >&2 ;;
    esac
    exit 1
  fi
}

step python-syntax "$PYTHON" -W error -m compileall -q src/main/python .github/hooks
step java-lint ./gradlew lintJava --console=plain
step unit-tests ./gradlew unitTest --console=plain
step functional-tests ./gradlew functionalTest --console=plain

echo "All checks passed."
