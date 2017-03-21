#
# CMakeLists.txt
#
# Copyright (C) 2009-11 by RStudio, Inc.
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

project(URLOPENER)

# include files
file(GLOB_RECURSE URLOPENER_HEADER_FILES "*.h*")

# set include directories
include_directories(
   ${CORE_SOURCE_DIR}/include
)

set(URLOPENER_SOURCE_FILES
  UrlOpenerMain.cpp
)


# configure urlopener.rc
configure_file (${CMAKE_CURRENT_SOURCE_DIR}/urlopener.rc.in
                ${CMAKE_CURRENT_BINARY_DIR}/urlopener.rc)


configure_file (${CMAKE_CURRENT_SOURCE_DIR}/urlopener.exe.manifest
                ${CMAKE_CURRENT_BINARY_DIR}/urlopener.exe.manifest COPYONLY)

set(LINK_FLAGS -Wl,-subsystem,windows -lversion)
add_custom_command(
   OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/res.o"
   COMMAND "${WINDRES}"
      -I "."
      -i "urlopener.rc"
      -o "${CMAKE_CURRENT_BINARY_DIR}/res.o"
      -Ocoff
   DEPENDS
      "${CMAKE_CURRENT_BINARY_DIR}/urlopener.rc"
      "${CMAKE_CURRENT_SOURCE_DIR}/urlopener.exe.manifest")
set(URLOPENER_SOURCE_FILES
   ${URLOPENER_SOURCE_FILES}
   "${CMAKE_CURRENT_BINARY_DIR}/res.o")

add_executable(urlopener
  ${URLOPENER_SOURCE_FILES}
)

# set link dependencies
target_link_libraries(urlopener
   rstudio-core
)

install(TARGETS urlopener DESTINATION ${RSTUDIO_INSTALL_BIN})
