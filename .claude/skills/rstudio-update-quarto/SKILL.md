---
name: rstudio-update-quarto
description: Use when updating the Quarto version in the RStudio repository, e.g. bumping to a new release. This skill is for macOS and Linux only.
---

# Update Quarto

Updates the pinned Quarto version across the RStudio codebase, mirrors the new release into the rstudio-buildtools S3 bucket, verifies the install, and opens a PR.

## Arguments

The user provides the **target Quarto version** (e.g. `1.9.36`).

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

Before modifying any files, verify that AWS credentials and required tools are in place. The upload script needs working AWS access, and discovering a credential problem after editing files wastes effort.

Run these checks and stop if any fail:

```bash
command -v aws >/dev/null 2>&1 && echo "aws: ok" || echo "aws: MISSING"
command -v wget >/dev/null 2>&1 && echo "wget: ok" || echo "wget: MISSING"
aws sts get-caller-identity
```

If `aws` or `wget` is missing, tell the user to install it. If `aws sts get-caller-identity` fails, tell the user to configure AWS credentials (e.g. `aws sso login` or `aws configure sso` if SSO isn't set up yet) and try again.

### 4. Create a branch

```bash
git checkout -b feature/update-quarto-<VERSION>
```

### 5. Update version strings

Update the Quarto version in the following four files. Each file uses a slightly different syntax — match the existing pattern exactly.

#### `NEWS.md`

In the `### Dependencies` section, update the Quarto line:

```
- Quarto <VERSION>
```

#### `dependencies/tools/upload-quarto.sh`

Bash assignment, no quotes:

```bash
QUARTO_VERSION=<VERSION>
```

Unlike the copilot upload script, `upload-quarto.sh` does not accept a CLI argument — it reads the version from this hardcoded value, so it must be updated before running the upload step.

#### `dependencies/common/install-quarto`

Bash assignment, no quotes:

```bash
QUARTO_VERSION=<VERSION>
```

#### `dependencies/windows/install-dependencies.cmd`

Windows batch syntax, no quotes:

```cmd
set QUARTO_VERSION=<VERSION>
```

After editing, verify all four files contain the new version string.

### 6. Upload to S3

Run the upload script. It downloads release archives for all supported platforms (linux-amd64, linux-arm64, macos, win) from the Quarto GitHub releases page and copies each to the `rstudio-buildtools` S3 bucket.

```bash
bash dependencies/tools/upload-quarto.sh
```

Confirm exit code 0 and that every platform archive was uploaded. If any download or upload fails, report the error and stop.

The upload script downloads each archive into the current working directory and does **not** clean up afterward, so remove the leftover archives once the upload succeeds:

```bash
rm -f quarto-<VERSION>-*
```

### 7. Verify the install

The default tools root (`/opt/rstudio-tools/...`) requires elevated privileges, so use a temporary directory to verify the install without needing `sudo`.

```bash
VERIFY_DIR="$(mktemp -d)"
trap 'rm -rf "$VERIFY_DIR"' EXIT
RSTUDIO_TOOLS_ROOT="$VERIFY_DIR" bash dependencies/common/install-quarto
```

Confirm exit code 0. The install script checks the resulting `quarto --version` against the expected value, so a successful run end-to-end is meaningful verification that the new release is usable. The `trap` ensures the temp directory is cleaned up whether the install succeeds or fails. If the install fails, report the error and stop.

After verification, tell the user they will need to re-run `dependencies/common/install-quarto` themselves (with appropriate privileges) to install the new Quarto into their dev environment.

### 8. Commit and open a PR

Commit the four modified files and open a pull request:

- **Branch**: `feature/update-quarto-<VERSION>`
- **Commit message**: `Update Quarto to <VERSION>`
- **PR title**: `Update Quarto to <VERSION>`
- **PR body**: mention the version bump and that the S3 upload and local install were verified
