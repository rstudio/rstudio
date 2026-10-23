#
# install-rig.cmake
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

# Shared by the Unix and Windows dependency installers.
cmake_minimum_required(VERSION 3.25)
include("${CMAKE_CURRENT_LIST_DIR}/rig.cmake")

if(NOT IS_ABSOLUTE "${RIG_ROOT}")
   message(FATAL_ERROR "RIG_ROOT must be an absolute dependency installation directory")
endif()

if(RIG_PLATFORM STREQUAL "macos" AND RIG_ARCH STREQUAL "universal")
   set(RIG_ARCHES x86_64 arm64)
elseif(DEFINED RIG_SHA256_${RIG_PLATFORM}_${RIG_ARCH})
   set(RIG_ARCHES "${RIG_ARCH}")
else()
   message(FATAL_ERROR "rig binaries unavailable for ${RIG_PLATFORM}-${RIG_ARCH}")
endif()

set(RIG_EXE "rig")
set(RIG_EXTENSION "tar.gz")
if(RIG_PLATFORM STREQUAL "windows")
   set(RIG_EXE "rig.exe")
   set(RIG_EXTENSION "zip")
endif()

set(RIG_DIR "${RIG_ROOT}/rig/${RSTUDIO_RIG_VERSION}")
set(RIG_STAGING "${RIG_DIR}.staging")

set(RIG_REQUIRED_FILES "bin/${RIG_EXE}" "share" "LICENSE")
if(RIG_PLATFORM STREQUAL "windows")
   list(APPEND RIG_REQUIRED_FILES "bin/rig-shim.exe" "bin/gsudo.exe" "LICENSE-gsudo")
elseif(RIG_PLATFORM STREQUAL "linux")
   list(APPEND RIG_REQUIRED_FILES "share/rig/cacert.pem")
endif()

# The marker is written only after the entire distribution is installed.
# Check the supporting files too, so an interrupted or damaged install retries.
function(rig_installed RESULT)
   set(${RESULT} FALSE PARENT_SCOPE)
   if(RIG_CLEAN OR NOT EXISTS "${RIG_DIR}/.installed")
      return()
   endif()
   foreach(FILE IN LISTS RIG_REQUIRED_FILES)
      if(NOT EXISTS "${RIG_DIR}/${FILE}")
         return()
      endif()
   endforeach()
   file(READ "${RIG_DIR}/.installed" RIG_INSTALLED)
   if(RIG_INSTALLED STREQUAL "${RIG_PLATFORM}-${RIG_ARCH}")
      set(${RESULT} TRUE PARENT_SCOPE)
   endif()
endfunction()

# Check before writing anything, so callers can skip privilege escalation
# (RIG_CHECK_ONLY) when the tools root is not writable and nothing is needed.
rig_installed(RIG_IS_INSTALLED)
if(RIG_IS_INSTALLED)
   message(STATUS "rig ${RSTUDIO_RIG_VERSION} already installed")
   return()
elseif(RIG_CHECK_ONLY)
   message(FATAL_ERROR "rig ${RSTUDIO_RIG_VERSION} is not installed in ${RIG_DIR}")
endif()

file(MAKE_DIRECTORY "${RIG_ROOT}/rig")
file(LOCK "${RIG_ROOT}/rig/install.lock" GUARD PROCESS TIMEOUT 300)

# Another process may have finished installing while this one waited.
rig_installed(RIG_IS_INSTALLED)
if(RIG_IS_INSTALLED)
   message(STATUS "rig ${RSTUDIO_RIG_VERSION} already installed")
   return()
endif()

if(NOT DEFINED RIG_URL_BASE)
   set(RIG_URL_BASE "https://github.com/r-lib/rig/releases/download/v${RSTUDIO_RIG_VERSION}")
endif()

# Keep an existing installation until its replacement has been verified.
file(REMOVE_RECURSE "${RIG_STAGING}")
file(MAKE_DIRECTORY "${RIG_STAGING}")
foreach(ARCH IN LISTS RIG_ARCHES)
   set(ARCHIVE "rig-${RIG_PLATFORM}-${ARCH}-${RSTUDIO_RIG_VERSION}.${RIG_EXTENSION}")
   message(STATUS "Downloading ${ARCHIVE}")
   file(DOWNLOAD "${RIG_URL_BASE}/${ARCHIVE}" "${RIG_STAGING}/${ARCHIVE}"
      EXPECTED_HASH "SHA256=${RIG_SHA256_${RIG_PLATFORM}_${ARCH}}"
      TLS_VERIFY ON STATUS DOWNLOAD_STATUS)
   list(GET DOWNLOAD_STATUS 0 DOWNLOAD_RESULT)
   if(NOT DOWNLOAD_RESULT EQUAL 0)
      message(FATAL_ERROR "rig download failed: ${DOWNLOAD_STATUS}")
   endif()
   file(ARCHIVE_EXTRACT INPUT "${RIG_STAGING}/${ARCHIVE}"
      DESTINATION "${RIG_STAGING}/${ARCH}")
   if(NOT EXISTS "${RIG_STAGING}/${ARCH}/bin/${RIG_EXE}")
      message(FATAL_ERROR "${ARCHIVE} does not contain bin/${RIG_EXE}")
   endif()
endforeach()

list(GET RIG_ARCHES 0 FIRST_ARCH)
set(RIG_CONTENTS "${RIG_STAGING}/${FIRST_ARCH}")
if(RIG_ARCH STREQUAL "universal")
   execute_process(COMMAND lipo -create
      "${RIG_STAGING}/x86_64/bin/rig" "${RIG_STAGING}/arm64/bin/rig"
      -output "${RIG_STAGING}/rig-universal" COMMAND_ERROR_IS_FATAL ANY)
   file(RENAME "${RIG_STAGING}/rig-universal" "${RIG_CONTENTS}/bin/rig")
endif()

configure_file("${CMAKE_CURRENT_LIST_DIR}/rig-LICENSE" "${RIG_CONTENTS}/LICENSE" COPYONLY)
if(RIG_PLATFORM STREQUAL "windows")
   configure_file("${CMAKE_CURRENT_LIST_DIR}/gsudo-LICENSE" "${RIG_CONTENTS}/LICENSE-gsudo" COPYONLY)
endif()
foreach(FILE IN LISTS RIG_REQUIRED_FILES)
   if(NOT EXISTS "${RIG_CONTENTS}/${FILE}")
      message(FATAL_ERROR "rig distribution is missing ${FILE}")
   endif()
endforeach()
file(WRITE "${RIG_CONTENTS}/.installed" "${RIG_PLATFORM}-${RIG_ARCH}")
file(REMOVE_RECURSE "${RIG_DIR}")
file(RENAME "${RIG_CONTENTS}" "${RIG_DIR}")
file(REMOVE_RECURSE "${RIG_STAGING}")
message(STATUS "Installed rig ${RSTUDIO_RIG_VERSION} to ${RIG_DIR}")
