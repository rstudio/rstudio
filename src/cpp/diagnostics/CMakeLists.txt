#
# CMakeLists.txt
#
# Copyright (C) 2009-11 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

project(DIAGNOSTICS)

# include files
file(GLOB_RECURSE DIAGNOSTICS_HEADER_FILES "*.h*")

# set include directories
include_directories(
   ${CORE_SOURCE_DIR}/include
   ${CMAKE_CURRENT_BINARY_DIR}
)

set(DIAGNOSTICS_SOURCE_FILES
  DiagnosticsMain.cpp
)

# config file
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.in
               ${CMAKE_CURRENT_BINARY_DIR}/config.h)

if(WIN32)
   # configure diagnostics.rc
   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/diagnostics.rc.in
                   ${CMAKE_CURRENT_BINARY_DIR}/diagnostics.rc)


   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/diagnostics.exe.manifest
                   ${CMAKE_CURRENT_BINARY_DIR}/diagnostics.exe.manifest COPYONLY)

   add_custom_command(
      OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/res.o"
      COMMAND windres.exe
         -I "."
         -i "diagnostics.rc"
         -o "${CMAKE_CURRENT_BINARY_DIR}/res.o"
         -Ocoff
      DEPENDS
         "${CMAKE_CURRENT_BINARY_DIR}/diagnostics.rc"
         "${CMAKE_CURRENT_SOURCE_DIR}/diagnostics.exe.manifest")
   set(DIAGNOSTICS_SOURCE_FILES
      ${DIAGNOSTICS_SOURCE_FILES}
      "${CMAKE_CURRENT_BINARY_DIR}/res.o")
endif()

add_executable(diagnostics
  ${DIAGNOSTICS_SOURCE_FILES}
)

# set link dependencies
target_link_libraries(diagnostics
   rstudio-core
)
if(NOT RSTUDIO_SESSION_WIN64)
   install(TARGETS diagnostics DESTINATION ${RSTUDIO_INSTALL_BIN})
endif()
