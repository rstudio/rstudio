#
# CMakeLists.txt
#
# Copyright (C) 2009-18 by RStudio, Inc.
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

project(RSRUN)

# include files
file(GLOB_RECURSE RSRUN_HEADER_FILES "*.h*")

# set include directories
include_directories(
   ${CORE_SOURCE_DIR}/include
   ${CMAKE_CURRENT_BINARY_DIR}
)

set(RSRUN_SOURCE_FILES
   RsRunMain.cpp
)

# config file
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.in
               ${CMAKE_CURRENT_BINARY_DIR}/config.h)

if(WIN32)

   list(
      APPEND URLOPENER_SOURCE_FILES
      "${CMAKE_CURRENT_BINARY_DIR}/rsrun.rc")

   configure_file(
      ${CMAKE_CURRENT_SOURCE_DIR}/rsrun.rc.in
      ${CMAKE_CURRENT_BINARY_DIR}/rsrun.rc)

   configure_file(
      ${CMAKE_CURRENT_SOURCE_DIR}/rsrun.exe.manifest
      ${CMAKE_CURRENT_BINARY_DIR}/rsrun.exe.manifest COPYONLY)

endif()

add_executable(rsrun
  ${RSRUN_SOURCE_FILES}
)

# set link dependencies
target_link_libraries(rsrun
   rstudio-core
)

install(TARGETS rsrun DESTINATION ${RSTUDIO_INSTALL_BIN})

# if doing a package build on MacOS, reroute the OpenSSL libraries to our bundled copies
if(APPLE AND RSTUDIO_PACKAGE_BUILD)
   find_package(OpenSSL REQUIRED QUIET)
   foreach(lib ${OPENSSL_CRYPTO_LIBRARY} ${OPENSSL_SSL_LIBRARY})
      get_filename_component(LIB_DIR ${lib} PATH)
      execute_process(COMMAND readlink ${lib} OUTPUT_VARIABLE LIB_FILE OUTPUT_STRIP_TRAILING_WHITESPACE)
      add_custom_command (TARGET rsrun POST_BUILD
         COMMAND install_name_tool -change ${LIB_DIR}/${LIB_FILE} @executable_path/../Frameworks/${LIB_FILE} ${CMAKE_CURRENT_BINARY_DIR}/rsrun)
   endforeach()
endif()

