#
# prepare-package.cmake
#
# Copyright (C) 2022 by Posit Software, PBC
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

cmake_minimum_required(VERSION 3.25)

# CMake's message is suppressed during install stage so just use echo here
function(echo MESSAGE)
   execute_process(COMMAND echo "-- ${MESSAGE}")
endfunction()

set(RSESSION_BINARY_DIR "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Resources/app/bin")
set(VERIFY_LIBRARY_PATHS_SCRIPT_PATH "@CMAKE_CURRENT_SOURCE_DIR@/scripts/verify-library-paths.sh")

# NOTE: This part of CMake will be run by the x86 branch of the build,
# so we don't want to filter based on the architecture here.
if(EXISTS "@RSESSION_ARM64_PATH@")

   echo("Found arm64 rsession binary: '@RSESSION_ARM64_PATH@'")

   # copy arm64 rsession binary
   configure_file(
      "@RSESSION_ARM64_PATH@"
      "${RSESSION_BINARY_DIR}/rsession-arm64"
      COPYONLY)

   # copy arm64 node installation
   set(NODE_ARM64_SOURCE "@CMAKE_CURRENT_SOURCE_DIR@/../../dependencies/common/node/@RSTUDIO_INSTALLED_NODE_VERSION@-arm64-installed")
   if(EXISTS "${NODE_ARM64_SOURCE}")
      echo("Installing arm64 node from '${NODE_ARM64_SOURCE}'")
      file(
         COPY "${NODE_ARM64_SOURCE}/"
         DESTINATION "${RSESSION_BINARY_DIR}/node-arm64"
         USE_SOURCE_PERMISSIONS)
   else()
      echo("Warning: arm64 node not found at '${NODE_ARM64_SOURCE}'")
   endif()

   if(EXISTS "@LICENSEMANAGER_ARM64_PATH@")
      echo("Found arm64 license-manager binary: '@LICENSEMANAGER_ARM64_PATH@'")

      # copy arm64 license-manager binary
      configure_file(
         "@LICENSEMANAGER_ARM64_PATH@"
         "${RSESSION_BINARY_DIR}/license-manager-arm64"
         COPYONLY)
   endif()

else()

   echo("No arm64 rsession binary available at '@RSESSION_ARM64_PATH@'")

endif()

# Combine the x86_64 and arm64 builds of diagnostics and rpostback into
# universal binaries. This runs only for universal builds: the primary build is
# x86_64 (so the binaries already installed under bin/ are the x86_64 slices)
# and a separate arm64 build has been produced. lipo cannot merge two inputs of
# the same architecture, so single-architecture builds must not reach here --
# hence gating on RSTUDIO_UNIVERSAL_BUILD, not on file existence alone.
if("@RSTUDIO_UNIVERSAL_BUILD@" STREQUAL "1")

   foreach(TOOL diagnostics rpostback)

      if("${TOOL}" STREQUAL "diagnostics")
         set(ARM64_SOURCE "@DIAGNOSTICS_ARM64_PATH@")
      else()
         set(ARM64_SOURCE "@RPOSTBACK_ARM64_PATH@")
      endif()

      set(X64_BINARY "${RSESSION_BINARY_DIR}/${TOOL}")

      # A universal build promises both slices. A missing input means the
      # x86_64 or arm64 build did not produce the tool; fail fast rather than
      # silently ship a thin binary that would still require Rosetta.
      if(NOT EXISTS "${X64_BINARY}")
         message(FATAL_ERROR "Universal build: missing x86_64 '${TOOL}' at '${X64_BINARY}'")
      endif()

      if(NOT EXISTS "${ARM64_SOURCE}")
         message(FATAL_ERROR "Universal build: missing arm64 '${TOOL}' at '${ARM64_SOURCE}'")
      endif()

      echo("Creating universal '${TOOL}' binary")

      execute_process(
         COMMAND
            lipo -create
               "${X64_BINARY}"
               "${ARM64_SOURCE}"
            -output "${X64_BINARY}.universal"
         RESULT_VARIABLE LIPO_RESULT)

      if(NOT LIPO_RESULT EQUAL 0)
         message(FATAL_ERROR "lipo failed for '${TOOL}' (exit ${LIPO_RESULT})")
      endif()

      file(RENAME "${X64_BINARY}.universal" "${X64_BINARY}")

   endforeach()

endif()

# fail the package build, rather than the app at launch, if a binary loads a
# library that isn't part of macOS
execute_process(
   COMMAND "${VERIFY_LIBRARY_PATHS_SCRIPT_PATH}" "${RSESSION_BINARY_DIR}"
   RESULT_VARIABLE VERIFY_PATHS_RESULT)

if(NOT VERIFY_PATHS_RESULT EQUAL 0)
   message(FATAL_ERROR "Library check failed (exit ${VERIFY_PATHS_RESULT}); see errors above")
endif()
