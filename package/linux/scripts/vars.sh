#!/usr/bin/env bash

#
# vars.sh -- shared code to define environment variables for Linux builds
#
# Copyright (C) 2025 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

if test -z "$BUILD_DIR"
then
   # set build type (if necessary) and build dir
   if test -z "$CMAKE_BUILD_TYPE"
   then
      CMAKE_BUILD_TYPE=RelWithDebInfo
      BUILD_DIR=build-$RSTUDIO_TARGET-$PACKAGE_TARGET
   else
      BUILD_DIR=build-$RSTUDIO_TARGET-$PACKAGE_TARGET-$CMAKE_BUILD_TYPE
   fi
fi

# make build directory absolute
BUILD_DIR=$(readlink -f "$BUILD_DIR")

# build RStudio version suffix
RSTUDIO_VERSION_ARRAY=(
   "${RSTUDIO_VERSION_MAJOR-99}"
   "${RSTUDIO_VERSION_MINOR-9}"
   "${RSTUDIO_VERSION_PATCH-9}"
)

RSTUDIO_VERSION_FULL=$(IFS="."; echo "${RSTUDIO_VERSION_ARRAY[*]}")"${RSTUDIO_VERSION_SUFFIX}"

