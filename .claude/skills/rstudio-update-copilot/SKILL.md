---
name: rstudio-update-copilot
description: Use when updating the copilot-language-server version in the RStudio repository, e.g. bumping to a new release. This skill is for macOS and Linux only.
---

# Update Copilot Language Server

Updates the pinned copilot-language-server version across the RStudio codebase, uploads the new version to S3, verifies the install, and opens a PR.

## Arguments

The user provides the **target copilot-language-server version** (e.g. `1.459.0`).

## Steps

### 1. Verify starting branch

Before doing anything else, confirm the current branch is `main`. This skill creates a new feature branch off the current branch, so starting off anything other than `main` would base the work on the wrong commit.

```bash
git branch --show-current
```

If the output is not `main`, **stop immediately** and warn the user that they must switch to `main` (and pull the latest) before re-running this skill. Do not proceed with any further steps.

### 2. Validate the version

Confirm the version argument matches the format `X.Y.Z` (digits and dots only). If it doesn't, ask the user to correct it before proceeding.

### 3. Check prerequisites

Before modifying any files, verify that AWS credentials are in place. The upload script needs working AWS access, and discovering a credential problem after editing files wastes effort.

Run these checks and stop if any fail:

```bash
command -v aws >/dev/null 2>&1 && echo "aws: ok" || echo "aws: MISSING"
command -v wget >/dev/null 2>&1 && echo "wget: ok" || echo "wget: MISSING"
aws sts get-caller-identity
```

If `aws` or `wget` is missing, tell the user to install it. If `aws sts get-caller-identity` fails, tell the user to configure AWS credentials (e.g. `aws sso login` or `aws configure sso` if SSO isn't set up yet) and try again.

### 4. Create a branch

```bash
git checkout -b feature/update-copilot-language-server-<VERSION>
```

### 5. Update version strings

Update `COPILOT_VERSION` in the following four files. Each file uses a slightly different syntax — match the existing pattern exactly.

#### `NEWS.md`

In the `### Dependencies` section, update the Copilot Language Server line:

```
- Copilot Language Server <VERSION>
```

#### `dependencies/common/install-copilot-language-server`

```bash
COPILOT_VERSION="<VERSION>"
```

#### `dependencies/tools/upload-copilot-language-server.sh`

This file uses the version as both a default value and a command-line argument fallback:

```bash
COPILOT_VERSION="${1:-<VERSION>}"
```

#### `dependencies/windows/install-dependencies.cmd`

Windows batch syntax — no quotes:

```cmd
set COPILOT_VERSION=<VERSION>
```

After editing, verify all four files contain the new version string.

### 6. Upload to S3

Run the upload script, passing the new version as an argument. This downloads the release from GitHub and uploads it to the rstudio-buildtools S3 bucket.

```bash
bash dependencies/tools/upload-copilot-language-server.sh <VERSION>
```

Confirm exit code 0 and the "Successfully uploaded" message. If it fails, report the error and stop.

### 7. Verify the install

The default tools root (`/opt/rstudio-tools/...`) requires elevated privileges, so use a temporary directory to verify the S3 download without needing `sudo`.

```bash
VERIFY_DIR="$(mktemp -d)"
trap 'rm -rf "$VERIFY_DIR"' EXIT
RSTUDIO_TOOLS_ROOT="$VERIFY_DIR" bash dependencies/common/install-copilot-language-server
```

Confirm exit code 0. The `trap` ensures the temp directory is cleaned up whether the install succeeds or fails. If the install fails, report the error and stop.

After verification, tell the user they will need to re-run `dependencies/common/install-copilot-language-server` themselves (with appropriate privileges) to install the new copilot-language-server into their dev environment.

### 8. Commit and open a PR

Commit the four modified files and open a pull request:

- **Branch**: `feature/update-copilot-language-server-<VERSION>`
- **Commit message**: `Update copilot-language-server to <VERSION>`
- **PR title**: `Update copilot-language-server to <VERSION>`
- **PR body**: mention the version bump and that the S3 upload and local install were verified
