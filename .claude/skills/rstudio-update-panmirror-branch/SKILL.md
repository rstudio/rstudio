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

The rest of this skill calls the full name of the branch being pinned `<branch>`.

If anything matches, someone has already cut it. Do not recreate or update it; tell the user what
exists (name and SHA) and ask which one to pin to. That full name, suffix included, is `<branch>`;
skip to step 4.

Otherwise `<branch>` is `release/rstudio-<release>`. Get quarto's current `main` tip:

```bash
git ls-remote --exit-code "$QUARTO_URL" refs/heads/main
```

**Ask the user before creating the branch**, showing the name and the `main` SHA. It writes to
another team's repository and cannot be quietly undone once builds have pulled it.

Create it through the GitHub API, which needs push access to quarto-dev/quarto but no local
clone:

```bash
gh api repos/quarto-dev/quarto/git/refs \
   -f ref="refs/heads/<branch>" -f sha="<main-sha>"
```

The call fails with `422 Reference already exists` rather than overwriting, so it cannot move an
existing branch. A `404` means the account lacks push access; ask the user to have someone with
access create the branch, then continue from step 4.

Confirm it exists at the expected SHA:

```bash
git ls-remote --exit-code "$QUARTO_URL" "refs/heads/<branch>"
```

### 4. Check the Node.js and package manager requirements

A new cut can change how quarto expects to be built, which breaks every RStudio build. Read
`engines.node` and `packageManager` from quarto's root `package.json`, and what RStudio builds
with:

```bash
gh api "repos/quarto-dev/quarto/contents/package.json?ref=<branch>" \
   --jq .content | base64 --decode | grep -E -A2 '"(engines|packageManager)"'
grep -n RSTUDIO_NODE_VERSION= dependencies/tools/rstudio-tools.sh
curl -fsSL https://nodejs.org/dist/index.json \
   | jq -r '.[] | select(.version=="v<RSTUDIO_NODE_VERSION>") | .npm'
sed -n '/<target name="panmirror"/,/<\/target>/p' src/gwt/build.xml | grep 'exec executable'
```

The last command shows which tool the `panmirror` target in `src/gwt/build.xml` runs:
`${npm.bin}` or `${yarn.bin}`.

- If `RSTUDIO_NODE_VERSION` does not satisfy `engines.node`, stop and tell the user: Node.js must
  be raised first, in a separate PR (the rstudio-update-nodejs skill), merged before this one.
- If `packageManager` names a different tool than the `panmirror` target runs, stop and tell the
  user: the target must be switched to that tool in this PR, alongside the pin.
- If `packageManager` is `npm@<X.Y.Z>` and the npm bundled with `RSTUDIO_NODE_VERSION` has a
  different major version, report it; the user decides whether that needs a Node.js bump.

### 5. Create a branch and update both scripts

```bash
git checkout -b feature/panmirror-<release>
```

Set the value in each file, matching the existing syntax:

- `dependencies/common/install-panmirror`: `PANMIRROR_BRANCH="<branch>"`
- `dependencies/windows/install-panmirror/clone-panmirror-repo.cmd`:
  `set PANMIRROR_BRANCH=<branch>`

Change only that line; the surrounding comments are cycle-independent. Then confirm the old name
is gone:

```bash
git grep -nF "release/rstudio-<old-release>" -- . ':!version/news' ':!NEWS.md' ':!.claude/skills'
git diff --stat
```

The grep should print nothing, and the diff should show the two scripts, one line each.

#### If step 4 found a package manager change

Switch the `panmirror` target in `src/gwt/build.xml` to the new tool, for both the dependency
install and the build, and keep the diff to that target. The diff should then show the two
scripts plus `build.xml`.

#### Build panmirror from the new branch

Every new cut can bring new build tooling (vite, its plugins, the package manager), and only a
real build shows the scripts, the clone and `build.xml` still work together:

```bash
dependencies/common/install-panmirror
(cd src/gwt && ant -Dpanmirror.dir=./lib/quarto/apps/panmirror panmirror)
node --check src/gwt/www/js/panmirror/panmirror.js
```

Pass `panmirror.dir` explicitly: `build.xml` prefers a clone under `/opt/rstudio-tools` (or
`c:/rstudio-tools`) when one exists, as in the Jenkins builder images, and that clone is still on
the previous branch. Check that the `panmirror location:` line ant prints is the checkout's own
`src/gwt/lib/quarto`.

`BUILD SUCCESSFUL` is not enough on its own: a vite change once produced a bundle that built
cleanly but was a syntax error, so the visual editor never loaded. `node --check` (using the
Node.js from `dependencies/common/node/` if `node` is not on `PATH`) must print nothing.

If any of the three fails, or is not permitted in this session, stop and report it rather than
committing. A failure that needs a `build.xml` change goes in this PR as its own commit.

### 6. Commit and open a PR

No NEWS.md entry: pinning a new cycle's branch is not user-facing. (A `-NN` fix branch gets a NEWS
entry for the fix it carries, written with that fix, not by this skill.)

- **Commit message**: `Pin panmirror to <branch>`. Each `build.xml` change is its own commit, before
  the pin, e.g. `Build panmirror with npm`.
- **PR title**: same as the commit message.
- **PR body**: the old and new branch names, the quarto `main` SHA the branch was cut from, and the
  files changed. If step 4 found a raised Node.js requirement, name the PR that met it. If the
  package manager changed, say from what to what and which quarto commit made the switch. For each
  `build.xml` change, say what broke without it and how the target changed. Say that the bundle
  was built and passed `node --check`.
- **Milestone**: match `version/RELEASE` against
  `gh api repos/rstudio/rstudio/milestones --jq '.[].title'` (titles look like
  `2026.11 (Montauk Daisy)`). Set it if one matches; never create one.

Open the PR only once the user has said to.
