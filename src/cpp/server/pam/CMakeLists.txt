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

project (PAM)

# include files
file(GLOB_RECURSE PAM_HEADER_FILES "*.h*")

# find PAM
find_package(PAM REQUIRED)

# source files
set(PAM_SOURCE_FILES
   PamMain.cpp
)

# set include directories
include_directories(
   ${Boost_INCLUDE_DIRS}
   ${CORE_SOURCE_DIR}/include
   ${SERVER_CORE_SOURCE_DIR}/include
   ${SHARED_CORE_SOURCE_DIR}/include
   ${PAM_INCLUDE_DIR}
)


# define executable
add_stripped_executable(rserver-pam ${PAM_SOURCE_FILES} ${PAM_HEADER_FILES})

# set link dependencies
target_link_libraries(rserver-pam
   rstudio-core
   rstudio-server-core
   ${PAM_LIBRARIES}
   ${CRASHPAD_LIBRARIES}
)

# installation rules
install(TARGETS rserver-pam DESTINATION ${RSTUDIO_INSTALL_BIN})




