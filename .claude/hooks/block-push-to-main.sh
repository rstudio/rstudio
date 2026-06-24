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

# A git invocation may carry global options between 'git' and its subcommand
# (e.g. 'git -C <dir> -c x=y commit'), so the subcommand is not necessarily the
# first token. This fragment matches a run of such options -- '-C <dir>' (whose
# argument may be single- or double-quoted, e.g. a path with spaces), '-c
# <name>=<value>', and other single-/double-dash flags -- so we can find the
# real commit/push subcommand, and the -C target, past them.
git_opts="([[:space:]]+(-C[[:space:]]+(\"[^\"]*\"|'[^']*'|[^[:space:]]+)|-c[[:space:]]+[^[:space:]]+|--?[^[:space:]]+))*"

# Block git push and git commit on protected branches
if echo "$command" | grep -qE "\bgit${git_opts}[[:space:]]+(push|commit)\b"; then

  # Resolve the directory the git command actually targets, so that commits
  # and pushes from a worktree are evaluated against the worktree's branch --
  # not whatever branch happens to be checked out in this hook's working
  # directory (typically the primary checkout). Prefer the argument to an
  # explicit 'git -C <dir>', then a leading 'cd <dir>', else the cwd. The -C
  # match is anchored to the leading option region (via "$git_opts") so a -C
  # appearing only inside a commit message (e.g. -m "see -C") is not mistaken
  # for a target directory.
  #
  # The '|| true' is required: under 'set -e'/'pipefail' a non-matching grep (no
  # -C / no cd, the common bare 'git commit' case) would otherwise fail the
  # assignment and abort the hook -- letting the command through unchecked.
  target_dir=$(echo "$command" | grep -oE "\bgit${git_opts}[[:space:]]+-C[[:space:]]+(\"[^\"]*\"|'[^']*'|[^[:space:]]+)" | head -1 | sed -E "s/.*-C[[:space:]]+//" || true)
  if [ -z "$target_dir" ]; then
    target_dir=$(echo "$command" | grep -oE '\bcd[[:space:]]+[^ ;&|]+' | head -1 | sed -E 's/^cd[[:space:]]+//' || true)
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
    if echo "$command" | grep -qE "\bgit${git_opts}[[:space:]]+push\b"; then
      echo "BLOCKED: git push on '$branch' is not allowed. Create a feature branch first." >&2
    else
      echo "BLOCKED: git commit on '$branch' is not allowed. Create a feature branch first." >&2
    fi
    exit 2
  fi
fi

# Allow: nothing matched a protected branch. Exit explicitly so the allow path
# does not inherit a non-zero status from a preceding test (e.g. the branch
# comparison above), which Claude Code would surface as a hook error.
exit 0
