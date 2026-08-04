#!/usr/bin/env bash
set -euo pipefail

# Same runtime set as the Ubuntu 24/26 engines; see ubuntu/e2e-setup.sh.
sudo apt-get update
sudo apt-get install -y xvfb jq \
  libfreetype6 libfontconfig1 libharfbuzz0b libfribidi0

# On arm64 ONLY, add the build toolchain and headers, because that engine
# source-compiles every R package: PPM publishes jammy binaries for x86_64 but
# not for arm64, and os-e2e-deps picks the repo from VERSION_CODENAME, so the
# arm64 engine gets __linux__/jammy/latest with source tarballs only. The
# runtime libs above let rsession's R *load* these libraries; compiling
# against them needs the -dev headers too.
#
# Mirrors what the source-compiling engines already install (see
# fedora/, rocky-9/ and rocky-10/ e2e-setup.sh), translated to Debian package
# names, plus libgit2-dev for gert (pulled in by devtools, and not needed on
# the RPM engines that get binaries). Guarded rather than unconditional so the
# x86_64 engine, which installs binaries, does not pay the download.
#
# Unlike debian-13/post-r-setup.sh we do NOT redirect arm64 at the noble PPM:
# jammy is older than noble, so noble binaries would be built against a newer
# glibc than this runner has. Debian's rewrite works because it points forward.
if [ "$(dpkg --print-architecture)" = arm64 ]; then
  sudo apt-get install -y \
    build-essential gfortran \
    libcurl4-openssl-dev libssl-dev libxml2-dev libgit2-dev \
    libfontconfig1-dev libfreetype6-dev libharfbuzz-dev libfribidi-dev \
    libpng-dev libjpeg-dev libtiff-dev \
    zlib1g-dev libicu-dev libcairo2-dev
fi

# No "allow unprivileged user namespaces" sysctl here (unlike Ubuntu 24/26):
# the kernel.apparmor_restrict_unprivileged_userns AppArmor restriction that
# blocks Chromium's sandbox was introduced in Ubuntu 23.10 / 24.04. Jammy
# (22.04) has no such setting and allows unprivileged user namespaces by
# default, so Chromium's sandbox works unmodified -- and running "sysctl -w"
# on the missing key would error.
