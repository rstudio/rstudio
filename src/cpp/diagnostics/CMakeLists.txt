#
# CMakeLists.txt
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
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
   ${SHARED_CORE_SOURCE_DIR}/include
   ${CMAKE_CURRENT_BINARY_DIR}
   ${EXT_SOURCE_DIR}
)

set(DIAGNOSTICS_SOURCE_FILES
  DiagnosticsMain.cpp
)

# config file
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.in
               ${CMAKE_CURRENT_BINARY_DIR}/config.h)

if(WIN32)

   list(
      APPEND DIAGNOSTICS_SOURCE_FILES
      "${CMAKE_CURRENT_BINARY_DIR}/diagnostics.rc")

   configure_file(
      ${CMAKE_CURRENT_SOURCE_DIR}/diagnostics.rc.in
      ${CMAKE_CURRENT_BINARY_DIR}/diagnostics.rc)

   configure_file(
      ${CMAKE_CURRENT_SOURCE_DIR}/diagnostics.exe.manifest
      ${CMAKE_CURRENT_BINARY_DIR}/diagnostics.exe.manifest COPYONLY)

endif()

add_stripped_executable(diagnostics
  ${DIAGNOSTICS_SOURCE_FILES}
)

# set link dependencies
target_link_libraries(diagnostics
   rstudio-core
)

if(NOT RSTUDIO_SESSION_WIN32)
   install(TARGETS diagnostics DESTINATION ${RSTUDIO_INSTALL_BIN})
endif()

