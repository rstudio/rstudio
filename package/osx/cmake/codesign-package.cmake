#
# codesign-package.cmake
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

# don't follow symlinks in GLOB_RECURSE
cmake_policy(SET CMP0009 NEW)
cmake_policy(SET CMP0011 NEW)

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

list(APPEND CODESIGN_TARGETS "${CMAKE_INSTALL_PREFIX}/RStudio.app")

file(GLOB_RECURSE CODESIGN_PLUGINS "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/plugins")
list(APPEND CODESIGN_TARGETS ${CODESIGN_PLUGINS})

file(GLOB_RECURSE CODESIGN_FRAMEWORKS "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Frameworks")
list(APPEND CODESIGN_TARGETS ${CODESIGN_FRAMEWORKS})

file(GLOB_RECURSE CODESIGN_MACOS "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/MacOS")
list(APPEND CODESIGN_TARGETS ${CODESIGN_MACOS})

# deep sign all targets
foreach(CODESIGN_TARGET ${CODESIGN_TARGETS})
   echo("Signing ${CODESIGN_TARGET}")
	execute_process(COMMAND codesign ${CODESIGN_FLAGS} "${CODESIGN_TARGET}")
endforeach()
