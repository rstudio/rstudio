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
if (NOT WIN32)
   set(MODIFIED_PATH "${NODEJS_PATH}:${YARN_PATH}:$ENV{PATH}")
endif()

set(ELECTRON_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}" CACHE INTERNAL "")
if (APPLE)
   # copy sources to build directory. note that the build directory cannot
   # be the "true" CMake directory as some files are resolved relative to
   # the desktop project's relative path in the application structure
   set(ELECTRON_BUILD_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../desktop-build-${UNAME_M}" CACHE INTERNAL "")
   file(MAKE_DIRECTORY "${ELECTRON_BUILD_DIR}")
   file(
      COPY "${CMAKE_CURRENT_SOURCE_DIR}/"
      DESTINATION "${ELECTRON_BUILD_DIR}/"
      REGEX "/out$" EXCLUDE
      REGEX "/node_modules$" EXCLUDE)
else()
   set(ELECTRON_BUILD_DIR "${ELECTRON_SOURCE_DIR}" CACHE INTERNAL "")
endif()

# define source dependencies
file(GLOB_RECURSE DESKTOP_SOURCES CONFIGURE_DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/src/*.ts")
list(APPEND DESKTOP_SOURCES package.json forge.config.js)

# define custom command for building RStudio with Electron
# note that OUTPUT tells CMake that this command should be used
# if that output does not exist and needs to be created
if (WIN32)

   add_custom_command(
      OUTPUT "${ELECTRON_BUILD_DIR}/out"
      DEPENDS "${DESKTOP_SOURCES}"
      COMMENT "Building desktop (Electron ${UNAME_M})"
      WORKING_DIRECTORY "${ELECTRON_BUILD_DIR}"
      COMMAND ${YARN} package
   )

else()

   add_custom_command(
      OUTPUT "${ELECTRON_BUILD_DIR}/out"
      DEPENDS "${DESKTOP_SOURCES}"
      COMMENT "Building desktop (Electron ${UNAME_M})"
      WORKING_DIRECTORY "${ELECTRON_BUILD_DIR}"
      COMMAND ${CMAKE_COMMAND} -E env "PATH=${MODIFIED_PATH}" "PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1" ${YARN} package
   )

endif()

# define a target, indicating that building this target
# requires that aforementioned OUTPUT file
# (this effectively "links" this target with that command)
add_custom_target(
   electron_build ALL
   DEPENDS "${ELECTRON_BUILD_DIR}/out"
)

# add target dependencies, so we don't build too many things at once
add_dependencies(electron_build rsession diagnostics)
if(TARGET gwt_build)
   add_dependencies(electron_build gwt_build)
endif()

# install binary
if (WIN32)
   install(DIRECTORY "${ELECTRON_BUILD_DIR}/out/RStudio-win32-x64/" DESTINATION "${RSTUDIO_INSTALL_ELECTRON}")
endif()
