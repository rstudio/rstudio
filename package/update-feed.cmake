#
# update-feed.cmake
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
# Writes the releases.json consumed by RStudio Desktop's in-app updater
# (src/node/desktop/src/main/updater.ts) for one packaged artifact. CMake is
# the one scripting runtime every package build already has, which is why
# this is not a shell or PowerShell script.
#
# Usage:
#
#   cmake -DPACKAGE_FILE=<zip or exe> -DVERSION=<rstudio version>
#         -DURL=<public download url> -DPLATFORM=<macos|windows>
#         [-DARCH=<universal|x86_64|arm64>] [-DCOMMIT=<sha>] [-DNOTES=<url>]
#         -DOUTPUT_DIR=<dir> -P package/update-feed.cmake
#
# The feed lands at <OUTPUT_DIR>/<channel>/<os>/<arch>/releases.json, the
# same relative path the desktop requests under its feed base URL, and that
# relative path is written to <OUTPUT_DIR>/feed-path for the caller to upload
# from. The channel comes from the version's pre-release tag exactly as the
# desktop derives it: none is "release", otherwise the tag (daily, hourly,
# preview). A "dev" version is refused; developer builds never update.
#
# The JSON is the Squirrel.Mac "default" server format (url, name, notes,
# pub_date), which Electron's autoUpdater reads directly on macOS, plus
# sha256 and size, which the desktop verifies itself on Windows.
#

cmake_minimum_required(VERSION 3.25)

foreach(required PACKAGE_FILE VERSION URL PLATFORM OUTPUT_DIR)
   if(NOT DEFINED ${required} OR "${${required}}" STREQUAL "")
      message(FATAL_ERROR "update-feed.cmake: -D${required}=... is required")
   endif()
endforeach()

if(NOT EXISTS "${PACKAGE_FILE}")
   message(FATAL_ERROR "update-feed.cmake: package file not found: ${PACKAGE_FILE}")
endif()

if(NOT URL MATCHES "^https://")
   message(FATAL_ERROR "update-feed.cmake: the download URL must use https: ${URL}")
endif()

# channel from the version's pre-release tag
if(VERSION MATCHES "^[0-9]+\\.[0-9]+\\.[0-9]+-([A-Za-z]+)\\+")
   string(TOLOWER "${CMAKE_MATCH_1}" CHANNEL)
elseif(VERSION MATCHES "^[0-9]+\\.[0-9]+\\.[0-9]+\\+")
   set(CHANNEL "release")
else()
   message(FATAL_ERROR "update-feed.cmake: unrecognized version: ${VERSION}")
endif()

if(NOT CHANNEL MATCHES "^(release|preview|daily|hourly)$")
   message(FATAL_ERROR "update-feed.cmake: builds on the '${CHANNEL}' channel do not auto-update")
endif()

# feed location, matching update-feed.ts
if(PLATFORM STREQUAL "macos")
   set(FEED_OS "macos")
   if(NOT DEFINED ARCH OR "${ARCH}" STREQUAL "")
      set(ARCH "universal")
   endif()
elseif(PLATFORM STREQUAL "windows")
   set(FEED_OS "windows")
   if(NOT DEFINED ARCH OR "${ARCH}" STREQUAL "")
      set(ARCH "x86_64")
   endif()
else()
   message(FATAL_ERROR "update-feed.cmake: unsupported platform '${PLATFORM}' (macos or windows)")
endif()

if(NOT DEFINED NOTES OR "${NOTES}" STREQUAL "")
   set(NOTES "https://docs.posit.co/ide/news/")
endif()
if(NOT DEFINED COMMIT)
   set(COMMIT "")
endif()

file(SHA256 "${PACKAGE_FILE}" SHA256)
file(SIZE "${PACKAGE_FILE}" SIZE)
string(TIMESTAMP PUB_DATE "%Y-%m-%dT%H:%M:%SZ" UTC)

set(FEED_PATH "${CHANNEL}/${FEED_OS}/${ARCH}/releases.json")
set(FEED_FILE "${OUTPUT_DIR}/${FEED_PATH}")

# every value here is produced by the build, not user input, so plain
# interpolation is safe; keep the layout stable for diffing
file(WRITE "${FEED_FILE}" "{
  \"version\": \"${VERSION}\",
  \"name\": \"RStudio ${VERSION}\",
  \"notes\": \"${NOTES}\",
  \"pub_date\": \"${PUB_DATE}\",
  \"url\": \"${URL}\",
  \"sha256\": \"${SHA256}\",
  \"size\": ${SIZE},
  \"commit\": \"${COMMIT}\"
}
")
file(WRITE "${OUTPUT_DIR}/feed-path" "${FEED_PATH}\n")

message(STATUS "Wrote update feed for RStudio ${VERSION} (${CHANNEL}) to ${FEED_FILE}")
