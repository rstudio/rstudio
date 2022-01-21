#
# CMakeLists.txt
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

cmake_minimum_required(VERSION 3.4.3)
project(ELECTRON_DESKTOP)

# install mac-terminal script on apple
if(APPLE)
   configure_file(${CMAKE_CURRENT_SOURCE_DIR}/mac-terminal.in
                  ${CMAKE_CURRENT_BINARY_DIR}/mac-terminal)
endif()

# defines NODEJS, NODEJS_PATH, YARN, YARN_PATH
include("${CMAKE_CURRENT_LIST_DIR}/../CMakeNodeTools.txt")

# ensure yarn and node are on the path
set(MODIFIED_PATH "${NODEJS_PATH}:${YARN_PATH}:$ENV{PATH}")

# copy sources to build directory. note that the build directory cannot
# be the "true" CMake directory as some files are resolved relative to
# the desktop project's relative path in the application structure
set(ELECTRON_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}" CACHE INTERNAL "")
set(ELECTRON_BUILD_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../desktop-build-${UNAME_M}" CACHE INTERNAL "")
file(REMOVE_RECURSE "${ELECTRON_BUILD_DIR}")
file(MAKE_DIRECTORY "${ELECTRON_BUILD_DIR}")
file(
   COPY "${CMAKE_CURRENT_SOURCE_DIR}/"
   DESTINATION "${ELECTRON_BUILD_DIR}/"
   REGEX "/out$" EXCLUDE
   REGEX "/node_modules$" EXCLUDE)

# build electron components
add_custom_target(
   electron_build ALL
   DEPENDS rsession diagnostics
   WORKING_DIRECTORY "${ELECTRON_BUILD_DIR}"
   COMMAND ${CMAKE_COMMAND} -E env "PATH=${MODIFIED_PATH}" "PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1" ${YARN} make
)

if(TARGET gwt_build)
   add_dependencies(electron_build gwt_build)
endif()

