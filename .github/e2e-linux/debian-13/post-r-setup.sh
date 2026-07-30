#!/usr/bin/env bash
set -euo pipefail

# Point R package installs at the Ubuntu 24 (noble) PPM, the only codename
# with arm64 binaries. Both os-e2e-deps and the in-test Playwright harness
# (fixtures/r-libs-setup.ts) derive the PPM binary repo from /etc/os-release's
# VERSION_CODENAME, which is "trixie" in this container -- but PPM serves NO
# arm64 binaries for trixie yet (x86_64 only), while noble has both.
# Rewriting the codename to noble steers BOTH shared consumers to
# https://packagemanager.posit.co/cran/__linux__/noble/latest so R packages
# arrive as noble arm64 binaries instead of source-compiling every one.
# Runs after rig (which needed the real distro) and before any R package
# install. apt is unaffected -- it keys off /etc/apt/sources.list.
# TEMPORARY: remove this hook once PPM publishes trixie arm64 binaries, so
# the tests use native Debian 13 packages (PPM already serves trixie x86_64
# binaries; arm64 is the only gap).
sudo sed -i 's/^VERSION_CODENAME=.*/VERSION_CODENAME=noble/' /etc/os-release
grep '^VERSION_CODENAME=' /etc/os-release
