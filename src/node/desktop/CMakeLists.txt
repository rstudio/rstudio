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
project(ELECTRON_DESKTOP)

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

   # build Electron Desktop with node.js
   add_custom_target(electron_build ALL)
   add_custom_command(
      TARGET electron_build
      POST_BUILD
      WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}"
      COMMAND
         ${NODEJS}
         "make")
endif()
