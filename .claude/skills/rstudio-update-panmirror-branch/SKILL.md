---
name: rstudio-update-panmirror-branch
description: Use when starting work on a new RStudio release cycle (after the previous release has branched) and panmirror, the visual editor, must be pinned to a new release/rstudio-<release> branch of the quarto-dev/quarto repository, or when PANMIRROR_BRANCH in the install-panmirror scripts needs to point at a new cycle's branch.
---

# Update the panmirror branch

RStudio builds panmirror (the visual editor) from a branch of
https://github.com/quarto-dev/quarto named for the release cycle. When a cycle starts, branch
quarto's `main` as `release/rstudio-<release>` and point `PANMIRROR_BRANCH` at it in both
install scripts.

`<release>` is the lowercase, hyphenated release name from `version/RELEASE`: `Parrot Balsam`
becomes `parrot-balsam`, so the branch is `release/rstudio-parrot-balsam`.

## Arguments

Optional: the release name. If none is given, read it from `version/RELEASE` and confirm it with
the user, since `version/RELEASE` only names the new cycle once it has been bumped.

## Steps

### 1. Verify starting branch

```bash
git branch --show-current
```

If it is not `main`, stop and ask the user to switch to an up-to-date `main` before re-running.

### 2. Read the current pin

```bash
git grep -n "PANMIRROR_BRANCH=" -- dependencies/common/install-panmirror \
   dependencies/windows/install-panmirror/clone-panmirror-repo.cmd
```

Both files must name the same branch. If they differ, stop and report it.

The current value takes one of two forms:

- `release/rstudio-<old-release>` -- the branch cut at the start of the previous cycle.
- `release/rstudio-<old-release>-NN` (`-01`, `-02`, ...) -- a branch created after the cut to
  carry a panmirror fix. Treat it exactly like the first form: replace the whole value. The
  suffix never carries into the new cycle.

If the current value already names `<release>`, the work is done; stop and say so.

### 3. Create the quarto branch

Branches named in the scripts are immutable (the comment above `PANMIRROR_BRANCH` explains why),
so only ever create a branch -- never move or push to an existing one.

Check whether it already exists:

```bash
QUARTO_URL=https://github.com/quarto-dev/quarto.git
git ls-remote "$QUARTO_URL" "refs/heads/release/rstudio-<release>" \
   "refs/heads/release/rstudio-<release>-*"
```

If anything matches, someone has already cut it. Do not recreate or update it; tell the user what
exists (name and SHA) and ask whether to pin to it.

Otherwise, get quarto's current `main` tip:

```bash
git ls-remote --exit-code "$QUARTO_URL" refs/heads/main
```

**Ask the user before creating the branch**, showing the name and the `main` SHA. It writes to
another team's repository and cannot be quietly undone once builds have pulled it.

Create it through the GitHub API, which needs push access to quarto-dev/quarto but no local
clone:

```bash
gh api repos/quarto-dev/quarto/git/refs \
   -f ref="refs/heads/release/rstudio-<release>" -f sha="<main-sha>"
```

The call fails with `422 Reference already exists` rather than overwriting, so it cannot move an
existing branch. A `404` means the account lacks push access; ask the user to have someone with
access create the branch, then continue from step 4.

Confirm it exists at the expected SHA:

```bash
git ls-remote --exit-code "$QUARTO_URL" "refs/heads/release/rstudio-<release>"
```

### 4. Check the Node.js requirement

panmirror's root `package.json` declares `engines.node`, which Yarn enforces at install time. A new
cut can raise it above RStudio's build-time Node.js, which breaks every build.

```bash
gh api "repos/quarto-dev/quarto/contents/package.json?ref=release/rstudio-<release>" \
   --jq .content | base64 --decode | grep -A2 '"engines"'
grep -n RSTUDIO_NODE_VERSION= dependencies/tools/rstudio-tools.sh
```

If `RSTUDIO_NODE_VERSION` does not satisfy the range, stop and tell the user: Node.js must be
raised first, in a separate PR (the rstudio-update-nodejs skill), merged before this one.

### 5. Create a branch and update both scripts

```bash
git checkout -b feature/panmirror-<release>
```

Set the value in each file, matching the existing syntax:

- `dependencies/common/install-panmirror`: `PANMIRROR_BRANCH="release/rstudio-<release>"`
- `dependencies/windows/install-panmirror/clone-panmirror-repo.cmd`:
  `set PANMIRROR_BRANCH=release/rstudio-<release>`

Change only that line; the surrounding comments are cycle-independent. Then confirm the old name
is gone:

```bash
git grep -nF "release/rstudio-<old-release>" -- . ':!version/news' ':!NEWS.md'
git diff --stat
```

The grep should print nothing, and the diff should show exactly two files, one line each.

### 6. Commit and open a PR

No NEWS.md entry: pinning a new cycle's branch is not user-facing. (A `-NN` fix branch gets a NEWS
entry for the fix it carries, written with that fix, not by this skill.)

- **Commit message**: `Pin panmirror to release/rstudio-<release>`
- **PR title**: same as the commit message.
- **PR body**: the old and new branch names, the quarto `main` SHA the branch was cut from, and the
  two files changed. If step 4 found a raised Node.js requirement, name the PR that met it.
- **Milestone**: match `version/RELEASE` against
  `gh api repos/rstudio/rstudio/milestones --jq '.[].title'` (titles look like
  `2026.11 (Montauk Daisy)`). Set it if one matches; never create one.

Open the PR only once the user has said to.
