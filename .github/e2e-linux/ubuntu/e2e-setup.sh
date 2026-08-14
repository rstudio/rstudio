#!/usr/bin/env bash
set -euo pipefail

# Shared by the Ubuntu 24 and Ubuntu 26 engines (bare GitHub-hosted runners).

# xvfb + jq: virtual display for the Electron app / Playwright, JSON parsing.
# The RStudio Desktop .deb pulls its own GUI runtime deps (libnss3, libgbm1,
# libgtk-3-0, ...) via apt's dependency resolution on install. Several R
# packages used in tests also have system-library dependencies: libfreetype6 /
# libfontconfig1 (ragg, systemfonts), libharfbuzz0b / libfribidi0
# (textshaping). Install the runtime libs (not the -dev variants) so
# rsession's R can load them without a source compile.
#
# Runtime-only works here because PPM publishes prebuilt binaries for every
# release/architecture this hook serves -- noble and resolute, x86_64 and
# arm64 -- so pak never compiles. Verified 2026-08-05: an ubuntu-26-arm64 run
# installed 45 binary packages from __linux__/resolute/latest with zero source
# compiles. Contrast ubuntu-22/e2e-setup.sh, which does need an arm64-guarded
# toolchain, -dev headers and PKG_INCLUDE_LINKINGTO=TRUE, because PPM publishes
# no jammy arm64 binaries and serves that engine source tarballs instead. If a
# future Ubuntu release lands here without arm64 binary coverage, it needs the
# ubuntu-22 treatment rather than this one.
sudo apt-get update
sudo apt-get install -y xvfb jq \
  libfreetype6 libfontconfig1 libharfbuzz0b libfribidi0

# Ubuntu 24+ restricts unprivileged user namespaces by default, which blocks
# the Electron / Chromium sandbox. Enable them before launching RStudio
# Desktop. (Ubuntu 22 has no such restriction; it uses its own script dir.)
sudo sysctl -w kernel.apparmor_restrict_unprivileged_userns=0
