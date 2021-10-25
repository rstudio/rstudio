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

if(NOT DEFINED RSTUDIO_NODE_VERSION)
   set(RSTUDIO_NODE_VERSION "14.17.5")
endif()
if(DEFINED ENV{RSTUDIO_NODE_VERSION})
   set(RSTUDIO_NODE_VERSION $ENV{RSTUDIO_NODE_VERSION})
endif()

# Detect node.js and yarn; prefer versions supplied by the dependency
# scripts but fall back to whatever is found on path.
find_program(NODEJS
   NAMES node
   NO_DEFAULT_PATH PATH_SUFFIXES "bin"
   PATHS "/opt/rstudio-tools/dependencies/common/node/${RSTUDIO_NODE_VERSION}"
         "${CMAKE_CURRENT_SOURCE_DIR}/../../../dependencies/common/node/${RSTUDIO_NODE_VERSION}")

if(NOT NODEJS)
   # fall back on any available node.js
   find_program(NODEJS NAMES node)
endif()

if(NODEJS)
   message(STATUS "Using node.js: ${NODEJS}")
else()
   message(FATAL_ERROR "node.js not found (required)")
endif()

find_program(YARN
    NAMES yarn
    NO_DEFAULT_PATH
    PATHS "$ENV{HOME}/.yarn/bin")
if (NOT YARN)
  find_program(YARN NAMES yarn)
else()

endif()

if(YARN)
   message(STATUS "Using yarn: ${YARN}")
else()
   message(STATUS "yarn not found (required for Electron)")
   set(YARN yarn)
endif()

get_filename_component(NODEJS_PATH ${NODEJS} DIRECTORY CACHE)
get_filename_component(YARN_PATH ${YARN} DIRECTORY CACHE)

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
   COMMAND ${CMAKE_COMMAND} -E env PATH="${NODEJS_PATH}:${YARN_PATH}:$ENV{PATH}" PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
      ${YARN}
      "install")

# Actual building and bundling of the Electron application is done via `yarn make`, not directly
# by the cmake-driven build. See the `make-package` scripts in the `package` folder at root
# of this repo.
