#!/usr/bin/env bash
#
# Claude Code PreToolUse hook (Bash matcher).
# Blocks git push and git commit when the current branch is main or matches rel-*.
#
set -euo pipefail

# Read the tool input from stdin
input=$(cat)

# Extract the command from the JSON payload
command=$(echo "$input" | jq -r '.tool_input.command // empty') || {
  echo "WARNING: Failed to parse hook input JSON. Blocking as a precaution." >&2
  exit 2
}

# Block git push and git commit on protected branches
if echo "$command" | grep -qE '\bgit\s+(push|commit)\b'; then
  branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null) || {
    echo "WARNING: Could not determine current branch. Blocking as a precaution." >&2
    exit 2
  }
  if [ "$branch" = "main" ] || echo "$branch" | grep -qE '^rel-'; then
    if echo "$command" | grep -qE '\bgit\s+push\b'; then
      echo "BLOCKED: git push on '$branch' is not allowed. Create a feature branch first." >&2
    else
      echo "BLOCKED: git commit on '$branch' is not allowed. Create a feature branch first." >&2
    fi
    exit 2
  fi
fi
