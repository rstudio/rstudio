#!/usr/bin/env bash

# upload-r.sh
#
# This script mirrors official R installers into the RStudio Build Tools
# (rstudio-buildtools) S3 bucket, where CI installs R from instead of using
# rig. Presumes you've already got AWS command line tools (awscli) installed,
# and configured with a valid AWS account.
#
# Usage:
#
#    upload-r.sh <version> [platform ...]
#
# With no platforms, every platform below is required. A missing installer is
# an error so bumping RSTUDIO_R_VERSION cannot leave one of the scheduled
# engines without an asset. To mirror an older R that upstream only built for
# some platforms, name that supported subset explicitly.
#
#    upload-r.sh 4.6.1
#    upload-r.sh 4.6.1 macos-arm64 windows-x86_64
#
# Sources are CRAN for Windows and macOS, and Posit's r-builds CDN for Linux
# (the same places rig pulled from). Both lay their files out inconsistently
# -- CRAN moves macOS builds between OS-codename directories across releases,
# retires old Windows installers into base/old/, and eventually moves whole
# major series off to cran-archive.r-project.org (R 3.x lives there now) --
# so this script absorbs that variation and writes ONE flat naming scheme
# into the bucket:
#
#    R/<version>/R-<version>-<platform>.<ext>
#
# That is what lets .github/actions/os-install-r-* name the asset they need
# from the runner's own OS and arch, with no knowledge of upstream layouts
# and no lookup call to resolve a version.

# Exit on error, undefined vars, pipe failures
set -euo pipefail

AWS_BUCKET="s3://rstudio-buildtools"
CRAN="https://cran.r-project.org"
CRAN_ARCHIVE="https://cran-archive.r-project.org"
RBUILDS="https://cdn.posit.co/r"

# Every platform CI installs R on. The key doubles as the token in the
# mirrored filename, so it has to stay in sync with the platform detection in
# .github/actions/os-install-r-unix and os-install-r-windows.
ALL_PLATFORMS=(
   windows-x86_64
   macos-arm64
   macos-x86_64
   ubuntu-2204-amd64
   ubuntu-2204-arm64
   ubuntu-2404-amd64
   ubuntu-2404-arm64
   ubuntu-2604-amd64
   ubuntu-2604-arm64
   debian-13-amd64
   debian-13-arm64
   rhel-9-x86_64
   rhel-9-aarch64
   rhel-10-x86_64
   rhel-10-aarch64
)

# Candidate upstream URLs for one platform, most-likely first. More than one
# because CRAN relocates installers as releases age: Windows installers move
# from base/ into base/old/<version>/, and each macOS build family gets a new
# OS-codename directory every few releases (R >= 4.6 arm64 is under
# sonoma-arm64, 4.1-4.5 under big-sur-arm64). Listing the alternatives beats
# hardcoding version cutoffs that upstream will move again.
r_source_urls() {
   local platform="$1"
   local version="$2"

   case "${platform}" in

      windows-x86_64)
         echo "${CRAN}/bin/windows/base/R-${version}-win.exe"
         echo "${CRAN}/bin/windows/base/old/${version}/R-${version}-win.exe"
         echo "${CRAN_ARCHIVE}/bin/windows/base/old/${version}/R-${version}-win.exe"
         ;;

      macos-arm64)
         echo "${CRAN}/bin/macosx/sonoma-arm64/base/R-${version}-arm64.pkg"
         echo "${CRAN}/bin/macosx/big-sur-arm64/base/R-${version}-arm64.pkg"
         ;;

      macos-x86_64)
         # R <= 4.2 predates the per-arch directories on macOS and carries no
         # arch suffix in the filename. R 3.x is on cran-archive, where the
         # '.nn' build is the notarized one and the one CRAN's index links.
         echo "${CRAN}/bin/macosx/big-sur-x86_64/base/R-${version}-x86_64.pkg"
         echo "${CRAN}/bin/macosx/base/R-${version}.pkg"
         echo "${CRAN_ARCHIVE}/bin/macosx/base/R-${version}.nn.pkg"
         echo "${CRAN_ARCHIVE}/bin/macosx/base/R-${version}.pkg"
         ;;

      # r-builds keys its Debian-family packages by distro only; the
      # architecture lives in the filename.
      ubuntu-*-amd64 | ubuntu-*-arm64 | debian-*-amd64 | debian-*-arm64)
         local arch="${platform##*-}"
         local distro="${platform%-*}"
         echo "${RBUILDS}/${distro}/pkgs/r-${version}_1_${arch}.deb"
         ;;

      rhel-*-x86_64 | rhel-*-aarch64)
         local arch="${platform##*-}"
         local distro="${platform%-*}"
         echo "${RBUILDS}/${distro}/pkgs/R-${version}-1-1.${arch}.rpm"
         ;;

      *)
         echo "Error: unknown platform '${platform}'" >&2
         return 1
         ;;

   esac
}

# The extension the mirrored copy keeps, which is also how the install actions
# decide between installer/apt/dnf.
r_extension() {
   case "$1" in
      windows-*) echo "exe" ;;
      macos-*)   echo "pkg" ;;
      ubuntu-* | debian-*) echo "deb" ;;
      rhel-*)    echo "rpm" ;;
      *)
         echo "Error: unknown platform '$1'" >&2
         return 1
         ;;
   esac
}

# check if command exists
command_exists() {
   command -v "$1" >/dev/null 2>&1
}

usage() {
   echo "Usage: $(basename "$0") <version> [platform ...]" >&2
   echo >&2
   echo "Platforms: ${ALL_PLATFORMS[*]}" >&2
}

R_VERSION="${1:-}"
if [[ -z "${R_VERSION}" ]]; then
   usage
   exit 1
fi
shift

# Validate version format
if [[ ! "${R_VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
   echo "Error: Invalid version format. Expected format: X.Y.Z" >&2
   exit 1
fi

if [[ $# -gt 0 ]]; then
   PLATFORMS=("$@")
else
   PLATFORMS=("${ALL_PLATFORMS[@]}")
fi

for PLATFORM in "${PLATFORMS[@]}"; do
   if [[ ! " ${ALL_PLATFORMS[*]} " == *" ${PLATFORM} "* ]]; then
      echo "Error: unknown platform '${PLATFORM}'" >&2
      usage
      exit 1
   fi
done

# Check dependencies
for TOOL in aws curl; do
   if ! command_exists "${TOOL}"; then
      echo "Error: ${TOOL} is not installed or not in PATH" >&2
      exit 1
   fi
done

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
trap 'rm -rf "${WORKDIR}"' EXIT

UPLOADED=()

for PLATFORM in "${PLATFORMS[@]}"; do

   EXT="$(r_extension "${PLATFORM}")"
   FILENAME="R-${R_VERSION}-${PLATFORM}.${EXT}"
   S3_PATH="${AWS_BUCKET}/R/${R_VERSION}/${FILENAME}"

   echo
   echo "==> ${PLATFORM}"

   # Already mirrored: leave it alone. Installers are immutable upstream, so
   # re-uploading only risks replacing a good copy with a bad download.
   if aws s3 ls "${S3_PATH}" >/dev/null 2>&1; then
      echo "    already present at ${S3_PATH}; skipping"
      UPLOADED+=("${FILENAME} (already present)")
      continue
   fi

   FOUND=""
   while IFS= read -r URL; do
      echo "    trying ${URL}"
      if curl -fsSL --retry 5 --retry-delay 10 -o "${WORKDIR}/${FILENAME}" "${URL}"; then
         FOUND="${URL}"
         break
      fi
   done < <(r_source_urls "${PLATFORM}" "${R_VERSION}")

   if [[ -z "${FOUND}" ]]; then
      echo "Error: no upstream installer for R ${R_VERSION} on ${PLATFORM}" >&2
      exit 1
   fi

   echo "    downloaded from ${FOUND} ($(du -h "${WORKDIR}/${FILENAME}" | cut -f1))"
   aws s3 cp "${WORKDIR}/${FILENAME}" "${S3_PATH}" --acl public-read
   rm -f "${WORKDIR}/${FILENAME}"

   UPLOADED+=("${FILENAME}")

done

echo

echo "✅ R ${R_VERSION}: ${#UPLOADED[@]} mirrored"
for ITEM in "${UPLOADED[@]}"; do
   echo "   ${ITEM}"
done

echo
echo "Mirrored files are served from:"
echo "   https://rstudio-buildtools.s3.amazonaws.com/R/${R_VERSION}/"
