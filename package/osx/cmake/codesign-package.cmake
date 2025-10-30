#
# codesign-package-electron.cmake
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

cmake_minimum_required(VERSION 3.19)

# CMake's message is suppressed during install stage so just use echo here
function(echo MESSAGE)
   execute_process(COMMAND echo "-- ${MESSAGE}")
endfunction()

# NOTE: we always attempt to sign a package build of RStudio
# (even if it's just a development build) as our usages of
# install_name_tool will invalidate existing signatures on
# bundled libraries and macOS will refuse to launch RStudio
# with the older invalid signature
if(@RSTUDIO_CODESIGN_USE_CREDENTIALS@)
   echo("codesign: using RStudio's credentials")
   set(ENV{RSESSION_ENTITLEMENTS_TYPE} "ci")
   list(APPEND CODESIGN_FLAGS --sign 4D663D999011E80361D8848C8487D70E4C41DB60)
   list(APPEND CODESIGN_FLAGS --prefix org.rstudio.)
else()
   echo("codesign: using ad-hoc signature")
   set(ENV{RSESSION_ENTITLEMENTS_TYPE} "adhoc")
   list(APPEND CODESIGN_FLAGS --sign -)
endif()

# include keychain path if provided
if(@RSTUDIO_CODESIGN_KEYCHAIN_PATH@)
   echo("codesign: using keychain at @RSTUDIO_CODESIGN_KEYCHAIN_PATH@")
   list(APPEND CODESIGN_FLAGS --keychain "@RSTUDIO_CODESIGN_KEYCHAIN_PATH@")
else()
   echo("codesign: using default keychain search list")
endif()

# add other flags -- codesign can be picky about argument order
list(APPEND CODESIGN_FLAGS --options runtime)
list(APPEND CODESIGN_FLAGS --timestamp --force --deep)

execute_process(
   COMMAND
      "@CMAKE_CURRENT_SOURCE_DIR@/scripts/codesign-package.sh"
      "@CMAKE_INSTALL_PREFIX@/RStudio.app"
      ${CODESIGN_FLAGS}
   WORKING_DIRECTORY
      "@CMAKE_CURRENT_SOURCE_DIR@"
   OUTPUT_VARIABLE CODESIGN_OUTPUT ECHO_OUTPUT_VARIABLE
   ERROR_VARIABLE CODESIGN_ERROR ECHO_ERROR_VARIABLE
   COMMAND_ERROR_IS_FATAL ANY
)

