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

# A binary in bin/ that loads a library outside macOS and the app's own
# Frameworks/ (e.g. from the build machine's Homebrew), or one missing from
# Frameworks/, would only fail at launch on a user's machine; check for that here.

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
for FILE in "${BIN_DIR}"/*; do

   [ -f "${FILE}" ] || continue

   # dependency lines are tab-indented; otool prints none for non-Mach-O files
   DEPS=$(otool -arch all -L "${FILE}" | { grep $'^\t' || true; } | cut -d' ' -f1 | tr -d '\t' | sort -u)

   for DEP in ${DEPS}; do
      case "${DEP}" in
         /usr/lib/*|/System/Library/*)
            ;;
         "${FRAMEWORKS_PREFIX}"*)
            if [ ! -e "${BIN_DIR}/${DEP#@executable_path/}" ]; then
               echo "error: '${FILE}' loads '${DEP}', which is not in the app bundle" >&2
               FAILED=1
            fi
            ;;
         *)
            echo "error: '${FILE}' loads '${DEP}', which is not part of macOS" >&2
            FAILED=1
            ;;
      esac
   done

done

if [ "${FAILED}" != "0" ]; then
   echo "Link these libraries statically, bundle them in Frameworks/, or stop linking them." >&2
   exit 1
fi
