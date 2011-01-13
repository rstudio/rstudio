#
# CMakeLists.txt
#
# Copyright (C) 2009-11 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

project (PAMCHK)

# include files
file(GLOB_RECURSE PAMCHK_HEADER_FILES "*.h*")

# source files
set(PAMCHK_SOURCE_FILES
   PamchkMain.cpp
)

# find PAM
find_package(PAM REQUIRED QUIET)

# set include directories
include_directories(
   ${Boost_INCLUDE_DIRS}
   ${CORE_SOURCE_DIR}/include
   ${PAM_INCLUDE_DIR}
)

# define executable
add_executable(rserver_pamchk ${PAMCHK_SOURCE_FILES} ${PAMCHK_HEADER_FILES})

# set link dependencies
target_link_libraries(rserver_pamchk
   rstudio-core
   ${PAM_LIBRARIES}
)

# installation rules
install(TARGETS rserver_pamchk DESTINATION ${RSTUDIO_INSTALL_BIN})

