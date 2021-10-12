#!/usr/bin/env bash

#
# install-quarto
#
# Copyright (C) 2021 by RStudio, PBC
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

set -e

source "$(dirname "${BASH_SOURCE[0]}")/../tools/rstudio-tools.sh"
section "Installing Quarto"

# variables that control download + installation process
QUARTO_VERSION="0.2.214"
QUARTO_SUBDIR="quarto/${QUARTO_VERSION}"
QUARTO_URL_BASE="https://github.com/quarto-dev/quarto-cli/releases/download/v${QUARTO_VERSION}"

# see if we already have binaries
if [ -d "${RSTUDIO_TOOLS_ROOT}/${QUARTO_SUBDIR}" ]; then
    echo "Quarto ${QUARTO_SUBDIR} already installed"
    exit 0
fi

# move to tools root
sudo-if-necessary-for "${RSTUDIO_TOOLS_ROOT}" "$@"
cd "${RSTUDIO_TOOLS_ROOT}"

# enter quarto subdirectory
mkdir -p "${QUARTO_SUBDIR}"
pushd "${QUARTO_SUBDIR}"

# determine sub-directory based on platform
PLATFORM="$(uname)-$(getconf LONG_BIT)"
case "${PLATFORM}" in

"Darwin-64")
  SUBDIR="macos"
  FILES=(
    "quarto-${QUARTO_VERSION}-macos.tar.gz"
  )
  ;;

"Linux-64")
  SUBDIR="linux"
  FILES=(
    "quarto-${QUARTO_VERSION}-linux-amd64.tar.gz"
  )
  ;;

*)
  echo "Quarto binaries not available for platform '${PLATFORM}'."
  exit 0
  ;;

esac

# download and extract files
for FILE in "${FILES[@]}"; do
  echo "Downloading ${FILE} from ${QUARTO_URL_BASE}/${FILE}"
  download "${QUARTO_URL_BASE}/${FILE}" "${FILE}"
  extract "${FILE}"
  rm -f "${FILE}"
done
