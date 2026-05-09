---
name: rstudio-update-electron
description: Use when updating the Electron version in the RStudio repository, e.g. bumping to a new release
---

# Update Electron

Updates the pinned Electron version across the RStudio codebase and verifies the result.

## Arguments

The user provides the **target Electron version** (e.g. `39.8.4`).

## Steps

### 1. Verify starting branch

Before doing anything else, confirm the current branch is `main`. Starting off anything other than `main` would base the work on the wrong commit.

```bash
git branch --show-current
```

If the output is not `main`, **stop immediately** and warn the user that they must switch to `main` (and pull the latest) before re-running this skill. Do not proceed with any further steps.

### 2. Update `NEWS.md`

Find the `### Dependencies` section and update the Electron version line:

```
- Electron <NEW_VERSION>
```

### 3. Update `src/node/desktop/package.json` and lockfile

Run from `src/node/desktop`:

```bash
cd src/node/desktop && npm install --save-dev --save-exact electron@<NEW_VERSION>
```

The `--save-exact` flag is **required** — Electron must be pinned to an exact version (e.g. `"electron": "39.8.5"`, not `"^39.8.5"`). This prevents uncontrolled upgrades across major/minor versions.

This updates both `package.json` and `package-lock.json` in one step. Confirm the command exits successfully (exit code 0). If it fails, report the error and stop.

After the install, verify that:
1. `package.json` has the exact version with **no `^` or `~` prefix**.
2. `package-lock.json` contains the new Electron version.

The lockfile must be included in the commit — if it drifts from the manifest, `npm ci` in CI will fail.

### 4. Run tests

Run from `src/node/desktop`:

```bash
cd src/node/desktop && npm test
```

Confirm the command exits successfully. If tests fail, report the failures and stop.

### 5. Done

Report that the Electron version has been updated and both `npm i` and `npm test` passed.
