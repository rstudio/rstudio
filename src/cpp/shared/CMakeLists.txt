#
# CMakeLists.txt
#
# Copyright (C) 2009-19 by RStudio, Inc.
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

project(SHARED)

# include files
file(GLOB_RECURSE SHARED_HEADER_FILES "*.h*")

# source files
set (SHARED_SOURCE_FILES
   Error.cpp
   FileLogDestination.cpp
   FilePath.cpp
   Json.cpp
   Logger.cpp
   SafeConvert.cpp
   StderrDestination.cpp
   SyslogDestination.cpp
   User.cpp
)

set (SHARED_INCLUDE_DIRS
   include
   ${BOOST_INCLUDEDIR}
)

include_directories(${SHARED_INCLUDE_DIRS})

add_library(rstudio-shared STATIC ${SHARED_SOURCE_FILES} ${SHARED_HEADER_FILES})

target_link_libraries(rstudio-shared
   ${Boost_LIBRARIES})