#!/usr/bin/env bash

# upload-nsis-multiuser.sh
#
# Downloads the NsisMultiUser framework (Include headers plus the bundled UAC
# and StdUtils plugins) from its GitHub repository at a pinned commit,
# repackages it into NsisMultiUser.zip (rooted at Include/ and Plugins/), and
# uploads it to the RStudio Build Tools (rstudio-buildtools) S3 bucket, where
# dependencies/windows/install-dependencies.cmd fetches it. Presumes you've
# already got AWS command line tools (awscli) installed, and configured with a
# valid AWS account.
#
# The Windows installer (package/win32/cmake/modules/NSIS.template.in) uses
# NsisMultiUser for per-user / all-users install support. NsisMultiUser cuts no
# tagged releases, so we pin a specific commit. The pinned version here MUST
# match CPACK_NSIS_MULTIUSER_VERSION (cmake/globals.cmake) and
# NSISMULTIUSER_VERSION (dependencies/windows/install-dependencies.cmd).
#
# The archive is built deterministically: the contents are fixed by the pinned
# commit, and file timestamps and entry order are normalized so re-running the
# script reproduces the same zip (verify with the printed SHA-256).

# Exit on error, undefined vars, pipe failures
set -euo pipefail

# Can pass a commit/tag/ref as the first argument, or fall back to the pin.
NSISMULTIUSER_VERSION="${1:-a33d494c62ad}"

REPO="Drizin/NsisMultiUser"
BASEURL="https://github.com/${REPO}/archive"
AWS_BUCKET="s3://rstudio-buildtools"
FILENAME="NsisMultiUser.zip"
S3_PATH="${AWS_BUCKET}/nsis-multiuser/${NSISMULTIUSER_VERSION}/"

# Contents included at the archive root. These must match the !addincludedir /
# !addplugindir paths in NSIS.template.in.
ARCHIVE_CONTENTS=(Include Plugins License.txt)

# Files that must be present in the upstream archive (fail loudly if the
# upstream layout ever changes).
EXPECTED_FILES=(
   Include/NsisMultiUser.nsh
   Include/NsisMultiUserLang.nsh
   Include/StdUtils.nsh
   Include/UAC.nsh
   Plugins/x86-ansi/UAC.dll
   Plugins/x86-ansi/StdUtils.dll
   Plugins/x86-unicode/UAC.dll
   Plugins/x86-unicode/StdUtils.dll
   License.txt
)

# check if command exists
command_exists() {
   command -v "$1" >/dev/null 2>&1
}

# Resolve a working Python 3 interpreter (used for deterministic archiving;
# portable across macOS/Linux/Windows git-bash, unlike `zip`, which git-bash
# lacks). We verify the interpreter actually runs, to skip the non-functional
# "python3"/"python" App Execution Alias stubs that Windows puts on PATH.
PYTHON=""
for _py in python3 python; do
   if command_exists "${_py}" && "${_py}" -c "import sys; sys.exit(0)" >/dev/null 2>&1; then
      PYTHON="$(command -v "${_py}")"
      break
   fi
done

# Function to cleanup on exit
WORKDIR=""
cleanup() {
   if [[ -n "${WORKDIR}" && -d "${WORKDIR}" ]]; then
      rm -rf "${WORKDIR}"
   fi
   if [[ -f "${FILENAME}" ]]; then
      rm -f "${FILENAME}"
      echo "🧹 Cleaned up temporary file: ${FILENAME}"
   fi
}

# Set up cleanup trap
trap cleanup EXIT

# Check dependencies
for tool in aws wget tar sha256sum; do
   if ! command_exists "${tool}"; then
      echo "Error: '${tool}' is not installed or not in PATH" >&2
      exit 1
   fi
done
if [[ -z "${PYTHON}" ]]; then
   echo "Error: python3 (or python) is not installed or not in PATH" >&2
   exit 1
fi

# Check AWS authentication
echo "🔒 Checking AWS authentication..."
if ! aws sts get-caller-identity >/dev/null 2>&1; then
   echo "AWS authentication required. Attempting to login..."
   if ! aws sso login; then
      echo "Error: Failed to authenticate with AWS" >&2
      exit 1
   fi
fi

WORKDIR="$(mktemp -d)"
TARBALL="${WORKDIR}/nsis-multiuser.tar.gz"

# Download the source at the pinned ref (GitHub archive, no auth required)
echo "⬇️ Downloading ${REPO}@${NSISMULTIUSER_VERSION}..."
if ! wget -O "${TARBALL}" "${BASEURL}/${NSISMULTIUSER_VERSION}.tar.gz"; then
   echo "Error: Failed to download ${REPO}@${NSISMULTIUSER_VERSION}" >&2
   exit 1
fi

# Extract (a GitHub archive contains a single top-level directory)
tar -xzf "${TARBALL}" -C "${WORKDIR}"
SRCDIR="$(find "${WORKDIR}" -mindepth 1 -maxdepth 1 -type d)"
if [[ -z "${SRCDIR}" || ! -d "${SRCDIR}" ]]; then
   echo "Error: could not locate extracted source directory" >&2
   exit 1
fi

# Verify the expected layout before packaging
for path in "${EXPECTED_FILES[@]}"; do
   if [[ ! -e "${SRCDIR}/${path}" ]]; then
      echo "Error: expected file missing from upstream archive: ${path}" >&2
      exit 1
   fi
done

# Build the archive rooted at Include/ and Plugins/ so it extracts directly
# into dependencies/windows/nsis-multiuser/<version>/. Entries are added in a
# fixed (sorted) order with a fixed timestamp, so re-running the script produces
# a byte-identical zip.
echo "📦 Archiving ${FILENAME}..."
ABS_OUT="$(pwd)/${FILENAME}"
rm -f "${ABS_OUT}"
"${PYTHON}" - "${SRCDIR}" "${ABS_OUT}" "${ARCHIVE_CONTENTS[@]}" <<'PY'
import os, sys, zipfile

srcdir, out = sys.argv[1], sys.argv[2]
roots = sys.argv[3:]
fixed_date = (2020, 1, 1, 0, 0, 0)

# Collect member paths (files only; directories are implied on extraction),
# relative to srcdir, in a deterministic sorted order.
members = []
for root in roots:
    path = os.path.join(srcdir, root)
    if os.path.isfile(path):
        members.append(root)
        continue
    for dirpath, dirnames, filenames in os.walk(path):
        for name in filenames:
            rel = os.path.relpath(os.path.join(dirpath, name), srcdir)
            members.append(rel.replace(os.sep, "/"))
members.sort()

with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as zf:
    for member in members:
        info = zipfile.ZipInfo(member, date_time=fixed_date)
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = 0o644 << 16
        with open(os.path.join(srcdir, member), "rb") as fh:
            zf.writestr(info, fh.read())
PY

echo "   SHA-256: $(sha256sum "${FILENAME}" | cut -d' ' -f1)"

# Upload to S3
echo "⬆️ Uploading to S3: ${S3_PATH}"
if ! aws s3 cp "${FILENAME}" "${S3_PATH}" --acl public-read; then
   echo "Error: Failed to upload to S3" >&2
   exit 1
fi

echo "✅ Successfully uploaded NsisMultiUser ${NSISMULTIUSER_VERSION} to S3"
echo "   Fetched by install-dependencies.cmd from:"
echo "   https://rstudio-buildtools.s3.amazonaws.com/nsis-multiuser/${NSISMULTIUSER_VERSION}/${FILENAME}"
