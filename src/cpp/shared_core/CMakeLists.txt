#
# CMakeLists.txt
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the following terms:
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
# documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
# rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
# permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
# Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
# WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
# COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
# OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
#

project(SHARED_CORE)

# include files
file(GLOB_RECURSE SHARED_CORE_HEADER_FILES "*.h*")

# source files
set (SHARED_CORE_SOURCE_FILES
   Assert.cpp
   Error.cpp
   FileLogDestination.cpp
   FilePath.cpp
   Hash.cpp
   Logger.cpp
   ReaderWriterMutex.cpp
   StderrLogDestination.cpp
   json/Json.cpp
   system/Crypto.cpp
   system/encryption/EncryptionVersion.cpp
   system/encryption/EncryptionConfiguration.cpp
)

set (SHARED_INCLUDE_DIRS
   include
   ${BOOST_INCLUDEDIR}
   ${EXT_SOURCE_DIR})

# UNIX specific
if (UNIX)

   set (SHARED_CORE_SOURCE_FILES
      ${SHARED_CORE_SOURCE_FILES}
      system/PosixSystem.cpp
      system/SyslogDestination.cpp
      system/User.cpp
   )

   find_library(PTHREAD_LIBRARIES pthread)
   set (SHARED_CORE_SYSTEM_LIBS
      ${PTHREAD_LIBRARIES})

else()

   set(SHARED_INCLUDE_DIRS
      ${SHARED_INCLUDE_DIRS}
      ${EXT_SOURCE_DIR}
   )

   set(SHARED_CORE_SOURCE_FILES
      ${SHARED_CORE_SOURCE_FILES}
      system/Win32StringUtils.cpp
      system/Win32User.cpp
   )

endif()

configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.in
               ${CMAKE_CURRENT_BINARY_DIR}/config.h)

include_directories(
   ${SHARED_INCLUDE_DIRS}
   ${TESTS_INCLUDE_DIR}
   ${CMAKE_CURRENT_BINARY_DIR}
)

add_library(rstudio-shared-core STATIC ${SHARED_CORE_HEADER_FILES} ${SHARED_CORE_SOURCE_FILES})
define_source_file_names(rstudio-shared-core)

target_link_libraries(rstudio-shared-core
   ${Boost_LIBRARIES}
   ${SHARED_CORE_SYSTEM_LIBS}
   gsl::gsl-lite-v1
   rstudio-rapidjson)

# Define executable for running unit tests
if (RSTUDIO_UNIT_TESTS_ENABLED)
   # Use gtest from vendor directory
   set(GTEST_SOURCE_DIR ${CMAKE_SOURCE_DIR}/src/cpp/tests/cpp/tests/vendor/gtest)
   include_directories(${GTEST_SOURCE_DIR}/include)
   
   # Add Google Test if not already added
   if(NOT TARGET gtest)
      add_subdirectory(${GTEST_SOURCE_DIR} gtest-build EXCLUDE_FROM_ALL)
   endif()

   file(GLOB_RECURSE SHARED_CORE_TEST_FILES "*Tests.cpp")

   add_executable(rstudio-shared-core-tests
      TestMain.cpp
      ${SHARED_CORE_TEST_FILES}
      ${SHARED_CORE_HEADER_FILES}
   )

   target_include_directories(rstudio-shared-core-tests PRIVATE
      ${CMAKE_CURRENT_SOURCE_DIR}/../tests/cpp/tests/vendor
   )

   target_link_libraries(rstudio-shared-core-tests
      rstudio-shared-core
      rstudio-core
      ${Boost_LIBRARIES}
      gtest
   )

   if(NOT WIN32)
      target_link_libraries(rstudio-shared-core-tests pthread)
   endif()
   
   enable_testing()
   include(GoogleTest)
   gtest_discover_tests(rstudio-shared-core-tests)
endif()
