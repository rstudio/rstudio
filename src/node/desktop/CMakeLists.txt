#
# CMakeLists.txt
#
# Copyright (C) 2021 by RStudio, PBC
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

cmake_minimum_required(VERSION 3.4.3)
project(ELECTRON_DESKTOP)

# defines NODEJS, NODEJS_PATH, YARN, YARN_PATH
include("${CMAKE_CURRENT_LIST_DIR}/../CMakeNodeTools.txt")

# ensure yarn and node are on the path
set(MODIFIED_PATH "${NODEJS_PATH}:${YARN_PATH}:$ENV{PATH}")

add_custom_target(electron_build ALL)
if (TARGET gwt_build)
   add_dependencies(electron_build gwt_build rsession diagnostics)
else()
   # for iterating on build infrastructure, skipping gwt build
   add_dependencies(electron_build rsession diagnostics)
endif()

# install mac-terminal script on apple
if(APPLE)
   configure_file(${CMAKE_CURRENT_SOURCE_DIR}/mac-terminal.in
                  ${CMAKE_CURRENT_BINARY_DIR}/mac-terminal)
endif()

# Install node_modules during build phase
add_custom_command(
   TARGET electron_build
   PRE_BUILD
   WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}"
   COMMAND ${CMAKE_COMMAND} -E env PATH="${MODIFIED_PATH}" PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
      ${YARN}
      "install")

# Actual building and bundling of the Electron application is done via `yarn make`, not directly
# by the cmake-driven build. See the `make-package` scripts in the `package` folder at root
# of this repo.
