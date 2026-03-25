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

### 2. Update `src/node/desktop/package.json`

In `devDependencies`, set the `electron` package to the exact new version:

```json
"electron": "<NEW_VERSION>"
```

### 3. Install dependencies

Run from `src/node/desktop`:

```bash
cd src/node/desktop && npm i
```

Confirm the command exits successfully (exit code 0). If it fails, report the error and stop.

### 4. Run tests

Run from `src/node/desktop`:

```bash
cd src/node/desktop && npm test
```

Confirm the command exits successfully. If tests fail, report the failures and stop.

### 5. Done

Report that the Electron version has been updated and both `npm i` and `npm test` passed.
