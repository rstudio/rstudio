#!/usr/bin/env bash

# Merge a rel-* branch back into the current branch with the conventions
# this repo relies on. Handles a few release-presentation differences that
# would otherwise either silently corrupt the working tree or require
# manual conflict resolution after every release-branch merge.
#
# Specifically:
#
# 1. Pass `-X no-renames` so git's rename detection does not conflate the
#    rel branch's NEWS.md rename (NEWS.md -> version/news/os/NEWS-<ver>.md)
#    with main's own rotation of NEWS.md. Without this, `--theirs` on the
#    archived NEWS path returns git's three-way merged result rather than
#    the rel branch's actual content, silently corrupting the release notes.
#
# 2. Auto-resolve the two conflicts that result:
#    - NEWS.md (deleted-by-them): keep main's rotated copy.
#    - version/news/os/NEWS-<ver>-<codename>.md (both-added): take the rel
#      branch's content, which has any bug-fix entries that landed after
#      the rotation.
#
# 3. Reset release-only paths (version/BUILDTYPE, the whats-new assets
#    directory, etc.) to HEAD's value. Git's trivial-merge fast path
#    would otherwise silently take the rel branch's content (e.g.
#    flipping BUILDTYPE from "Daily" to "Release", or pulling in
#    release-finalized whats-new updates) because main has not modified
#    the file since the merge base. The `merge=ours` driver attribute
#    does not help here: it is only consulted when both sides have
#    changed the file, not in the one-sided trivial-merge case.
#
# The branch argument is resolved to a remote-tracking branch (e.g.
# "rel-golden-wattle" becomes "<remote>/rel-golden-wattle") so the merge
# always reflects what is published rather than a possibly-stale local
# copy. Pass a qualified ref like "origin/rel-golden-wattle" to skip the
# inference. The script also fetches the resolved branch before merging.
#
# The script stages a merge commit but does not commit it. Review with
# `git diff --staged` and then `git commit` once satisfied.

set -euo pipefail

if [ $# -lt 1 ]; then
    echo "usage: $0 <branch>" >&2
    echo "" >&2
    echo "Examples:" >&2
    echo "  $0 rel-golden-wattle           # resolves to <remote>/rel-golden-wattle" >&2
    echo "  $0 origin/rel-golden-wattle    # use an already-qualified ref" >&2
    exit 64
fi

BRANCH="$1"

# Move to the repo root.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

# Working tree must be clean so the merge state is unambiguous.
if ! git diff-index --quiet HEAD --; then
    echo "error: working tree has uncommitted changes; commit or stash first." >&2
    exit 1
fi

# Files that should always retain main's value after a rel-branch merge,
# regardless of what the rel branch did to them.
RELEASE_ONLY_FILES=(
    version/BUILDTYPE
    version/RELEASE
    version/CALENDAR_VERSION
    version/PATCH
)

# Directories whose contents should always retain main's value after a
# rel-branch merge. Any file under these paths that the merge modified
# (or added on the rel branch side) is reverted to HEAD.
RELEASE_ONLY_DIRS=(
    src/node/desktop/src/assets/whats-new
)

# Always resolve to a remote-tracking branch so the merge picks up what's
# actually published, not a possibly-stale local copy. If the user passes
# a qualified ref (e.g. "origin/rel-golden-wattle"), use it as-is. If they
# pass a bare branch name, prefix it with the remote tracked by the
# current branch (falling back to "origin", then to the first configured
# remote).
ORIGINAL_INPUT="$BRANCH"
REMOTE=""

if [[ "$BRANCH" != refs/* ]]; then
    while IFS= read -r r; do
        [ -n "$r" ] || continue
        if [[ "$BRANCH" == "$r/"* ]]; then
            REMOTE="$r"
            break
        fi
    done < <(git remote)

    if [ -z "$REMOTE" ]; then
        if upstream="$(git rev-parse --abbrev-ref '@{upstream}' 2>/dev/null)" \
            && [[ "$upstream" == */* ]] \
            && git remote get-url "${upstream%%/*}" >/dev/null 2>&1; then
            REMOTE="${upstream%%/*}"
        elif git remote get-url origin >/dev/null 2>&1; then
            REMOTE="origin"
        else
            REMOTE="$(git remote | head -n 1)"
        fi

        if [ -z "$REMOTE" ]; then
            echo "error: no remotes configured; cannot resolve '$BRANCH'." >&2
            exit 1
        fi

        BRANCH="$REMOTE/$BRANCH"
        echo "Resolved '$ORIGINAL_INPUT' -> '$BRANCH'"
    fi
fi

# Refresh the remote-tracking branch so the merge reflects the latest
# published state of the rel branch.
if [ -n "$REMOTE" ]; then
    REMOTE_BRANCH="${BRANCH#$REMOTE/}"
    echo "Fetching $REMOTE $REMOTE_BRANCH..."
    git fetch "$REMOTE" "$REMOTE_BRANCH"
fi

# Do the merge.
echo "Merging $BRANCH (-X no-renames, --no-ff, --no-commit)..."
git merge --no-commit --no-ff -X no-renames "$BRANCH" || true

# Auto-resolve the two expected conflicts.

# (1) NEWS.md "deleted by them": keep main's version. `git add` on a file
# with a UD stage tells git we want our version (the working copy).
if git status --porcelain | grep -Eq '^UD NEWS\.md$'; then
    git add NEWS.md
    echo "  resolved NEWS.md: kept main's version"
fi

# (2) version/news/os/NEWS-<ver>-<codename>.md "both added": take theirs.
while IFS= read -r path; do
    case "$path" in
        version/news/os/NEWS-*.md)
            git checkout --theirs -- "$path"
            git add -- "$path"
            echo "  resolved $path: took rel branch's content"
            ;;
    esac
done < <(git diff --name-only --diff-filter=U)

# Report any remaining (unexpected) conflicts.
remaining="$(git diff --name-only --diff-filter=U)"
if [ -n "$remaining" ]; then
    echo "" >&2
    echo "Unresolved conflicts remain in:" >&2
    echo "$remaining" | sed 's/^/  /' >&2
    echo "" >&2
    echo "Resolve manually, then 'git add' the file(s) and 'git commit'." >&2
    exit 2
fi

# A clean merge with no conflicts means nothing was staged for merging --
# bail out rather than pretend.
MERGE_HEAD_PATH="$(git rev-parse --git-path MERGE_HEAD)"
if [ ! -f "$MERGE_HEAD_PATH" ]; then
    echo "" >&2
    echo "No merge in progress; nothing was changed." >&2
    exit 1
fi

# Restore release-only files to their pre-merge (HEAD) values where the
# merge wandered. During a merge, `git checkout HEAD -- <path>` resets
# both the index and working tree to the pre-merge commit's content.
echo ""
for f in "${RELEASE_ONLY_FILES[@]}"; do
    expected="$(git show "HEAD:$f")"
    actual="$(cat "$f")"
    if [ "$expected" != "$actual" ]; then
        git checkout HEAD -- "$f"
        echo "  reset $f: '$actual' -> '$expected'"
    fi
done

for d in "${RELEASE_ONLY_DIRS[@]}"; do
    while IFS= read -r f; do
        [ -z "$f" ] && continue
        if git cat-file -e "HEAD:$f" 2>/dev/null; then
            git checkout HEAD -- "$f"
            echo "  reset $f"
        else
            git rm -f -- "$f" >/dev/null
            echo "  removed $f (not present on main)"
        fi
    done < <(git diff --cached --name-only HEAD -- "$d")
done

# Summary so a human can eyeball that release-only files are correct.
echo ""
echo "Merge staged. Verify these values look right for main, then commit:"
for f in "${RELEASE_ONLY_FILES[@]}"; do
    printf "  %-32s = %s\n" "$f" "$(cat "$f")"
done
echo ""
echo "Then:  git diff --staged && git commit"
