#
# CMakeLists.txt
#
# Copyright (C) 2021 by RStudio, PBC
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

if(RSTUDIO_DESKTOP AND (NOT RSTUDIO_SESSION_WIN32))
   add_subdirectory(desktop)
endif()

if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/CMakeOverlay.txt")
   include(CMakeOverlay.txt)
endif()
