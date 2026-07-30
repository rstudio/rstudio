#!/usr/bin/env bash
set -euo pipefail

# Several build-time -devel packages (boost-devel, pango-devel, ...) live in
# EPEL / CodeReady Builder (CRB) rather than the base repos. On Rocky both
# are trivially available: epel-release ships in the extras repo, and the CRB
# repo is named "crb". Enabling them up front lets install-dependencies-yum
# resolve everything.
sudo dnf -y install dnf-plugins-core
sudo dnf -y install epel-release
sudo dnf config-manager --set-enabled crb
sudo dnf repolist --enabled | grep -qiE 'crb|codeready' \
  || echo "::warning::CRB repo not enabled; build -devel packages may fail to resolve."
