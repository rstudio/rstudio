#!/usr/bin/env bash
set -euo pipefail

# Shared by the Fedora 43 and Fedora 44 engines (fedora:NN containers).

# Xvfb (virtual display for the Electron app / Playwright) plus the runtime
# libraries Chromium/Electron and the Playwright-managed chromium need on
# Fedora. This list is load-bearing, not redundant: the RStudio Electron RPM
# is built with CPACK_RPM_PACKAGE_AUTOREQPROV off (see
# package/linux/CMakeLists.txt) and declares only libxkbcommon-x11 and
# sqlite, so dnf does NOT auto-pull the GUI/Chromium libs on install -- they
# must come from here. The last row mirrors the font/text libs some test R
# packages load: freetype/fontconfig (ragg, systemfonts), harfbuzz/fribidi
# (textshaping).
# lsof: the desktop fixture uses it to reclaim an orphaned RStudio on the
# worker's fixed CDP port (a leftover from a prior interrupted run). The
# minimal Fedora image doesn't ship it; without it the reclaim and port-free
# wait silently degrade.
sudo dnf -y install \
  xorg-x11-server-Xvfb lsof \
  nss nspr atk at-spi2-atk at-spi2-core cups-libs \
  libdrm mesa-libgbm libxkbcommon \
  libX11 libXcomposite libXdamage libXext libXfixes libXrandr libXScrnSaver libXtst libxcb \
  gtk3 pango cairo alsa-lib \
  freetype fontconfig harfbuzz fribidi

# Fedora has no prebuilt CRAN binaries (PPM serves none), so the e2e R
# packages (tidyverse, shiny, rmarkdown, roxygen2, ...) are compiled from
# source by pak. That needs a compiler stack (gcc/g++/gfortran + make) plus
# the -devel headers their compiled dependencies link against:
# libcurl/openssl/libxml2 (curl, openssl, xml2), the font/text stack
# (systemfonts, textshaping, ragg), the image libs (ragg), zlib (data.table
# and others), and ICU (stringi). zlib ships as zlib-ng on Fedora, so the
# headers come from zlib-ng-compat-devel.
sudo dnf -y install \
  gcc gcc-c++ gcc-gfortran make \
  libcurl-devel openssl-devel libxml2-devel \
  fontconfig-devel freetype-devel harfbuzz-devel fribidi-devel \
  libpng-devel libjpeg-turbo-devel libtiff-devel \
  zlib-ng-compat-devel libicu-devel cairo-devel
