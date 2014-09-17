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

project(CLANG)

# include files
file(GLOB_RECURSE CLANG_HEADER_FILES "*.h*")

# set include directories
include_directories(
   ${CORE_SOURCE_DIR}/include
   ${CMAKE_CURRENT_SOURCE_DIR}/include
   ${CMAKE_CURRENT_BINARY_DIR}
)

set(CLANG_SOURCE_FILES
  Main.cpp
)

# config file
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/rsclang-config.h.in
               ${CMAKE_CURRENT_BINARY_DIR}/rsclang-config.h)

if(WIN32)
   # configure rsclang.rc
   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/rsclang.rc.in
                   ${CMAKE_CURRENT_BINARY_DIR}/rsclang.rc)


   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/rsclang.exe.manifest
                   ${CMAKE_CURRENT_BINARY_DIR}/rsclang.exe.manifest COPYONLY)

   add_custom_command(
      OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/res.o"
      COMMAND windres.exe
         -I "."
         -i "rsclang.rc"
         -o "${CMAKE_CURRENT_BINARY_DIR}/res.o"
         -Ocoff
      DEPENDS
         "${CMAKE_CURRENT_BINARY_DIR}/rsclang.rc"
         "${CMAKE_CURRENT_SOURCE_DIR}/rsclang.exe.manifest")
   set(CLANG_SOURCE_FILES
      ${CLANG_SOURCE_FILES}
      "${CMAKE_CURRENT_BINARY_DIR}/res.o")
endif()

add_executable(rsclang
  ${CLANG_SOURCE_FILES} ${CLANG_HEADER_FILES}
)

# set link dependencies
target_link_libraries(rsclang
   rstudio-core
   ${CMAKE_DL_LIBS}
)

if(NOT RSTUDIO_SESSION_WIN64)
   install(TARGETS rsclang DESTINATION ${RSTUDIO_INSTALL_BIN})
endif()
