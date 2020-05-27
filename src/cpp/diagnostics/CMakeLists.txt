#
# CMakeLists.txt
#
# Copyright (C) 2020 by RStudio, PBC
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

# if doing a package build on MacOS, reroute depdendent libraries to our bundled copies
if(APPLE AND RSTUDIO_PACKAGE_BUILD)

   find_package(OpenSSL REQUIRED QUIET)
   foreach(lib ${OPENSSL_CRYPTO_LIBRARY} ${OPENSSL_SSL_LIBRARY})
      # get the real path to the library (with all symlinks resolved) and filename
      get_filename_component(LIB_PATH "${lib}" REALPATH)
      get_filename_component(LIB_FILE "${LIB_PATH}" NAME)
      # this command does the following:
      # 1. runs otool -L to find the path at which the OpenSSL library is
      #    linked into the executable (this path is unfortunately not canonical
      #    and includes layers of both resolved and unresolved symlinks)
      # 2. extracts the path from the output using grep and awk
      # 3. runs install_name_tool and tells it to replace the path with one that
      #    points to a bundled copy of the same file
      add_custom_command (TARGET diagnostics POST_BUILD
         COMMAND install_name_tool -change `otool -L ${CMAKE_CURRENT_BINARY_DIR}/diagnostics | grep ${LIB_FILE} | awk '{ print $$1 }'` @executable_path/../Frameworks/${LIB_FILE} ${CMAKE_CURRENT_BINARY_DIR}/diagnostics)
   endforeach()

   # do something similar to the above to fix paths to SOCI libraries
   foreach(lib ${SOCI_DEPENDENCIES})
      get_filename_component(LIB_NAME "${lib}" NAME_WE)
      add_custom_command (TARGET diagnostics POST_BUILD
          COMMAND install_name_tool -change `otool -L ${CMAKE_CURRENT_BINARY_DIR}/diagnostics | grep ${LIB_NAME} | awk '{ print $$1 }'` @executable_path/../Frameworks/$$\(otool -L ${CMAKE_CURRENT_BINARY_DIR}/diagnostics | grep ${LIB_NAME} | awk '{ print $$1 }' | xargs basename\) ${CMAKE_CURRENT_BINARY_DIR}/diagnostics)
   endforeach()

endif()



