#!/usr/bin/env bash
# Auto-formats Java files changed relative to BASE_REF (default origin/main)
# with the repo's google-java-format jar, then runs fail-fast.sh.
set -u
cd "$(dirname "$0")/.."

BASE_REF="${BASE_REF:-origin/main}"
JAR="tools/google-java-format.jar"

if [ -f "$JAR" ]; then
  mapfile -t files < <(
    { git diff --name-only --diff-filter=d "$BASE_REF" -- '*.java' 2>/dev/null
      git diff --name-only --diff-filter=d -- '*.java'
      git ls-files --others --exclude-standard -- '*.java'; } | sort -u
  )
  if [ "${#files[@]}" -gt 0 ]; then
    echo "==> formatting ${#files[@]} changed Java file(s)"
    java -jar "$JAR" -i "${files[@]}" || echo "WARNING: formatter failed; continuing." >&2
  fi
else
  echo "WARNING: $JAR not found; skipping formatting." >&2
fi

exec "$(dirname "$0")/fail-fast.sh"
