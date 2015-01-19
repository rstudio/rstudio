#
# CMakeLists.txt
#
# Copyright (C) 2009-12 by RStudio, Inc.
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

project (TESTS)

file(GLOB_RECURSE TESTS_SOURCE_FILES "${CMAKE_SOURCE_DIR}/*Tests.cpp")

include_directories(
   include
   ${LIBR_SOURCE_DIR}/include
   ${CORE_SOURCE_DIR}/include
   ${MONITOR_SOURCE_DIR}/include
   ${R_SOURCE_DIR}/include
   ${CMAKE_CURRENT_BINARY_DIR}
   ${Boost_INCLUDE_DIRS}
)

set(TESTS_HEADER_FILES
   tests/TestRunner.hpp
)

add_executable(test-runner
   tests/TestMain.cpp
   ${TESTS_SOURCE_FILES}
   ${TESTS_HEADER_FILES}
)

target_link_libraries(test-runner
   rstudio-core
   rstudio-core-hunspell
   rstudio-core-synctex
   ${RSTUDIO_CORE_ZLIB}
   rstudio-monitor
   rstudio-r
   rstudio-session-workers
   ${SESSION_SYSTEM_LIBRARIES}
   ${LIBR_LIBRARIES}
   ${CMAKE_DL_LIBS}
   ${Boost_LIBRARIES}
)

add_custom_command(TARGET test-runner
   POST_BUILD
   COMMAND "${CMAKE_CURRENT_BINARY_DIR}/test-runner"
)
