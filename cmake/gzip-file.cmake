#
# gzip-file.cmake
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

# Compresses INPUT into OUTPUT as a raw gzip stream. Run this script with the
# working directory set to INPUT's folder and INPUT as a bare file name:
# file(ARCHIVE_CREATE) resolves its input paths relative to the current
# working directory, which breaks when the caller's working directory has
# symlinked components (e.g. /tmp on macOS).
#
# Usage:
#   cmake -DINPUT=<file name> -DOUTPUT=<absolute path> -P gzip-file.cmake

if(NOT INPUT OR NOT OUTPUT)
   message(FATAL_ERROR "gzip-file.cmake requires -DINPUT=<file> and -DOUTPUT=<file>")
endif()

file(ARCHIVE_CREATE
   OUTPUT "${OUTPUT}"
   PATHS "${INPUT}"
   FORMAT raw
   COMPRESSION GZip)

# match the permissions install(DIRECTORY) applies to installed files,
# rather than inheriting the caller's umask
file(CHMOD "${OUTPUT}"
   PERMISSIONS OWNER_READ OWNER_WRITE GROUP_READ WORLD_READ)
