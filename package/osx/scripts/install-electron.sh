#!/usr/bin/env bash

#
# install-electron.sh
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

# use rsync to 'merge' Electron build with our own
ELECTRON_BUILD_X86="../../../src/node/desktop-build-x86_64/out/RStudio-darwin-x64/RStudio.app"
ELECTRON_BUILD_ARM="../../../src/node/desktop-build-x86_64/out/RStudio-darwin-arm64/RStudio.app"
echo "${ELECTRON_BUILD_X86}"
if [ -e "${ELECTRON_BUILD_X86}" ]; then
	rsync -azvhP "${ELECTRON_BUILD_X86}" "${RSTUDIO_INSTALL_DIR}"
fi

ELECTRON_BUILD_ARM="${ELECTRON_BUILD_DIR}/out/RStudio-darwin-arm64/RStudio.app"
echo "${ELECTRON_BUILD_ARM}"
if [ -e "${ELECTRON_BUILD_ARM}" ]; then
	rsync -azvhP "${ELECTRON_BUILD_ARM}" "${RSTUDIO_INSTALL_DIR}"
fi


