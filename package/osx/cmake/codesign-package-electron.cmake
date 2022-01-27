#
# codesign-package-electron.cmake
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

cmake_minimum_required(VERSION 3.19)

# CMake's message is suppressed during install stage so just use echo here
function(echo MESSAGE)
   execute_process(COMMAND echo "-- ${MESSAGE}")
endfunction()

# flags to pass to codesign executable
set(CODESIGN_FLAGS
   --options runtime
   --timestamp
   --entitlements "@CMAKE_CURRENT_SOURCE_DIR@/entitlements.plist"
   --force
   --deep)

# NOTE: we always attempt to sign a package build of RStudio
# (even if it's just a development build) as our usages of
# install_name_tool will invalidate existing signatures on
# bundled libraries and macOS will refuse to launch RStudio
# with the older invalid signature
if(@RSTUDIO_CODESIGN_USE_CREDENTIALS@)
   echo("codesign: using RStudio's credentials")
   list(APPEND CODESIGN_FLAGS
      -s 8A388E005EF927A09B952C6E71B0E8F2F467AB26
      -i org.rstudio.RStudio)
else()
   echo("codesign: using ad-hoc signature")
   list(APPEND CODESIGN_FLAGS -s -)
endif()

execute_process(
   COMMAND
      "@CMAKE_CURRENT_SOURCE_DIR@/scripts/codesign-package.sh"
      "@CMAKE_INSTALL_PREFIX@/RStudio.app"
      ${CODESIGN_FLAGS}
   WORKING_DIRECTORY
      "@CMAKE_INSTALL_PREFIX@"
   OUTPUT_VARIABLE CODESIGN_OUTPUT ECHO_OUTPUT_VARIABLE
   ERROR_VARIABLE CODESIGN_ERROR ECHO_ERROR_VARIABLE
   COMMAND_ERROR_IS_FATAL ANY
)

