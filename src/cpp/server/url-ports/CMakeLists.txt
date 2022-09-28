#
# CMakeLists.txt
#
# Copyright (C) 2022 by RStudio, PBC
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

project (RSERVER_URLS)

# include files
#file(GLOB_RECURSE URL_HEADER_FILES "*.h*")

# source files
set(URL_SOURCE_FILES
    UrlPortsMain.cpp
)

# set include directories
include_directories(
   include
   ${Boost_INCLUDE_DIRS}
   ${SERVER_SYSTEM_INCLUDE_DIRS}
   ${CORE_INCLUDE_DIRS}
   ${SERVER_CORE_SOURCE_DIR}/include
   ${SHARED_CORE_SOURCE_DIR}/include
   ${CORE_SOURCE_DIR}/include
   ${TESTS_INCLUDE_DIR}
)

# define executable
add_stripped_executable(rserver-url ${URL_SOURCE_FILES})

# set link dependencies
target_link_libraries(rserver-url
   rstudio-server-core
)

# installation rules
install(TARGETS rserver-url DESTINATION ${RSTUDIO_INSTALL_BIN})

# test files
if (RSTUDIO_UNIT_TESTS_ENABLED)

   set(URL_TEST_FILES
       UrlPortsMainTests.cpp)

   add_executable(rserver-url-tests
      TestMain.cpp
      ${URL_TEST_FILES}
   )

   target_link_libraries(rserver-url-tests
      rstudio-server-core
   )
endif()
