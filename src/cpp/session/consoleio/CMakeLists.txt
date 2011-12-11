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

project(CONSOLEIO)

# include files
file(GLOB_RECURSE CONSOLEIO_HEADER_FILES "*.h*")

# set include directories
include_directories(
   ${Boost_INCLUDE_DIRS}
   ${CORE_SOURCE_DIR}/include
)

set(CONSOLEIO_SOURCE_FILES
  ConsoleIOMain.cpp
)

add_definitions(-DWINVER=0x501
                -D_WIN32_WINNT=0x0501
                -D_WIN32_IE=0x600
                -DWIN32_LEAN_AND_MEAN)
set(LINK_FLAGS -Wl,-subsystem,console -lversion)

add_executable(consoleio
  ${CONSOLEIO_SOURCE_FILES}
)

# set link dependencies
target_link_libraries(consoleio
   rstudio-core
)
