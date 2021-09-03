#
# CMakeLists.txt
#
# Copyright (C) 2021 by RStudio, PBC
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

cmake_minimum_required(VERSION 3.4.3)
project (RSTUDIO_ELECTRON)

# allow opt in / out of ELECTRON build
# mainly for faster iteration in builds
if(NOT DEFINED ELECTRON_BUILD)
   set(ELECTRON_BUILD Yes)
endif()

if(DEFINED ENV{ELECTRON_BUILD})
   set(ELECTRON_BUILD $ENV{ELECTRON_BUILD})
endif()

if(ELECTRON_BUILD)
   message(STATUS "Configured to build ELECTRON")

   if(NOT DEFINED RSTUDIO_NODE_VERSION)
      set(RSTUDIO_NODE_VERSION "14.17.5")
   endif()
   if(DEFINED ENV{RSTUDIO_NODE_VERSION})
      set(RSTUDIO_NODE_VERSION $ENV{RSTUDIO_NODE_VERSION})
   endif()

   # prefer specific node.js version installed via dependency scripts
   find_program(NODEJS node NO_DEFAULT_PATH PATH_SUFFIXES "bin"
      PATHS "/opt/rstudio-tools/dependencies/common/node/${RSTUDIO_NODE_VERSION}"
            "${CMAKE_CURRENT_SOURCE_DIR}/../../../dependencies/common/node/${RSTUDIO_NODE_VERSION}")

   if(NOT NODEJS)
      # fall back on any available node.js
      find_program(NODEJS NAMES node)
   endif()

   if(NODEJS)
      message(STATUS "Using node.js: ${NODEJS}")
   else()
      message(FATAL_ERROR "node.js not found (required to build ELECTRON)")
   endif()

   if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/CMakeOverlay.txt")
      include(CMakeOverlay.txt)
   endif()
endif()
