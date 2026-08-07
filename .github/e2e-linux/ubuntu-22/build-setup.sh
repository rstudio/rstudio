#!/usr/bin/env bash
set -euo pipefail

# The ubuntu-22.04 runner image ships pkgconf, which declares
# "Breaks: pkg-config" -- and install-dependencies-jammy asks apt to install
# pkg-config, so apt refuses with "held broken packages" and the whole
# dependency install aborts. Both packages provide /usr/bin/pkg-config. Swap
# pkgconf out for pkg-config in one apt transaction (the trailing "-" on
# pkgconf removes it) so the dependency script's install succeeds. A real
# jammy box without pkgconf preinstalled never hits this, which is why the
# shared script does not handle it.
sudo apt-get update
sudo apt-get install -y pkg-config pkgconf-

# The ubuntu-22.04 runner image presets JAVA_HOME to an older JDK, and the
# GWT ant build targets Java 17 ("invalid target release: 17"). Point
# JAVA_HOME and PATH at the image's Temurin 17, falling back to the
# apt-installed openjdk-17 from install-dependencies-jammy (present by the
# time the build consumes JAVA_HOME). The 24.04/26.04 images already default
# to a new-enough JDK, so the sibling engines don't need this.
#
# Both halves are arch-specific, and this hook dir is shared by the
# ubuntu-22-x86_64 and ubuntu-22-arm64 engines: the runner image names the
# variable JAVA_HOME_17_<ARCH>, and Debian/Ubuntu installs openjdk to
# java-17-openjdk-<dpkg arch>. Picking the x86_64 pair unconditionally would
# export a nonexistent JAVA_HOME on arm64, reintroducing the very build
# failure this block exists to prevent.
case "$(dpkg --print-architecture)" in
  arm64) J17="${JAVA_HOME_17_ARM64:-/usr/lib/jvm/java-17-openjdk-arm64}" ;;
  *)     J17="${JAVA_HOME_17_X64:-/usr/lib/jvm/java-17-openjdk-amd64}" ;;
esac
echo "JAVA_HOME=$J17" >> "$GITHUB_ENV"
echo "$J17/bin" >> "$GITHUB_PATH"
