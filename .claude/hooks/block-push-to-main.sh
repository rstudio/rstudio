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

  # Resolve the directory the git command actually targets, so that commits
  # and pushes from a worktree are evaluated against the worktree's branch --
  # not whatever branch happens to be checked out in this hook's working
  # directory (typically the primary checkout). Prefer an explicit
  # 'git -C <dir> commit|push', then a leading 'cd <dir>', else the cwd.
  target_dir=$(echo "$command" | grep -oE '\bgit[[:space:]]+-C[[:space:]]+[^ ]+[[:space:]]+(commit|push)\b' | head -1 | sed -E 's/^git[[:space:]]+-C[[:space:]]+//; s/[[:space:]]+(commit|push)$//')
  if [ -z "$target_dir" ]; then
    target_dir=$(echo "$command" | grep -oE '\bcd[[:space:]]+[^ ;&|]+' | head -1 | sed -E 's/^cd[[:space:]]+//')
  fi
  target_dir="${target_dir:-.}"

  # strip surrounding quotes, if any
  target_dir="${target_dir%\"}"; target_dir="${target_dir#\"}"
  target_dir="${target_dir%\'}"; target_dir="${target_dir#\'}"

  branch=$(git -C "$target_dir" rev-parse --abbrev-ref HEAD 2>/dev/null) || {
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
