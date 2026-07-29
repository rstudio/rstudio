#!/usr/bin/env bash
set -euo pipefail

# Xvfb (virtual display for the Electron app / Playwright) plus the runtime
# libraries Electron/Chromium needs on Debian. This list is load-bearing, not
# redundant: the RStudio Desktop .deb declares only libssl-dev, libclang-dev,
# libsqlite3-0, libxkbcommon-x11-0, and libc6 (see RSTUDIO_DEBIAN_DEPENDS in
# package/linux/CMakeLists.txt), so apt does NOT pull the GUI/Chromium libs
# when it installs the .deb -- they must come from here. The bare-runner
# Ubuntu siblings get them for free from the runner image; a minimal
# container does not. Playwright's later `install --with-deps` overlaps much
# of this set but omits GTK3 / libxss1 / libxtst6, which the Electron app
# itself needs. Package names are trixie's (t64 suffixes from the time64
# transition). The last row mirrors the font/text libs some test R packages
# load: freetype/fontconfig (ragg, systemfonts), harfbuzz/fribidi
# (textshaping).
# lsof: the desktop fixture uses it to reclaim an orphaned RStudio on the
# worker's fixed CDP port; the minimal Debian image doesn't ship it.
sudo apt-get update
sudo apt-get install -y xvfb jq lsof \
  libnss3 libnspr4 libatk1.0-0t64 libatk-bridge2.0-0t64 libatspi2.0-0t64 \
  libcups2t64 libdbus-1-3 libdrm2 libgbm1 libxkbcommon0 \
  libgtk-3-0t64 libpango-1.0-0 libcairo2 libasound2t64 \
  libx11-6 libxcomposite1 libxdamage1 libxext6 libxfixes3 libxrandr2 \
  libxss1 libxtst6 libxcb1 \
  libfreetype6 libfontconfig1 libharfbuzz0b libfribidi0

# The minimal debian:13 image sets no locale, so R starts with a non-UTF-8
# charset and prints "Character set is not UTF-8; please change your locale"
# into the console. Console-content tests (e.g. the ANSI cursor suite) assert
# on exact console lines, so that warning breaks them deterministically.
# C.UTF-8 is built into glibc on Debian 13 (no locale-gen needed); exporting
# it via GITHUB_ENV carries through the setpriv drop into Electron and
# rsession. The bare-runner siblings inherit a UTF-8 locale from the runner
# image and never hit this.
{
  echo "LANG=C.UTF-8"
  echo "LC_ALL=C.UTF-8"
} >> "$GITHUB_ENV"
