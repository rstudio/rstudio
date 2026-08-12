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
for tool in aws wget curl shasum; do
   command -v "$tool" >/dev/null 2>&1 && echo "$tool: ok" || echo "$tool: MISSING"
done
aws sts get-caller-identity
```

The upload script uses `wget`; step 6 verifies the mirror with `curl` and `shasum`. If any tool is missing, tell the user to install it. If `aws sts get-caller-identity` fails, tell the user to configure AWS credentials (e.g. `aws sso login` or `aws configure sso` if SSO isn't set up yet) and try again.

Do not skip this check on the assumption that the upload script handles it. `upload-quarto.sh` starts with `aws sts get-caller-identity || aws sso login`, which opens an interactive SSO login — that has nowhere to go in a non-interactive session and leaves the script waiting.

### 4. Create a branch

```bash
git checkout -b feature/update-quarto-<VERSION>
```

### 5. Update version strings

Update the Quarto version in the following four files. Each file uses a slightly different syntax — match the existing pattern exactly.

#### `NEWS.md`

In the `### Dependencies` section of the release notes at the top of the file, set the Quarto line:

```
- Quarto <VERSION>
```

**There is often no Quarto line to update.** That section lists only the dependencies modified during the current release cycle, so when the notes are rotated for a new release, the unchanged dependency lines are pruned (see commit `e8b91f112a`). If no Quarto line is present, add one, keeping the ordering used before the prune: Ace, MathJax, Copilot Language Server, Electron, Node.js, Quarto, xterm.js.

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

After editing, confirm all four files contain the new version and that nothing else still pins the old one:

```bash
git grep -n "QUARTO_VERSION\|^- Quarto" -- NEWS.md dependencies
git grep -nF "<OLD_VERSION>"
```

The first command should show only the new version. Use `-F` on the second: without it the dots in the version are regex wildcards, which match unrelated text in minified sources and bury the real hits in hundreds of kilobytes of output.

The second command is expected to match the archived release notes under `version/news/os/` — **leave those alone.** They record what shipped in past releases, so rewriting them would falsify release history. If the old version turns up anywhere outside `version/news/os/`, stop and report it rather than guessing whether it needs updating.

### 6. Upload to S3

Run the upload script. It downloads release archives for all supported platforms (linux-amd64, linux-arm64, macos, win) from the Quarto GitHub releases page and copies each to the `rstudio-buildtools` S3 bucket.

```bash
bash dependencies/tools/upload-quarto.sh
```

**Do not treat exit code 0 as proof the upload worked.** The script has no `set -e` and does not check any command's status: a failed `wget` still falls through to `aws s3 cp`, the loop continues to the next platform, and the script's exit status reflects only its final command. It also produces hundreds of lines of progress output, so a failure partway up the log is easy to miss.

Verify the result directly instead. First confirm the downloaded archives are intact, using the checksums published alongside the release. Size alone proves nothing here: an interrupted `wget` leaves a partial file, the script uploads it without complaint, and a partial local file matches its own partial upload.

```bash
CHECKSUMS="${TMPDIR:-/tmp}/quarto-<VERSION>-checksums.txt"
curl -fsSL "https://github.com/quarto-dev/quarto-cli/releases/download/v<VERSION>/quarto-<VERSION>-checksums.txt" -o "$CHECKSUMS"
grep -E "quarto-<VERSION>-(linux-amd64\.tar\.gz|linux-arm64\.tar\.gz|macos\.tar\.gz|win\.zip)$" "$CHECKSUMS" | shasum -a 256 -c
```

Expect exactly four `OK` lines. Anything less — a `FAILED` line, or fewer than four files checked — means an archive is corrupt or the checksum list did not download, so re-run the upload script (`wget -c` resumes the partial file) and check again. On Linux, `sha256sum -c` is equivalent.

Then confirm the upload itself was complete, by comparing the object sizes against those same verified archives:

```bash
aws s3 ls "s3://rstudio-buildtools/quarto/<VERSION>/"
ls -l quarto-<VERSION>-*
```

Both report sizes in bytes, so the four pairs should match exactly. Verified local bytes plus a matching object length is sufficient: S3 checks its own payload integrity on upload, so a length-preserving corruption in transit is not a realistic failure. Finally, the mirror is only useful if it is publicly readable — the upload requests `--acl public-read`, but a bucket policy that rejects ACLs can leave the objects private without failing the upload — so confirm each one is reachable anonymously:

```bash
for f in linux-amd64.tar.gz linux-arm64.tar.gz macos.tar.gz win.zip; do
   url="https://rstudio-buildtools.s3.amazonaws.com/quarto/<VERSION>/quarto-<VERSION>-$f"
   echo "$f -> $(curl -s -o /dev/null -w '%{http_code}' -r 0-0 "$url")"
done
```

A ranged request returns `206` for a readable object; `403` means the object is private. If any archive is missing, mismatched, or private, report it and stop.

If an archive ever needs to be re-uploaded, or the mirror's contents are in doubt, the end-to-end check is to fetch each object from its public URL and run the same `shasum -a 256 -c` against it. That covers integrity and readability in one pass, at the cost of re-downloading roughly 700 MB.

The upload script downloads each archive into the current working directory and does **not** clean up afterward. That is roughly 700 MB of untracked files in the repository root, so remove them once the upload is verified:

```bash
rm -f quarto-<VERSION>-*
git status --short
```

`git status` should show exactly the four tracked files modified in step 5 and no `quarto-<VERSION>-*` archives. It is **not** expected to be clean — those edits are not committed until step 8.

### 7. Verify the install

The default tools root (`/opt/rstudio-tools/...`) requires elevated privileges, so use a temporary directory to verify the install without needing `sudo`.

```bash
VERIFY_DIR="$(mktemp -d)"
trap 'rm -rf "$VERIFY_DIR"' EXIT
RSTUDIO_TOOLS_ROOT="$VERIFY_DIR" bash dependencies/common/install-quarto
echo "install exit=$?"

# the installed binary must report the new version
"$VERIFY_DIR/quarto/bin/quarto" --version

# a second run should take the "already installed" path
RSTUDIO_TOOLS_ROOT="$VERIFY_DIR" bash dependencies/common/install-quarto
```

Run `quarto --version` yourself, as above; do not infer it from the install script's output. The script does compare versions (`install-quarto:38-44`), but that is a pre-install early exit for a tools root that already has Quarto — against the empty temporary root it is skipped entirely, so exit code 0 on its own proves only that the archive downloaded and extracted. Running the binary is what shows the release is usable, since it exercises the bundled Deno runtime. The `trap` cleans up the temporary directory whether the install succeeds or fails. If the install fails, or the reported version is not the expected one, report it and stop.

Note that this step does **not** verify the S3 mirror from step 6: `install-quarto:31` downloads from the Quarto GitHub releases page, and the `RSTUDIO_BUILDTOOLS` alternative on the following line is commented out (as is the equivalent in `install-dependencies.cmd`). The mirror is a fallback, which is why step 6 verifies it directly.

After verification, tell the user they will need to re-run `dependencies/common/install-quarto` themselves (with appropriate privileges) to install the new Quarto into their dev environment.

### 8. Commit and open a PR

Commit the four modified files and open a pull request:

- **Branch**: `feature/update-quarto-<VERSION>`
- **Commit message**: `Update Quarto to <VERSION>`
- **PR title**: `Update Quarto to <VERSION>`
- **PR body**: the old and new versions, the files updated, and what verification actually established — that all four archives are mirrored and publicly readable, and that the installed binary reports the new version. Describe the checks that were run, not the steps this skill lists.
- **Milestone**: match the release name in `version/RELEASE` against the existing milestones (`gh api repos/rstudio/rstudio/milestones --jq '.[].title'`) and set it if one matches. Never create a milestone.

This is a dependency bump with no associated GitHub issue, so no issue reference is needed. No NEWS.md entry beyond the `### Dependencies` line from step 5 is needed either.
