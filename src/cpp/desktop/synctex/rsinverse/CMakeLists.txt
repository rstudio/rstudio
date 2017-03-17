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

project(RSINVERSE)

# include files
file(GLOB_RECURSE RSINVERSE_HEADER_FILES "*.h*")

# set include directories
include_directories(
   ${CORE_SOURCE_DIR}/include
)

set(RSINVERSE_SOURCE_FILES
  RsInverseMain.cpp
)


# configure rsinverse.rc
configure_file (${CMAKE_CURRENT_SOURCE_DIR}/rsinverse.rc.in
                ${CMAKE_CURRENT_BINARY_DIR}/rsinverse.rc)


configure_file (${CMAKE_CURRENT_SOURCE_DIR}/rsinverse.exe.manifest
                ${CMAKE_CURRENT_BINARY_DIR}/rsinverse.exe.manifest COPYONLY)

set(LINK_FLAGS -Wl,-subsystem,windows -lversion)
add_custom_command(
   OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/res.o"
   COMMAND "${WINDRES}"
      -I "."
      -i "rsinverse.rc"
      -o "${CMAKE_CURRENT_BINARY_DIR}/res.o"
      -Ocoff
   DEPENDS
      "${CMAKE_CURRENT_BINARY_DIR}/rsinverse.rc"
      "${CMAKE_CURRENT_SOURCE_DIR}/rsinverse.exe.manifest")
set(RSINVERSE_SOURCE_FILES
   ${RSINVERSE_SOURCE_FILES}
   "${CMAKE_CURRENT_BINARY_DIR}/res.o")

set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -mwindows")

add_executable(rsinverse
  ${RSINVERSE_SOURCE_FILES}
)

# set link dependencies
target_link_libraries(rsinverse
   rstudio-core
)

install(TARGETS rsinverse DESTINATION ${RSTUDIO_INSTALL_BIN})
