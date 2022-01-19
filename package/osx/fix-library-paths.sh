#!/usr/bin/env bash

#
# fix-library-paths.sh
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

if [ "$#" = "0" ]; then
   echo "Usage: $0 [frameworks directory] [prefix] [files]"
   exit 0
fi

# The FILES argument can be a glob, e.g. '*.dylib', which may not exist
# for local package builds (which might only have build components
# for the current architecture).
shopt -s nullglob

DIR="$1"
PREFIX="$2"
FILES="$3"

cd "$DIR"
for FILE in ${FILES}; do

   install_name_tool -id "${FILE}" "${FILE}"

   LIBPATHS=$( \
      otool -L "${FILE}" | \
      tail -n+2 | \
      cut -d' ' -f1 | \
      sed 's|\t||g' | \
      grep -E 'homebrew|local'
   )

   for LIBPATH in ${LIBPATHS}; do
      OLD="${LIBPATH}"
      NEW="${PREFIX}/$(basename "${OLD}")"
      install_name_tool -change "${OLD}" "${NEW}" "${FILE}"
   done

done
