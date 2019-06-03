#
# CMakeLists.txt
#
# Copyright (C) 2009-19 by RStudio, Inc.
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

project(DIAGNOSTICS)

# include files
file(GLOB_RECURSE DIAGNOSTICS_HEADER_FILES "*.h*")

# set include directories
include_directories(
   ${CORE_SOURCE_DIR}/include
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

# add origin rpath for suse/sles
if(RSTUDIO_PACKAGE_VARIANT STREQUAL "SLES")
   set(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
   set_target_properties(diagnostics PROPERTIES
                         INSTALL_RPATH \$ORIGIN)
endif()

# set link dependencies
target_link_libraries(diagnostics
   rstudio-core
)
if(NOT RSTUDIO_SESSION_WIN32)
   install(TARGETS diagnostics DESTINATION ${RSTUDIO_INSTALL_BIN})
endif()

# if doing a package build on MacOS, reroute the OpenSSL libraries to our bundled copies
if(APPLE AND RSTUDIO_PACKAGE_BUILD)
   find_package(OpenSSL REQUIRED QUIET)
   foreach(lib ${OPENSSL_CRYPTO_LIBRARY} ${OPENSSL_SSL_LIBRARY})
      get_filename_component(LIB_DIR ${lib} PATH)
      execute_process(COMMAND readlink ${lib} OUTPUT_VARIABLE LIB_FILE OUTPUT_STRIP_TRAILING_WHITESPACE)
      add_custom_command (TARGET diagnostics POST_BUILD
         COMMAND install_name_tool -change ${LIB_DIR}/${LIB_FILE} @executable_path/../Frameworks/${LIB_FILE} ${CMAKE_CURRENT_BINARY_DIR}/diagnostics)
   endforeach()
endif()
