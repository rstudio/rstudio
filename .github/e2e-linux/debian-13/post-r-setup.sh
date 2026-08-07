#!/usr/bin/env bash
set -euo pipefail

# On arm64 ONLY, point R package installs at the Ubuntu 24 (noble) PPM. Both
# os-e2e-deps and the in-test Playwright harness (fixtures/r-libs-setup.ts)
# derive the PPM binary repo from /etc/os-release's VERSION_CODENAME, which is
# "trixie" in this container -- and PPM serves NO arm64 binaries for trixie yet,
# while noble has both arches. Rewriting the codename to noble steers BOTH
# shared consumers to
# https://packagemanager.posit.co/cran/__linux__/noble/latest so R packages
# arrive as noble arm64 binaries instead of source-compiling every one.
# Runs after rig (which needed the real distro) and before any R package
# install. apt is unaffected -- it keys off /etc/apt/sources.list.
#
# This hook dir is shared by the debian-13-arm64 and debian-13-x86_64 engines,
# hence the arch guard: PPM already serves trixie x86_64 binaries, so on x86_64
# the rewrite would buy nothing and cost something -- it would install noble
# binaries into a trixie container (soname risk) and stop the engine exercising
# native Debian 13 packages, which is much of the point of having it.
# TEMPORARY: remove this hook once PPM publishes trixie arm64 binaries, so the
# arm64 engine also uses native Debian 13 packages.
if [ "$(dpkg --print-architecture)" = arm64 ]; then
  sudo sed -i 's/^VERSION_CODENAME=.*/VERSION_CODENAME=noble/' /etc/os-release
else
  echo "x86_64: keeping the container's own codename (PPM serves trixie x86_64 binaries)."
fi
grep '^VERSION_CODENAME=' /etc/os-release
