#
# CMakeLists.txt
#
# Copyright (C) 2020 by RStudio, PBC
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

# set minimum version
cmake_minimum_required(VERSION 3.4.3)

# don't add gwt for special 32-bit binary builds on windows (since
# we've already got it from the 64-bit build), for development mode
# (since we'll want to build it incrementally using superdevmode),
# or when building monitor only
if ( (NOT RSTUDIO_SESSION_WIN32) AND
     (NOT RSTUDIO_DEVELOPMENT) AND
     (NOT RSTUDIO_CONFIG_MONITOR_ONLY) )
   add_subdirectory(gwt)
endif()

add_subdirectory(cpp)

if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/CMakeOverlay.txt")
   include(CMakeOverlay.txt)
endif()

