#
# fix-library-paths.cmake
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

# CMake's message is suppressed during install stage so just use echo here
function(echo MESSAGE)
   execute_process(COMMAND echo "-- ${MESSAGE}")
endfunction()

set(FIX_LIBRARY_PATHS_SCRIPT_PATH "@CMAKE_CURRENT_SOURCE_DIR@/scripts/fix-library-paths.sh")
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
      "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/MacOS/rsession-arm64"
      COPYONLY)

   # copy required Homebrew libraries
   set(HOMEBREW_LIBS gettext krb5 libpq openssl@1.1 sqlite3)

   file(MAKE_DIRECTORY "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Frameworks/arm64")
   foreach(LIB ${HOMEBREW_LIBS})
      set(LIBPATH "${HOMEBREW_ARM64_PREFIX}/opt/${LIB}/lib")
      file(GLOB LIBFILES "${LIBPATH}/*.dylib")
      foreach(LIBFILE ${LIBFILES})
         file(
            COPY "${LIBFILE}"
            DESTINATION "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Frameworks/arm64")
      endforeach()
   endforeach()

   # fix library paths on arm64 components
   execute_process(
      COMMAND
         "${FIX_LIBRARY_PATHS_SCRIPT_PATH}"
         "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Frameworks/arm64"
         "@executable_path/../Frameworks/arm64"
         "*.dylib")

   execute_process(
      COMMAND
         "${FIX_LIBRARY_PATHS_SCRIPT_PATH}"
         "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/MacOS"
         "@executable_path/../Frameworks/arm64"
         "rsession-arm64")

else()

   echo("No arm64 rsession binary available at '@RSESSION_ARM64_PATH@'")

endif()

# fix library paths on x86_64 components
execute_process(
   COMMAND
      "${FIX_LIBRARY_PATHS_SCRIPT_PATH}"
      "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Frameworks"
      "@executable_path/../Frameworks"
      "*.dylib")

execute_process(
   COMMAND
      "${FIX_LIBRARY_PATHS_SCRIPT_PATH}"
      "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/MacOS"
      "@executable_path/../Frameworks"
      "RStudio diagnostics rpostback rsession")


