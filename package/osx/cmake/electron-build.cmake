#
# electron-build.cmake
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

execute_process(
	COMMAND
		"@CMAKE_CURRENT_SOURCE_DIR@/electron-build.sh"
		"@CMAKE_CURRENT_SOURCE_DIR@/../../src/node/desktop"
		"@CPACK_PACKAGE_VERSION@"
		"@CMAKE_CURRENT_SOURCE_DIR@/install"
)

