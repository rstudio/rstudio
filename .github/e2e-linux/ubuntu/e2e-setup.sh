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
sudo apt-get update
sudo apt-get install -y xvfb jq \
  libfreetype6 libfontconfig1 libharfbuzz0b libfribidi0

# Ubuntu 24+ restricts unprivileged user namespaces by default, which blocks
# the Electron / Chromium sandbox. Enable them before launching RStudio
# Desktop. (Ubuntu 22 has no such restriction; it uses its own script dir.)
sudo sysctl -w kernel.apparmor_restrict_unprivileged_userns=0
