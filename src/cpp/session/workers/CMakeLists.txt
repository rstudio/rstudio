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

project (SESSION_WORKERS)

# include files
file(GLOB_RECURSE SESSION_WORKERS_HEADER_FILES "*.h*")



# source files
set(SESSION_WORKERS_SOURCE_FILES
   SessionWebRequestWorker.cpp
)


# include directories
include_directories(
   ${CORE_SOURCE_DIR}/include
   ${SESSION_SOURCE_DIR}/include/session/worker_safe
)

# define library
add_library(rstudio-session-workers STATIC
   ${SESSION_WORKERS_SOURCE_FILES}
   ${SESSION_WORKERS_HEADER_FILES})

# link dependencies
target_link_libraries(rstudio-session-workers
   rstudio-core
)
