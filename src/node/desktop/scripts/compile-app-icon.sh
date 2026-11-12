#!/usr/bin/env bash

#
# compile-app-icon.sh
#
# Copyright (C) 2026 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#

# Compiles resources/icons/macos-app-icon/RStudio.icon (an Icon Composer
# document) into the Assets.car checked in beside it. Re-run this, and commit
# the result, whenever the .icon changes. Requires Xcode 26 or newer.
#
# The compiled catalog is checked in, rather than built with the app, because
# actool's output differs from run to run: the x86_64 and arm64 slices of a
# universal build would each get a different Assets.car, which the universal
# merge rejects. It also keeps Xcode off the list of build requirements.

set -euo pipefail

ICON_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../resources/icons/macos-app-icon" && pwd)"

# xcode-select often points at the Command Line Tools, which have no actool.
if ! xcrun --find actool > /dev/null 2>&1
then
   export DEVELOPER_DIR="/Applications/Xcode.app/Contents/Developer"
fi

# Older actool versions reject a .icon with an error that doesn't mention the
# version, so check it up front.
ACTOOL_VERSION="$(xcrun actool --version | plutil -extract 'com\.apple\.actool\.version.short-bundle-version' raw -o - - 2> /dev/null || true)"
ACTOOL_MAJOR="${ACTOOL_VERSION%%.*}"
if ! [[ "${ACTOOL_MAJOR}" =~ ^[0-9]+$ ]] || (( ACTOOL_MAJOR < 26 ))
then
   echo "error: compiling RStudio.icon requires actool from Xcode 26 or newer (found: ${ACTOOL_VERSION:-none})" >&2
   exit 1
fi

OUTPUT_DIR="$(mktemp -d -t compile-app-icon)"
trap 'rm -rf "${OUTPUT_DIR}"' EXIT

# --app-icon names the icon inside the catalog; CFBundleIconName in
# Info.plist.in must match it.
xcrun actool "${ICON_DIR}/RStudio.icon" \
   --compile "${OUTPUT_DIR}" \
   --output-format human-readable-text \
   --notices \
   --warnings \
   --errors \
   --output-partial-info-plist "${OUTPUT_DIR}/partial-info.plist" \
   --app-icon RStudio \
   --include-all-app-icons \
   --enable-on-demand-resources NO \
   --development-region en \
   --target-device mac \
   --minimum-deployment-target 13.0 \
   --platform macosx

cp "${OUTPUT_DIR}/Assets.car" "${ICON_DIR}/Assets.car"
echo "Wrote ${ICON_DIR}/Assets.car"
