#!/usr/bin/env bash
set -euo pipefail

# This is a separate container from the build job, so it needs its own repo
# enablement. RHEL 10 (and thus Rocky 10) dropped the X.Org server, so Xvfb
# is gone; the display comes from Xwayland running under Cage. Cage and
# libXScrnSaver live in EPEL / CRB rather than the base repos, so enable
# both. Xwayland itself ships in Rocky's AppStream (the repo UBI 10 omits).
sudo dnf -y install epel-release
sudo dnf config-manager --set-enabled crb
sudo dnf repolist --enabled | grep -qiE 'crb|codeready' \
  || echo "::warning::CRB repo not enabled; Cage/libXScrnSaver may fail to resolve."

# Rocky 10 (like RHEL 10) has no Xvfb, so the virtual display comes from
# Xwayland running under Cage, a minimal headless Wayland compositor.
# Everything else is the runtime the Electron app and the Playwright-managed
# chromium need. This list is load-bearing, not redundant: the RStudio
# Electron RPM is built with CPACK_RPM_PACKAGE_AUTOREQPROV off (see
# package/linux/CMakeLists.txt) and declares only libxkbcommon-x11 and
# sqlite, so dnf does NOT auto-pull the GUI/Chromium libs on install; they
# must come from here. The last row mirrors the font/text libs some test R
# packages load: freetype/fontconfig (ragg, systemfonts), harfbuzz/fribidi
# (textshaping).
# lsof: the desktop fixture uses it to reclaim an orphaned RStudio on the
# worker's fixed CDP port; the minimal Rocky image doesn't ship it.
sudo dnf -y install \
  xorg-x11-server-Xwayland cage lsof \
  nss nspr atk at-spi2-atk at-spi2-core cups-libs \
  libdrm mesa-libgbm libxkbcommon \
  libX11 libXcomposite libXdamage libXext libXfixes libXrandr libXScrnSaver libXtst libxcb \
  gtk3 pango cairo alsa-lib \
  freetype fontconfig harfbuzz fribidi

# The Playwright harness's R provisioner (fixtures/r-libs-setup.ts) only
# derives a PPM binary repo from an Ubuntu codename; on Rocky there is none,
# so it compiles CRAN packages from source. That needs a compiler stack
# (gcc/g++/gfortran + make) plus the -devel headers the compiled dependencies
# link against. RHEL 10 ships zlib as zlib-ng, hence zlib-ng-compat-devel.
# These -devel packages come from CRB/EPEL, enabled above.
sudo dnf -y install \
  gcc gcc-c++ gcc-gfortran make \
  libcurl-devel openssl-devel libxml2-devel \
  fontconfig-devel freetype-devel harfbuzz-devel fribidi-devel \
  libpng-devel libjpeg-turbo-devel libtiff-devel \
  zlib-ng-compat-devel libicu-devel cairo-devel

# The minimal rockylinux/rockylinux:10 image sets no UTF-8 locale, so R
# starts with a non-UTF-8 charset and prints "Character set is not UTF-8;
# please change your locale" into the console. Console-content tests assert
# on exact console lines, so that warning breaks them deterministically.
# C.UTF-8 is built into glibc; exporting it via GITHUB_ENV carries through
# the setpriv drop into Electron and rsession.
{
  echo "LANG=C.UTF-8"
  echo "LC_ALL=C.UTF-8"
} >> "$GITHUB_ENV"
