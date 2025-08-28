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

# find out where x64 homebrew lives
set(HOMEBREW_X64_PREFIX "/usr/local")

# copy required Homebrew libraries
list(APPEND HOMEBREW_LIBS gettext openssl sqlite3)
if(@RSTUDIO_PRO_BUILD@)
   list(APPEND HOMEBREW_LIBS krb5 libpq)
endif()

file(MAKE_DIRECTORY "${X64_FRAMEWORKS_DIRECTORY}")
foreach(LIB ${HOMEBREW_LIBS})
   set(LIBPATH "${HOMEBREW_X64_PREFIX}/opt/${LIB}/lib")
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
