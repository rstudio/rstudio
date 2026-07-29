#!/usr/bin/env bash
set -euo pipefail

# Same runtime set as the Ubuntu 24/26 engines; see ubuntu/e2e-setup.sh.
sudo apt-get update
sudo apt-get install -y xvfb jq \
  libfreetype6 libfontconfig1 libharfbuzz0b libfribidi0

# No "allow unprivileged user namespaces" sysctl here (unlike Ubuntu 24/26):
# the kernel.apparmor_restrict_unprivileged_userns AppArmor restriction that
# blocks Chromium's sandbox was introduced in Ubuntu 23.10 / 24.04. Jammy
# (22.04) has no such setting and allows unprivileged user namespaces by
# default, so Chromium's sandbox works unmodified -- and running "sysctl -w"
# on the missing key would error.
