#
# prepare-package.cmake
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit pursuant
# to the terms of a commercial license agreement with Posit, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

cmake_minimum_required(VERSION 3.4.3)

# CMake's message is suppressed during install stage so just use echo here
function(echo MESSAGE)
   execute_process(COMMAND echo "-- ${MESSAGE}")
endfunction()

if(@RSTUDIO_ELECTRON@)
   set(RSESSION_BINARY_DIR "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Resources/app/bin")
   set(X64_FRAMEWORKS_DIRECTORY "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Resources/app/Frameworks")
   set(ARM64_FRAMEWORKS_DIRECTORY "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Resources/app/Frameworks/arm64")
else()
   set(RSESSION_BINARY_DIR "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/MacOS")
   set(X64_FRAMEWORKS_DIRECTORY "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Frameworks")
   set(ARM64_FRAMEWORKS_DIRECTORY "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Frameworks/arm64")
endif()

set(FIX_LIBRARY_PATHS_SCRIPT_PATH "@CMAKE_CURRENT_SOURCE_DIR@/scripts/fix-library-paths.sh")

# NOTE: This part of CMake will be run by the x86 branch of the build,
# so we don't want to filter based on the architecture here.
if(EXISTS "@RSESSION_ARM64_PATH@")

   echo("Found arm64 rsession binary: '@RSESSION_ARM64_PATH@'")

   # find out where arm64 homebrew lives
   if(EXISTS "$ENV{HOME}/homebrew/arm64")
      set(HOMEBREW_ARM64_PREFIX "$ENV{HOME}/homebrew/arm64")
   else()
      set(HOMEBREW_ARM64_PREFIX "/opt/homebrew")
   endif()

   echo("Homebrew prefix: '${HOMEBREW_ARM64_PREFIX}'")

   # copy arm64 rsession binary
   configure_file(
      "@RSESSION_ARM64_PATH@"
      "${RSESSION_BINARY_DIR}/rsession-arm64"
      COPYONLY)

   # copy required Homebrew libraries
   set(HOMEBREW_LIBS gettext krb5 libpq openssl@1.1 sqlite3)

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

if(@RSTUDIO_ELECTRON@)

   # find out where x64 homebrew lives
   if(EXISTS "$ENV{HOME}/homebrew/x86_64")
      set(HOMEBREW_X64_PREFIX "$ENV{HOME}/homebrew/x86_64")
   else()
      set(HOMEBREW_X64_PREFIX "/usr/local")
   endif()
   
   # copy required Homebrew libraries
   set(HOMEBREW_LIBS gettext krb5 libpq openssl@1.1 sqlite3)
   
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

endif()

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

if(NOT @RSTUDIO_ELECTRON@)
   execute_process(
      COMMAND
      "${FIX_LIBRARY_PATHS_SCRIPT_PATH}"
      "${RSESSION_BINARY_DIR}"
      "@executable_path/../Frameworks"
      "RStudio")
endif()

