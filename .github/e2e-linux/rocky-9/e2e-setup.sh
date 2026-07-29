#!/usr/bin/env bash
set -euo pipefail

# This is a separate container from the build job, so it needs its own repo
# enablement. Unlike RHEL 10, RHEL 9 (and thus Rocky 9) still ships the X.Org
# server, so the display comes from Xvfb (installed below) out of AppStream
# -- no Cage/Xwayland needed. EPEL / CRB are still enabled here for the R
# build toolchain's -devel headers and any runtime lib outside the base repos.
sudo dnf -y install epel-release
sudo dnf config-manager --set-enabled crb
sudo dnf repolist --enabled | grep -qiE 'crb|codeready' \
  || echo "::warning::CRB repo not enabled; some -devel headers may fail to resolve."

# Xvfb (virtual display for the Electron app / Playwright) plus the runtime
# libraries Chromium/Electron and the Playwright-managed chromium need on
# Rocky 9. RHEL 9 keeps the X.Org server, so xorg-x11-server-Xvfb is
# available in AppStream (RHEL 10 dropped it, which is why the v10 engine
# uses Cage/Xwayland instead). This list is load-bearing, not redundant: the
# RStudio Electron RPM is built with CPACK_RPM_PACKAGE_AUTOREQPROV off (see
# package/linux/CMakeLists.txt) and declares only libxkbcommon-x11 and
# sqlite, so dnf does NOT auto-pull the GUI/Chromium libs on install; they
# must come from here. The last row mirrors the font/text libs some test R
# packages load: freetype/fontconfig (ragg, systemfonts), harfbuzz/fribidi
# (textshaping).
# lsof: the desktop fixture uses it to reclaim an orphaned RStudio on the
# worker's fixed CDP port; the minimal Rocky image doesn't ship it.
sudo dnf -y install \
  xorg-x11-server-Xvfb lsof \
  nss nspr atk at-spi2-atk at-spi2-core cups-libs \
  libdrm mesa-libgbm libxkbcommon \
  libX11 libXcomposite libXdamage libXext libXfixes libXrandr libXScrnSaver libXtst libxcb \
  gtk3 pango cairo alsa-lib \
  freetype fontconfig harfbuzz fribidi

# Defensive fallback only. On Rocky 9 x86_64, os-e2e-deps points pak at PPM's
# rhel9 repo, which DOES serve x86_64 binaries, so the harness's R packages
# install as prebuilt binaries (not compiled) -- unlike the arm64/Fedora
# engines where source compilation is the norm. This toolchain is here in
# case a package has no rhel9 binary and pak has to source-compile it.
# NOTE: RHEL 9 still ships classic zlib, so the header is zlib-devel -- NOT
# the zlib-ng-compat-devel the v10 engine uses (RHEL 10 switched to zlib-ng).
# These -devel packages come from CRB/EPEL, enabled above.
sudo dnf -y install \
  gcc gcc-c++ gcc-gfortran make \
  libcurl-devel openssl-devel libxml2-devel \
  fontconfig-devel freetype-devel harfbuzz-devel fribidi-devel \
  libpng-devel libjpeg-turbo-devel libtiff-devel \
  zlib-devel libicu-devel cairo-devel

# The minimal rockylinux/rockylinux:9 image sets no UTF-8 locale, so R starts
# with a non-UTF-8 charset and prints "Character set is not UTF-8; please
# change your locale" into the console. Console-content tests (e.g. the ANSI
# cursor suite) assert on exact console lines, so that warning breaks them
# deterministically. C.UTF-8 is built into glibc (no locale package needed);
# exporting it via GITHUB_ENV carries through the setpriv drop into Electron
# and rsession.
{
  echo "LANG=C.UTF-8"
  echo "LC_ALL=C.UTF-8"
} >> "$GITHUB_ENV"
