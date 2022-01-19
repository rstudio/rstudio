#!/usr/bin/env bash

#
# electron-build.sh
#
# Copyright (C) 2022 by RStudio, PBC
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

set -v

ELECTRON_SRC_DIR="${1-"../../src/node/desktop"}"
ELECTRON_VERSION="${2-"99.9.9"}"
RSTUDIO_INSTALL_DIR="${3-"$(pwd)/install"}"

# copy node sources to a separate build directory
ELECTRON_BUILD_DIR="$(dirname "${ELECTRON_SRC_DIR}")/desktop-build-$(arch)"
rm -rf "${ELECTRON_BUILD_DIR}"
mkdir -p "${ELECTRON_BUILD_DIR}"
rsync -aqzvhP --exclude=node_modules --exclude=out "${ELECTRON_SRC_DIR}/" "${ELECTRON_BUILD_DIR}"

# move to build directory, and rebuild node modules
cd "${ELECTRON_BUILD_DIR}"
rm -rf node_modules
yarn

# build the electron package
./scripts/update-json-version.sh "${ELECTRON_VERSION}"
yarn make

# use rsync to 'merge' Electron build with our own
# TODO: why are the paths so weird?
ELECTRON_BUILD_X86="${ELECTRON_BUILD_DIR}/out/RStudio-darwin-x64/RStudio.app/Contents/resources/app/RStudio.app"
echo "${ELECTRON_BUILD_X86}"
if [ -e "${ELECTRON_BUILD_X86}" ]; then
	rsync -azvhP "${ELECTRON_BUILD_X86}" "${RSTUDIO_INSTALL_DIR}"
fi

ELECTRON_BUILD_ARM="${ELECTRON_BUILD_DIR}/out/RStudio-darwin-arm64/RStudio.app"
echo "${ELECTRON_BUILD_ARM}"
if [ -e "${ELECTRON_BUILD_ARM}" ]; then
	rsync -azvhP "${ELECTRON_BUILD_ARM}" "${RSTUDIO_INSTALL_DIR}"
fi


