#!/usr/bin/env bash
#
# Kick off a cross-platform RStudio E2E certification run.
#
# Dispatches the cert-e2e-rstudio.yml GitHub Actions workflow against
# rstudio/rstudio, running the full Playwright suite on macOS, Windows, and
# Linux Server against the three pre-built installers you provide.
#
# Requires the gh CLI (https://cli.github.com), authenticated against
# rstudio/rstudio (gh auth status).
#
# Usage:
#   scripts/run-cert-e2e.sh \
#     --branch <release-branch> \
#     --mac   <macOS .dmg URL> \
#     --win   <Windows .exe URL> \
#     --linux <Linux .deb URL> \
#     [--r-version <R version>] \
#     [--grep-invert <pattern>]
#
# Example:
#   scripts/run-cert-e2e.sh \
#     --branch rel-blue-plumbago \
#     --mac   https://dailies.rstudio.com/.../RStudio-2026.06.0.dmg \
#     --win   https://dailies.rstudio.com/.../RStudio-2026.06.0.exe \
#     --linux https://dailies.rstudio.com/.../rstudio-server-2026.06.0-amd64.deb
#
# The --branch arg becomes the workflow's --ref, which controls BOTH which
# version of cert-e2e-rstudio.yml runs AND which e2e/ test cut is checked out
# on each runner. Use the release branch you want to certify.
#
# The workflow itself HEAD-checks each URL before launching the per-platform
# jobs, so a 404'd build fails fast without burning runner minutes.

set -euo pipefail

REPO="rstudio/rstudio"
WORKFLOW="cert-e2e-rstudio.yml"

usage() {
  sed -n '3,33p' "$0" | sed 's/^# \{0,1\}//'
  exit 1
}

branch=""
mac=""
win=""
linux=""
r_version=""
grep_invert=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --branch)      branch="$2";      shift 2 ;;
    --mac)         mac="$2";         shift 2 ;;
    --win)         win="$2";         shift 2 ;;
    --linux)       linux="$2";       shift 2 ;;
    --r-version)   r_version="$2";   shift 2 ;;
    --grep-invert) grep_invert="$2"; shift 2 ;;
    -h|--help)     usage ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      ;;
  esac
done

missing=()
[[ -z "$branch" ]] && missing+=(--branch)
[[ -z "$mac"    ]] && missing+=(--mac)
[[ -z "$win"    ]] && missing+=(--win)
[[ -z "$linux"  ]] && missing+=(--linux)
if [[ ${#missing[@]} -gt 0 ]]; then
  echo "Missing required argument(s): ${missing[*]}" >&2
  echo
  usage
fi

# Local extension sanity check -- catches the most common paste error
# (wrong URL in the wrong slot) before involving GitHub at all.
[[ "$mac"   == *.dmg ]] || { echo "macOS URL must end in .dmg: $mac" >&2;   exit 2; }
[[ "$win"   == *.exe ]] || { echo "Windows URL must end in .exe: $win" >&2; exit 2; }
[[ "$linux" == *.deb ]] || { echo "Linux URL must end in .deb: $linux" >&2; exit 2; }

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI not found on PATH. Install https://cli.github.com and run 'gh auth login'." >&2
  exit 3
fi

echo "Dispatching cert run on $REPO:"
echo "  Branch:    $branch"
echo "  macOS:     $mac"
echo "  Windows:   $win"
echo "  Linux:     $linux"
[[ -n "$r_version"   ]] && echo "  R version: $r_version"
[[ -n "$grep_invert" ]] && echo "  grep_invert: $grep_invert"
echo

args=(
  --repo "$REPO"
  --ref "$branch"
  -f "mac_installer_url=$mac"
  -f "win_installer_url=$win"
  -f "linux_installer_url=$linux"
)
[[ -n "$r_version"   ]] && args+=(-f "r_version=$r_version")
[[ -n "$grep_invert" ]] && args+=(-f "grep_invert=$grep_invert")

gh workflow run "$WORKFLOW" "${args[@]}"

cat <<MSG

Cert run dispatched. View progress at:
  https://github.com/$REPO/actions/workflows/$WORKFLOW

(Use 'gh run watch' on the latest run for live status in the terminal.)
MSG
