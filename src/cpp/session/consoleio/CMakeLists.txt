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

project(CONSOLEIO)

# include files
file(GLOB_RECURSE CONSOLEIO_HEADER_FILES "*.h*")

# set include directories
include_directories(
   ${CORE_SOURCE_DIR}/include
)

set(CONSOLEIO_SOURCE_FILES
  ConsoleIOMain.cpp
)


# configure consoleio.rc
configure_file (${CMAKE_CURRENT_SOURCE_DIR}/consoleio.rc.in
                ${CMAKE_CURRENT_BINARY_DIR}/consoleio.rc)


configure_file (${CMAKE_CURRENT_SOURCE_DIR}/consoleio.exe.manifest
                ${CMAKE_CURRENT_BINARY_DIR}/consoleio.exe.manifest COPYONLY)

add_custom_command(
   OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/res.o"
   COMMAND "${WINDRES}"
      -I "."
      -i "consoleio.rc"
      -o "${CMAKE_CURRENT_BINARY_DIR}/res.o"
      -Ocoff
   DEPENDS
      "${CMAKE_CURRENT_BINARY_DIR}/consoleio.rc"
      "${CMAKE_CURRENT_SOURCE_DIR}/consoleio.exe.manifest")
set(CONSOLEIO_SOURCE_FILES
   ${CONSOLEIO_SOURCE_FILES}
   "${CMAKE_CURRENT_BINARY_DIR}/res.o")

add_executable(consoleio
  ${CONSOLEIO_SOURCE_FILES}
)

# set link dependencies
target_link_libraries(consoleio
   rstudio-core
)

install(TARGETS consoleio DESTINATION ${RSTUDIO_INSTALL_BIN})
