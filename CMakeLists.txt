#
# CMakeLists.txt
#
# Copyright (C) 2009-12 by RStudio, Inc.
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

# use clang on osx
if(APPLE)
  set(CMAKE_C_COMPILER /usr/bin/cc)
  set(CMAKE_CXX_COMPILER /usr/bin/c++)
endif()

cmake_minimum_required(VERSION 2.6)
project (RStudio)

# project globals
include(CMakeGlobals.txt)

# remove previous installation if requested
if(RSTUDIO_UNINSTALL_PREVIOUS)
  install(CODE "execute_process(COMMAND rm -rf \${CMAKE_INSTALL_PREFIX})")
endif()

# install root docs
if (NOT RSTUDIO_SESSION_WIN64)
   # dynamically configure SOURCE with the git revision hash
   INSTALL(CODE "
      exec_program(git ARGS rev-parse HEAD
                   OUTPUT_VARIABLE RSTUDIO_GIT_REVISION_HASH)
      configure_file (\"${CMAKE_CURRENT_SOURCE_DIR}/SOURCE.in\"
                      \"${CMAKE_CURRENT_BINARY_DIR}/SOURCE\")
    ")

   configure_file(${CMAKE_CURRENT_SOURCE_DIR}/VERSION.in
                  ${CMAKE_CURRENT_BINARY_DIR}/VERSION)

   # install root docs
   install(FILES README.md
                 INSTALL
                 COPYING
                 NOTICE
                 ${CMAKE_CURRENT_BINARY_DIR}/SOURCE
                 ${CMAKE_CURRENT_BINARY_DIR}/VERSION
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING})
endif()

# overlay
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/CMakeOverlay.txt")
   include(CMakeOverlay.txt)
endif()

# main src
add_subdirectory(src)

# packaging
add_subdirectory(package)





   


