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

project (CORE_DEV)

# include files
file(GLOB_RECURSE CORE_DEV_HEADER_FILES "*.h*")

# source files
set(CORE_DEV_SOURCE_FILES 
   Main.cpp
)

# set include directories
include_directories(
   ${Boost_INCLUDE_DIRS}
   ${CORE_SOURCE_DIR}/include
)

# define executable
add_executable(coredev ${CORE_DEV_SOURCE_FILES} ${CORE_DEV_HEADER_FILES})

# set link dependencies
target_link_libraries(coredev
   rstudio-core
)

# copy profiler script
configure_file(coredev-profile.in ${CMAKE_CURRENT_BINARY_DIR}/coredev-profile)



