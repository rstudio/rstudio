#
# CMakeLists.txt
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

project (SERVER_CORE)

# include files
file(GLOB_RECURSE SERVER_CORE_HEADER_FILES "*.h*")

# source files
set (SERVER_CORE_SOURCE_FILES
   http/SecureCookie.cpp
   RVersionsScanner.cpp
   SecureKeyFile.cpp
   ServerLicense.cpp
   sessions/SessionSignature.cpp
   UrlPorts.cpp
)

if (RSTUDIO_HAS_SOCI)
   list(APPEND SERVER_CORE_SOURCE_FILES
      DatabaseUtils.cpp
      DatabaseUtilsOverlay.cpp
      ServerDatabase.cpp
      ServerDatabaseOverlay.cpp)
endif()

if (RSTUDIO_USE_PAM)
   list(APPEND SERVER_CORE_SOURCE_FILES system/Pam.cpp)
endif()

# define server core include dir
set(SERVER_CORE_INCLUDE_DIRS ${SERVER_CORE_INCLUDE_DIRS} include)

# additional include dirs
include_directories(
   ${SERVER_CORE_INCLUDE_DIRS}
   ${CMAKE_CURRENT_BINARY_DIR}
   ${TESTS_INCLUDE_DIR}
   ${CORE_SOURCE_DIR}/include
   ${SHARED_CORE_SOURCE_DIR}/include
)

# define shared library
add_library(rstudio-server-core STATIC ${SERVER_CORE_SOURCE_FILES} ${SERVER_CORE_HEADER_FILES})
define_source_file_names(rstudio-server-core)

# link dependencies
target_link_libraries(
   rstudio-server-core
   rstudio-core
   ${SERVER_CORE_SYSTEM_LIBRARIES}
)	

# define executable (for running unit tests)
if (RSTUDIO_UNIT_TESTS_ENABLED)
   # Use gtest from vendor directory
   set(GTEST_SOURCE_DIR ${CMAKE_SOURCE_DIR}/src/cpp/tests/cpp/tests/vendor/gtest)
   include_directories(${GTEST_SOURCE_DIR}/include)
   
   # Add Google Test if not already added
   if(NOT TARGET gtest)
      add_subdirectory(${GTEST_SOURCE_DIR} gtest-build EXCLUDE_FROM_ALL)
   endif()

   file(GLOB_RECURSE SERVER_CORE_TEST_FILES "*Tests.cpp")
   add_executable(rstudio-server-core-tests
      TestMain.cpp
      ${SERVER_CORE_TEST_FILES}
      ${SERVER_CORE_HEADER_FILES}
   )
   target_link_libraries(rstudio-server-core-tests
      rstudio-core
      rstudio-server-core
      ${Boost_LIBRARIES}
      ${CORE_SYSTEM_LIBRARIES}
      gtest
      pthread
   )
   
   enable_testing()
   include(GoogleTest)
   gtest_discover_tests(rstudio-server-core-tests)
endif()
