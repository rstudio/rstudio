#
# merge-electron.cmake
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

# CMake's message is suppressed during install stage so just use echo here
function(echo MESSAGE)
   execute_process(COMMAND echo "-- ${MESSAGE}")
endfunction()

# run npm to ensure required packages are installed
execute_process(
	COMMAND
		"@NPM@"
		"ci"
	WORKING_DIRECTORY
		"@CMAKE_CURRENT_SOURCE_DIR@/scripts"
)


# run the merge script
execute_process(
	COMMAND
		"@NODEJS@"
		"@CMAKE_CURRENT_SOURCE_DIR@/scripts/merge-electron.mjs"
		"@ELECTRON_SOURCE_DIR@/../desktop-build-x86_64/out/RStudio-darwin-x64/RStudio.app"
		"@ELECTRON_SOURCE_DIR@/../desktop-build-arm64/out/RStudio-darwin-arm64/RStudio.app"
		"@CMAKE_CURRENT_SOURCE_DIR@/install/RStudio.app"
	WORKING_DIRECTORY
		"@CMAKE_CURRENT_SOURCE_DIR@/scripts"
)

