#
# CMakeLists.txt
#
# Copyright (C) 2019 by RStudio, Inc.
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

project(SHARED_CORE)

# include files
file(GLOB_RECURSE SHARED_CORE_HEADER_FILES "*.h*")

# source files
set (SHARED_CORE_SOURCE_FILES
   Error.cpp
   FileLogDestination.cpp
   FilePath.cpp
   Logger.cpp
   StderrDestination.cpp
   SyslogDestination.cpp
   json/Json.cpp
)

# UNIX specific
if (UNIX)

   set (SHARED_CORE_SOURCE_FILES
      ${SHARED_CORE_SOURCE_FILES}
      system/User.cpp
   )

endif()

set (SHARED_INCLUDE_DIRS
   include
   ${BOOST_INCLUDEDIR}
)

include_directories(${SHARED_INCLUDE_DIRS})

add_library(rstudio-shared-core STATIC ${SHARED_CORE_HEADER_FILES} ${SHARED_CORE_SOURCE_FILES})

target_link_libraries(rstudio-shared-core
   ${Boost_LIBRARIES})