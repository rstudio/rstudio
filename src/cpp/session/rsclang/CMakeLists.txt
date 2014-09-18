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

project(RSCLANG)

# verify libclang is installed
set(LIBCLANG_VERSION "3.5")
set(LIBCLANG_DIR "${RSTUDIO_DEPENDENCIES_DIR}/common/libclang/${LIBCLANG_VERSION}")
if(NOT EXISTS "${LIBCLANG_DIR}")
  message(FATAL_ERROR "liblang ${LIBCLANG_VERSION} not found  (re-run install-dependencies script to install)")
endif()

# include files
file(GLOB_RECURSE RSCLANG_HEADER_FILES "*.h*")

# set include directories
include_directories(
   ${CORE_SOURCE_DIR}/include
   ${CMAKE_CURRENT_SOURCE_DIR}/include
   ${CMAKE_CURRENT_BINARY_DIR}
)

set(RSCLANG_SOURCE_FILES
  Main.cpp
  libclang/libclang.cpp
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
   set(RSCLANG_SOURCE_FILES
      ${RSCLANG_SOURCE_FILES}
      "${CMAKE_CURRENT_BINARY_DIR}/res.o")
endif()

add_executable(rsclang
  ${RSCLANG_SOURCE_FILES} ${RSCLANG_HEADER_FILES}
)

# set link dependencies
target_link_libraries(rsclang
   rstudio-core
   ${CMAKE_DL_LIBS}
)

if(NOT RSTUDIO_SESSION_WIN64)

   # install rsclang utility
   install(TARGETS rsclang DESTINATION ${RSTUDIO_INSTALL_BIN})

   # install libclang
   file(GLOB LIBCLANG_FILES "${LIBCLANG_DIR}/libclang.*")
   install(PROGRAMS ${LIBCLANG_FILES}
           DESTINATION  ${RSTUDIO_INSTALL_BIN}/libclang)

endif()
