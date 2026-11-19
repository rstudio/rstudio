#!/usr/bin/env bash
#
# Claude Code PreToolUse hook (Bash matcher).
# Blocks git commit when the current branch is main or matches rel-*, and git
# push when it would update such a branch on the remote.
#
# Keep this compatible with bash 3.2 (macOS /bin/bash): e.g. an empty array
# expands as ${arr[@]+"${arr[@]}"}, since "${arr[@]}" fails under 'set -u'.
#
set -euo pipefail

# Read the tool input from stdin
input=$(cat)

# Extract the command from the JSON payload
command=$(echo "$input" | jq -r '.tool_input.command // empty') || {
  echo "WARNING: Failed to parse hook input JSON. Blocking as a precaution." >&2
  exit 2
}

# Join continuation lines: the checks below read the command a line at a time
continuation=$'\\\n'
command="${command//"$continuation"/ }"

# A git invocation may carry global options between 'git' and its subcommand
# (e.g. 'git -C <dir> -c x=y commit'), so the subcommand is not necessarily the
# first token. This fragment matches a run of such options -- '-C <dir>' (whose
# argument may be single- or double-quoted, e.g. a path with spaces), '-c
# <name>=<value>', and other single-/double-dash flags -- so we can find the
# real commit/push subcommand, and the -C target, past them.
git_opts="([[:space:]]+(-C[[:space:]]+(\"[^\"]*\"|'[^']*'|[^[:space:]]+)|-c[[:space:]]+[^[:space:]]+|--?[^[:space:]]+))*"

is_protected() {
  [ "$1" = "main" ] || [[ "$1" == rel-* ]]
}

# Prints the remote branches that a 'git push' with the arguments in $1 would
# update, one per line: '*' when it may update any branch, and '?' when the
# shell expands an argument, so what it pushes can't be read here. $2 is the
# current branch, which git pushes when no refspec is given, and which HEAD
# names. That assumes the default push configuration: push.default=upstream, or
# a remote.<name>.push refspec, can push the current branch under another name.
push_targets() {
  local token spec dst
  local skip_arg=false end_of_opts=false have_remote=false
  local -a tokens=() specs=()

  read -r -a tokens <<< "$1" || true
  for token in ${tokens[@]+"${tokens[@]}"}; do
    case "$token" in
      *[\$\`\{\\?[]*)
        echo '?'
        return ;;
    esac

    token="${token//\"/}"
    token="${token//\'/}"

    if $skip_arg; then
      skip_arg=false
      continue
    fi

    if ! $end_of_opts; then
      case "$token" in
        --)
          end_of_opts=true
          continue ;;
        --all|--mirror|--branches)
          echo '*'
          continue ;;
        -o|--push-option|--repo|--receive-pack|--exec)
          skip_arg=true
          continue ;;
        -*)
          continue ;;
      esac
    fi

    # the first operand is the remote; the rest are refspecs
    if $have_remote; then
      specs+=("$token")
    else
      have_remote=true
    fi
  done

  if [ ${#specs[@]} -eq 0 ]; then
    echo "$2"
    return
  fi

  for spec in "${specs[@]}"; do
    spec="${spec#+}"

    # ':' alone pushes every branch that exists on both sides; ':dst' deletes
    case "$spec" in
      :)
        dst='*' ;;
      *:*)
        dst="${spec#*:}" ;;
      *)
        dst="$spec" ;;
    esac

    case "$dst" in
      *'*'*)
        dst='*' ;;
      HEAD|@)
        dst="$2" ;;
      refs/heads/*)
        dst="${dst#refs/heads/}" ;;
      heads/*)
        dst="${dst#heads/}" ;;
      refs/*)
        continue ;;
    esac

    echo "$dst"
  done
}

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

  if is_protected "$branch" && echo "$command" | grep -qE "\bgit${git_opts}[[:space:]]+commit\b"; then
    echo "BLOCKED: git commit on '$branch' is not allowed. Create a feature branch first." >&2
    exit 2
  fi

  # A push is judged by the branches it updates, not the one checked out, so
  # that 'git push origin <feature-branch>' works from a checkout on main. Drop
  # redirections (e.g. '2>&1', '>log'), operator and target, then split the
  # command into simple commands, so each push's arguments end where its
  # command does. A push is found anywhere in a command, so that wrapped ones
  # (e.g. bash -c '...') are still checked; the cost is that a commit message
  # quoting a push to main is blocked too.
  newline=$'\n'
  segments=$(echo "$command" | sed -E 's/[0-9]*(&>>?|>>?&?|>\||<&|<<<?|<>|<)[[:space:]]*[^[:space:];&|()<>]*//g')
  for op in '&&' '||' ';' '|' '&' '(' ')'; do
    segments="${segments//"$op"/$newline}"
  done

  while IFS= read -r segment; do
    # the text after 'git push', or nothing when the segment has no push
    args=$(echo "$segment" | sed -nE "s/(^|.*[^[:alnum:]_])git${git_opts}[[:space:]]+push([^[:alnum:]_]|\$)/ /p")
    if [ -z "$args" ]; then
      continue
    fi

    while IFS= read -r target; do
      if [ "$target" = '*' ]; then
        echo "BLOCKED: git push that may update every branch (--all, --mirror, --branches, ':' or a glob) is not allowed. Push a feature branch by name." >&2
        exit 2
      fi
      if [ "$target" = '?' ]; then
        echo "BLOCKED: git push with an argument the shell expands (\$, \`, {, \\, ? or [) is not allowed, since the branch it updates can't be checked. Name the branch literally." >&2
        exit 2
      fi
      if is_protected "$target"; then
        echo "BLOCKED: git push to '$target' is not allowed. Push a feature branch instead." >&2
        exit 2
      fi
    done < <(push_targets "$args" "$branch")
  done <<< "$segments"
fi

# Allow: nothing matched a protected branch. Exit explicitly so the allow path
# does not inherit a non-zero status from a preceding test (e.g. the branch
# comparison above), which Claude Code would surface as a hook error.
exit 0
