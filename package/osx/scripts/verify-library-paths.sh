#!/usr/bin/env bash

#
# verify-library-paths.sh
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
#

# fix-library-paths.sh points Homebrew references at Frameworks/ without
# checking that the library was bundled there, and binaries it doesn't process
# (e.g. license-manager) keep absolute Homebrew paths. Either would only fail
# at launch on a user's machine, so check for both here.

set -euo pipefail

if [ "$#" != "1" ]; then
   echo "Usage: $0 [bin directory]" >&2
   exit 1
fi

BIN_DIR="$1"
if [ ! -d "${BIN_DIR}" ]; then
   echo "error: bin directory '${BIN_DIR}' does not exist" >&2
   exit 1
fi

FRAMEWORKS_PREFIX="@executable_path/../Frameworks/"

shopt -s nullglob

FAILED=0
for FILE in "${BIN_DIR}"/* "${BIN_DIR}"/../Frameworks/*.dylib "${BIN_DIR}"/../Frameworks/arm64/*.dylib; do

   [ -f "${FILE}" ] || continue

   # dependency lines are tab-indented; otool prints none for non-Mach-O files
   DEPS=$(otool -arch all -L "${FILE}" | { grep $'^\t' || true; } | cut -d' ' -f1 | tr -d '\t' | sort -u)

   for DEP in ${DEPS}; do
      case "${DEP}" in
         "${FRAMEWORKS_PREFIX}"*)
            # the executables that load bundled libraries all live in bin/
            if [ ! -e "${BIN_DIR}/${DEP#@executable_path/}" ]; then
               echo "error: '${FILE}' loads '${DEP}', which is not in the app bundle" >&2
               FAILED=1
            fi
            ;;
         /opt/homebrew/*|/usr/local/*)
            echo "error: '${FILE}' loads '${DEP}' from the build machine's Homebrew" >&2
            FAILED=1
            ;;
      esac
   done

done

if [ "${FAILED}" != "0" ]; then
   echo "Bundle these libraries (HOMEBREW_LIBS in prepare-package.cmake) and point the binaries at" \
      "Frameworks/, or stop linking them." >&2
   exit 1
fi
