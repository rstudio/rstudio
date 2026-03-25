---
name: rstudio-update-electron
description: Use when updating the Electron version in the RStudio repository, e.g. bumping to a new release
---

# Update Electron

Updates the pinned Electron version across the RStudio codebase and verifies the result.

## Arguments

The user provides the **target Electron version** (e.g. `39.8.4`).

## Steps

### 1. Update `NEWS.md`

Find the `### Dependencies` section and update the Electron version line:

```
- Electron <NEW_VERSION>
```

### 2. Update `src/node/desktop/package.json` and lockfile

Run from `src/node/desktop`:

```bash
cd src/node/desktop && npm install electron@<NEW_VERSION> --save-dev
```

This updates both `package.json` and `package-lock.json` in one step. Confirm the command exits successfully (exit code 0). If it fails, report the error and stop.

After the install, verify that `package-lock.json` contains the new Electron version. The lockfile must be included in the commit — if it drifts from the manifest, `npm ci` in CI will fail.

### 3. Run tests

Run from `src/node/desktop`:

```bash
cd src/node/desktop && npm test
```

Confirm the command exits successfully. If tests fail, report the failures and stop.

### 4. Done

Report that the Electron version has been updated and both `npm i` and `npm test` passed.
