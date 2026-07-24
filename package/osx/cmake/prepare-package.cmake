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

cmake_minimum_required(VERSION 3.6.3)

# CMake's message is suppressed during install stage so just use echo here
function(echo MESSAGE)
   execute_process(COMMAND echo "-- ${MESSAGE}")
endfunction()

set(RSESSION_BINARY_DIR "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Resources/app/bin")
set(X64_FRAMEWORKS_DIRECTORY "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Resources/app/Frameworks")
set(ARM64_FRAMEWORKS_DIRECTORY "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Resources/app/Frameworks/arm64")
set(FIX_LIBRARY_PATHS_SCRIPT_PATH "@CMAKE_CURRENT_SOURCE_DIR@/scripts/fix-library-paths.sh")

# NOTE: This part of CMake will be run by the x86 branch of the build,
# so we don't want to filter based on the architecture here.
if(EXISTS "@RSESSION_ARM64_PATH@")

   echo("Found arm64 rsession binary: '@RSESSION_ARM64_PATH@'")

   # find out where arm64 homebrew lives
   set(HOMEBREW_ARM64_PREFIX "/opt/homebrew")
   echo("Homebrew prefix: '${HOMEBREW_ARM64_PREFIX}'")

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

   # copy required Homebrew libraries
   list(APPEND HOMEBREW_LIBS gettext openssl sqlite3)
   if(@RSTUDIO_PRO_BUILD@)
      list(APPEND HOMEBREW_LIBS krb5 libpq)
   endif()

   file(MAKE_DIRECTORY "${ARM64_FRAMEWORKS_DIRECTORY}")
   foreach(LIB ${HOMEBREW_LIBS})
      set(LIBPATH "${HOMEBREW_ARM64_PREFIX}/opt/${LIB}/lib")
      file(GLOB LIBFILES "${LIBPATH}/*.dylib")
      foreach(LIBFILE ${LIBFILES})
         file(
            COPY "${LIBFILE}"
            DESTINATION "${ARM64_FRAMEWORKS_DIRECTORY}")
      endforeach()
   endforeach()

   # fix library paths on arm64 components
   execute_process(
      COMMAND
         "${FIX_LIBRARY_PATHS_SCRIPT_PATH}"
         "${ARM64_FRAMEWORKS_DIRECTORY}"
         "@executable_path/../Frameworks/arm64"
         "*.dylib")

   execute_process(
      COMMAND
         "${FIX_LIBRARY_PATHS_SCRIPT_PATH}"
         "${RSESSION_BINARY_DIR}"
         "@executable_path/../Frameworks/arm64"
         "rsession-arm64")

else()

   echo("No arm64 rsession binary available at '@RSESSION_ARM64_PATH@'")

endif()

# find out where homebrew lives for the primary architecture
# UNAME_M reflects the primary build arch: x86_64 when configured under
# arch -x86_64 (universal or x64-only), arm64 when configured under
# arch -arm64 (arm64-only)
if("@UNAME_M@" STREQUAL "arm64")
   set(HOMEBREW_PRIMARY_PREFIX "/opt/homebrew")
else()
   set(HOMEBREW_PRIMARY_PREFIX "/usr/local")
endif()

# copy required Homebrew libraries for the primary architecture
list(APPEND HOMEBREW_LIBS gettext openssl sqlite3)
if(@RSTUDIO_PRO_BUILD@)
   list(APPEND HOMEBREW_LIBS krb5 libpq)
endif()

file(MAKE_DIRECTORY "${X64_FRAMEWORKS_DIRECTORY}")
foreach(LIB ${HOMEBREW_LIBS})
   set(LIBPATH "${HOMEBREW_PRIMARY_PREFIX}/opt/${LIB}/lib")
   file(GLOB LIBFILES "${LIBPATH}/*.dylib")
   foreach(LIBFILE ${LIBFILES})
      file(
         COPY "${LIBFILE}"
         DESTINATION "${X64_FRAMEWORKS_DIRECTORY}")
   endforeach()
endforeach()


# fix library paths on x86_64 components
execute_process(
   COMMAND
      "${FIX_LIBRARY_PATHS_SCRIPT_PATH}"
      "${X64_FRAMEWORKS_DIRECTORY}"
      "@executable_path/../Frameworks"
      "*.dylib")

execute_process(
   COMMAND
      "${FIX_LIBRARY_PATHS_SCRIPT_PATH}"
      "${RSESSION_BINARY_DIR}"
      "@executable_path/../Frameworks"
      "diagnostics rpostback rsession")

# Combine the x86_64 and arm64 builds of diagnostics and rpostback into
# universal binaries. This runs only for universal builds: the primary build is
# x86_64 (so the binaries already installed under bin/ are the x86_64 slices)
# and a separate arm64 build has been produced. lipo cannot merge two inputs of
# the same architecture, so single-architecture builds must not reach here --
# hence gating on RSTUDIO_UNIVERSAL_BUILD, not on file existence alone.
if("@RSTUDIO_UNIVERSAL_BUILD@" STREQUAL "1")

   # stage arm64 slices outside CMAKE_INSTALL_PREFIX so a leftover can never be
   # packaged into the DMG or signed
   set(LIPO_STAGING_DIR "@CMAKE_CURRENT_BINARY_DIR@/arm64-lipo-staging")
   file(MAKE_DIRECTORY "${LIPO_STAGING_DIR}")

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

      # stage the arm64 build and point it at the arm64 Frameworks directory
      file(COPY "${ARM64_SOURCE}" DESTINATION "${LIPO_STAGING_DIR}")
      execute_process(
         COMMAND
            "${FIX_LIBRARY_PATHS_SCRIPT_PATH}"
            "${LIPO_STAGING_DIR}"
            "@executable_path/../Frameworks/arm64"
            "${TOOL}"
         RESULT_VARIABLE FIX_PATHS_RESULT)

      if(NOT FIX_PATHS_RESULT EQUAL 0)
         message(FATAL_ERROR "Failed to fix arm64 library paths for '${TOOL}' (exit ${FIX_PATHS_RESULT})")
      endif()

      # verify the rewrite took: no absolute Homebrew/local dylib paths may
      # remain, or the shipped arm64 slice would fail to load its bundled
      # libraries on an end-user machine. lipo succeeding does not catch this.
      execute_process(
         COMMAND otool -L "${LIPO_STAGING_DIR}/${TOOL}"
         OUTPUT_VARIABLE FIXED_TOOL_LIBS)

      if(FIXED_TOOL_LIBS MATCHES "/opt/homebrew|/usr/local")
         message(FATAL_ERROR "arm64 '${TOOL}' still references absolute Homebrew paths after fixing library paths:\n${FIXED_TOOL_LIBS}")
      endif()

      # fuse the (already path-fixed) x86_64 slice with the arm64 slice
      execute_process(
         COMMAND
            lipo -create
               "${X64_BINARY}"
               "${LIPO_STAGING_DIR}/${TOOL}"
            -output "${X64_BINARY}.universal"
         RESULT_VARIABLE LIPO_RESULT)

      if(NOT LIPO_RESULT EQUAL 0)
         message(FATAL_ERROR "lipo failed for '${TOOL}' (exit ${LIPO_RESULT})")
      endif()

      file(RENAME "${X64_BINARY}.universal" "${X64_BINARY}")

   endforeach()

   # remove staging artifacts so they are not packaged or signed
   file(REMOVE_RECURSE "${LIPO_STAGING_DIR}")

endif()
