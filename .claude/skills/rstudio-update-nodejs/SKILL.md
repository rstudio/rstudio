---
name: rstudio-update-nodejs
description: Use when updating Node.js versions in the RStudio repository, e.g. bumping to a new release. Handles both build-time Node (RSTUDIO_NODE_VERSION, used for building Electron/GWT) and installed Node (RSTUDIO_INSTALLED_NODE_VERSION, shipped with the product). This skill is for macOS and Linux only.
---

# Update Node.js Versions

Updates the pinned Node.js version(s) across the RStudio codebase, uploads binaries to S3, verifies the install, and opens a PR.

RStudio maintains **two distinct Node.js versions**:

- **Build-time Node** (`RSTUDIO_NODE_VERSION`) — used for building Electron, GWT, and other product components. Not shipped to users.
- **Installed Node** (`RSTUDIO_INSTALLED_NODE_VERSION`) — stripped-down `node` binary shipped with the product, used by copilot and Posit AI.

These versions can differ. The skill handles updating either or both in a single invocation.

## Arguments

The user specifies which version(s) to update:

- **Build-time only**: e.g. "update build node to 22.14.0"
- **Installed only**: e.g. "update installed node to 22.23.0"
- **Both**: e.g. "update both node versions to 22.14.0" or "update build node to 22.14.0 and installed to 22.23.0"

Each version is a semver string like `22.14.0` (no `v` prefix).

## Steps

### 1. Verify starting branch

Before doing anything else, confirm the current branch is `main`. This skill creates a new feature branch off the current branch, so starting off anything other than `main` would base the work on the wrong commit.

```bash
git branch --show-current
```

If the output is not `main`, **stop immediately** and warn the user that they must switch to `main` (and pull the latest) before re-running this skill. Do not proceed with any further steps.

### 2. Validate the version(s)

Confirm each provided version matches the format `X.Y.Z` (digits and dots only). If any version doesn't match, ask the user to correct it before proceeding.

### 3. Check prerequisites

Before modifying any files, verify that AWS credentials and required tools are in place. The upload script needs working AWS access, and discovering a credential problem after editing files wastes effort.

Run these checks and stop if any fail:

```bash
command -v aws >/dev/null 2>&1 && echo "aws: ok" || echo "aws: MISSING"
command -v wget >/dev/null 2>&1 && echo "wget: ok" || echo "wget: MISSING"
aws sts get-caller-identity
```

If `aws` or `wget` is missing, tell the user to install it. If `aws sts get-caller-identity` fails, tell the user to configure AWS credentials (e.g. `aws sso login` or `aws configure sso` if SSO isn't set up yet) and try again.

### 4. Create a branch

Use the following naming convention:

- Build-time only: `feature/update-node-build-<VERSION>`
- Installed only: `feature/update-node-installed-<VERSION>`
- Both, same version: `feature/update-node-<VERSION>`
- Both, different versions: `feature/update-node-build-<V1>-installed-<V2>`

```bash
git checkout -b <BRANCH_NAME>
```

### 5. Update version strings

Each file uses a different syntax — match the existing pattern exactly. Only update files for the version(s) being changed.

Before editing, note the current values of `RSTUDIO_NODE_VERSION` and `RSTUDIO_INSTALLED_NODE_VERSION` from `cmake/globals.cmake` so you can find-and-replace accurately.

#### 5a. Build-time Node files (only if updating build)

**`cmake/globals.cmake`** (~line 243) — CMake cache variable:
```cmake
set(RSTUDIO_NODE_VERSION "<VERSION>" CACHE INTERNAL "Node version for building")
```

**`dependencies/tools/rstudio-tools.sh`** (~line 58) — bash export with quotes:
```bash
export RSTUDIO_NODE_VERSION="<VERSION>"
```

**`dependencies/tools/rstudio-tools.cmd`** (~line 54) — Windows batch, no quotes:
```cmd
set RSTUDIO_NODE_VERSION=<VERSION>
```

**`src/gwt/build.xml`** (~line 234) — XML property attribute:
```xml
<property name="node.version" value="<VERSION>"/>
```

**`src/node/desktop/envvars.sh`** (~lines 6, 8) — two hardcoded paths, one with `-arm64` suffix:
```bash
NODE_PATH=$(readlink -fn ../../../dependencies/common/node/<VERSION>-arm64/bin)
```
```bash
NODE_PATH=$(realpath ../../../dependencies/common/node/<VERSION>/bin)
```

**`src/node/CMakeNodeTools.txt`** (~line 22) — CMake fallback default:
```cmake
set(RSTUDIO_NODE_VERSION "<VERSION>")
```

**`src/node/desktop/.vscode/settings.json`** (~line 81) — JSON string with backslash path separators:
```json
"PATH": "${workspaceFolder}\\..\\..\\..\\dependencies\\common\\node\\<VERSION>;${env:PATH}"
```

#### 5b. Installed Node files (only if updating installed)

**`cmake/globals.cmake`** (~line 246) — CMake cache variable:
```cmake
set(RSTUDIO_INSTALLED_NODE_VERSION "<VERSION>" CACHE INTERNAL "Node version installed with product")
```

**`dependencies/tools/rstudio-tools.sh`** (~line 70) — bash export with quotes:
```bash
export RSTUDIO_INSTALLED_NODE_VERSION="<VERSION>"
```

**`dependencies/tools/rstudio-tools.cmd`** (~line 57) — Windows batch, no quotes:
```cmd
set RSTUDIO_INSTALLED_NODE_VERSION=<VERSION>
```

**`dependencies/tools/upload-node.sh`** (~line 13) — bash variable with `v` prefix:
```bash
NODE_VERSION="v<VERSION>"
```
The `v` prefix is required here and only here. All other files use the bare version.

**`NEWS.md`** — in the `### Dependencies` section, update the Node.js line:
```
- Node.js <VERSION> (copilot, Posit AI)
```

Build-time updates do NOT get a NEWS.md entry — only installed node does.

#### 5c. Verify edits

Three files contain both versions (`cmake/globals.cmake`, `rstudio-tools.sh`, `rstudio-tools.cmd`). When updating both versions, take care not to cross-contaminate the two variables in these files.

After editing, review the diff:

```bash
git diff
```

Confirm:
- Only the intended version strings changed
- Syntax matches each file's format (CMake quotes, bash quotes, batch no-quotes, XML attributes, JSON strings)
- The `v` prefix appears ONLY in `upload-node.sh`

### 6. Upload to S3

Upload Node.js binaries for each unique new version using `dependencies/tools/upload-node.sh`. This script downloads platform archives from nodejs.org and uploads them to the `rstudio-buildtools` S3 bucket.

The script reads its version from the hardcoded `NODE_VERSION` variable on line 13 (set in Step 5b for installed updates). The upload covers all platforms: darwin-arm64, darwin-x64, linux-arm64, linux-x64, win-x64, win-arm64.

**If updating installed** (or both to the same version): `upload-node.sh` already has the correct version from Step 5b. Run it:

```bash
bash dependencies/tools/upload-node.sh
```

Confirm exit code 0.

**If updating build only**: `upload-node.sh` wasn't edited in Step 5 (it tracks the installed version by convention). Temporarily edit it to `NODE_VERSION="v<BUILD_VERSION>"`, run it, then revert to the original installed value:

```bash
# Save the original value, set to build version, upload, restore
bash dependencies/tools/upload-node.sh
git checkout dependencies/tools/upload-node.sh
```

**If updating both to different versions**: `upload-node.sh` was set to the installed version in Step 5b. Upload the build version first (temporarily), then the installed version:

1. Edit `upload-node.sh` to `NODE_VERSION="v<BUILD_VERSION>"`
2. Run `bash dependencies/tools/upload-node.sh` — confirm exit code 0
3. Edit `upload-node.sh` back to `NODE_VERSION="v<INSTALLED_VERSION>"` (its committed value)
4. Run `bash dependencies/tools/upload-node.sh` — confirm exit code 0

If any upload fails, report the error and stop.

After uploading, remove the downloaded archives left in the repo root by the upload script (they match `node-v*` and are not gitignored):

```bash
rm -f node-v<VERSION>-*
```

If both versions were uploaded and they differ, remove both:

```bash
rm -f node-v<BUILD_VERSION>-* node-v<INSTALLED_VERSION>-*
```

### 7. Verify the install

Run the dependency installer to confirm the S3 binaries download and extract correctly. The script must be run from `dependencies/common/` because it invokes `./install-node` and `./install-yarn` via relative paths. It installs into `dependencies/common/node/` which is gitignored.

First, remove any cached local installs for the new version(s) so the installer is forced to download fresh from S3. The `install-node` script skips download when it finds an existing `node/<version>/bin/node`, so stale local copies would mask a broken upload.

```bash
cd dependencies/common
# Remove cached installs for the new version(s) to force a fresh download.
# Glob covers both plain and suffixed dirs (e.g. 22.14.0, 22.14.0-arm64, 22.14.0-installed, 22.14.0-arm64-installed).
rm -rf node/<VERSION>*
```

If both versions are being updated to different values, remove both:

```bash
rm -rf node/<BUILD_VERSION>* node/<INSTALLED_VERSION>*
```

Then run the installer:

```bash
bash install-npm-dependencies
```

Confirm exit code 0. If the install fails, report the error and stop. This also installs the new Node.js locally for development use.

### 8. Commit and open a PR

Commit all modified files and open a pull request.

**Commit message and PR title** (varies by mode):

- Build-time only: `Update build-time Node.js to <VERSION>`
- Installed only: `Update installed Node.js to <VERSION>`
- Both, same version: `Update Node.js to <VERSION>`
- Both, different versions: `Update Node.js (build: <V1>, installed: <V2>)`

**PR body** should mention the version bump(s), that the S3 upload and local install were verified, and list old→new for each version that changed.
