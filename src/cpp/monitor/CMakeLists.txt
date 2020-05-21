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

project (MONITOR)

# include files
file(GLOB_RECURSE MONITOR_HEADER_FILES "*.h*")

# source files
set (MONITOR_SOURCE_FILES
   audit/ConsoleAction.cpp
   events/Event.cpp
   metrics/Metric.cpp
   MonitorClient.cpp
   MonitorClientOverlay.cpp
)


# include directories
include_directories(
   include
   ${CMAKE_CURRENT_BINARY_DIR}
   ${EXT_SOURCE_DIR}
   ${CORE_SOURCE_DIR}/include
   ${SERVER_CORE_SOURCE_DIR}/include
   ${SHARED_CORE_SOURCE_DIR}/include
)

# define library
add_library(rstudio-monitor STATIC ${MONITOR_SOURCE_FILES}
                                   ${MONITOR_HEADER_FILES})
define_source_file_names(rstudio-monitor)

# link dependencies
target_link_libraries(rstudio-monitor
   rstudio-core
)

