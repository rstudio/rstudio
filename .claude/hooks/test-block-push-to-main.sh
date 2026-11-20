#!/usr/bin/env bash
#
# Table-driven test of block-push-to-main.sh. It isn't run in CI; run it after
# changing the hook, under bash 3.2 (macOS /bin/bash) and a current bash:
#
#   .claude/hooks/test-block-push-to-main.sh [<path-to-bash>]
#
# Each case runs the hook from a scratch checkout on main ($M) or on a feature
# branch ($F) and checks whether the command is allowed or blocked.
#
# The cases are command text for the hook to read, so their '$' must not expand:
# shellcheck disable=SC2016
set -u

hook="$(cd "$(dirname "$0")" && pwd)/block-push-to-main.sh"
bash_bin="${1:-bash}"

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

M="$tmp/on-main"
F="$tmp/on-feature"
export GIT_AUTHOR_NAME=test GIT_AUTHOR_EMAIL=test@example.com
export GIT_COMMITTER_NAME=test GIT_COMMITTER_EMAIL=test@example.com
git init -q -b main "$M"
git -C "$M" -c commit.gpgsign=false commit -q --no-verify --allow-empty -m init
git init -q -b feature/x "$F"
git -C "$F" -c commit.gpgsign=false commit -q --no-verify --allow-empty -m init

count=0
failures=0

check() {
  local expected="$1" dir="$2" cmd="$3"
  local payload status actual

  payload=$(jq -n --arg c "$cmd" '{tool_input: {command: $c}}')
  (cd "$dir" && printf '%s' "$payload" | "$bash_bin" "$hook" > /dev/null 2> "$tmp/stderr")
  status=$?

  case $status in
    0) actual=allow ;;
    2) actual=block ;;
    *) actual="error($status)" ;;
  esac

  count=$((count + 1))
  if [ "$actual" != "$expected" ]; then
    failures=$((failures + 1))
    echo "FAIL [$(basename "$dir")] expected $expected, got $actual: $cmd"
    sed 's/^/    /' "$tmp/stderr"
  fi
}

# from a checkout on main: pushes that leave main alone
check allow "$M" 'git push origin bugfix/foo'
check allow "$M" 'git push origin bugfix/foo 2>&1'
check allow "$M" 'git push origin feature/x >log 2>&1'
check allow "$M" 'git push -u origin feature/x'
check allow "$M" 'git push -o ci.skip origin feature/x'
check allow "$M" 'git push --push-option ci.skip origin feature/x'
check allow "$M" 'git push --force-with-lease=feature/x:abc origin feature/x'
check allow "$M" 'git push origin HEAD:refs/heads/feature/x'
check allow "$M" 'git push origin heads/feature/x'
check allow "$M" 'git push origin main:feature/x'
check allow "$M" 'git push origin --delete feature/x'
check allow "$M" 'git push origin refs/tags/v1.0'
check allow "$M" 'git push origin feature/x; echo done'
check allow "$M" 'git status && git push origin feature/x 2>&1 | tail -3'
check allow "$M" 'ls -la'
check allow "$M" 'git log --oneline -3'

# from a checkout on main: pushes that update main
check block "$M" 'git push'
check block "$M" 'git push 2>&1'
check block "$M" 'git push origin'
check block "$M" 'git push -u origin'
check block "$M" 'bash -c "git push"'
check block "$M" 'git push origin HEAD'
check block "$M" 'git push origin @'
check block "$M" 'git push origin main'
check block "$M" 'git push origin +main'
check block "$M" 'git push origin feature/x:main'
check block "$M" 'git push origin feature/x:refs/heads/main'
check block "$M" 'git push origin refs/heads/main'
check block "$M" 'git push origin heads/main'
check block "$M" 'git push origin HEAD:heads/main'
check block "$M" 'git push origin :main'
check block "$M" 'git push origin --delete main'
check block "$M" 'git push origin -- main'
check block "$M" 'git push origin rel-2026.12'
check block "$M" 'git push origin feature/x && git push origin main'
check block "$M" 'git push origin feature/x main'
check block "$M" 'git -c push.default=current push'
check block "$M" $'git push origin \\\n  main'
check block "$M" $'git \\\n  push origin main'

# a redirection ends only its own target, not the arguments after it
check block "$M" 'git push origin feature/x>log main'
check block "$M" 'git push origin feature/x 2>&1 main'
check block "$M" 'git push origin feature/x &>/dev/null main'

# pushes that may update any branch
check block "$M" 'git push --all origin'
check block "$M" 'git push --mirror origin'
check block "$M" 'git push origin :'
check block "$M" "git push origin 'refs/heads/*:refs/heads/*'"

# arguments the shell expands can't be checked, so they are blocked
check block "$M" 'git push origin $(git branch --show-current)'
check block "$M" 'git push origin `git branch --show-current`'
check block "$M" 'git push origin "$BR"'
check block "$M" 'git push origin {main,feature/x}'
check block "$M" 'git push origin ma\in'
check block "$M" 'git push origin ma?n'
check block "$F" 'git push -u origin "$(git branch --show-current)"'

# commits, and pushes from another checkout
check block "$M" 'git commit -m "x"'
check block "$M" 'git -C . commit -m x'
check allow "$M" "git -C $F commit -m x"
check allow "$M" "git -C $F push"
check allow "$M" "git -C \"$F\" push origin HEAD"
check block "$M" "git -C $F push origin main"
check allow "$M" "cd $F && git push"

# from a checkout on a feature branch
check allow "$F" 'git push'
check allow "$F" 'git push -u origin HEAD'
check allow "$F" 'git push origin HEAD 2>&1'
check allow "$F" 'git commit -m "x"'
check allow "$F" 'git commit -m "push from a worktree"'
check block "$F" 'git push origin main'
check block "$F" 'git push origin HEAD:main'
check block "$F" 'git push origin heads/main'
check block "$F" 'git push --all'
check block "$F" "git -C $M commit -m x"
check block "$F" "git -C $M push"
check block "$F" "cd $M && git push origin HEAD"

# the documented cost of finding a push anywhere in a command
check block "$F" 'git commit -m "why git push origin main is blocked"'

echo "$count cases, $failures failures (bash $("$bash_bin" -c 'echo $BASH_VERSION'))"
[ $failures -eq 0 ]
