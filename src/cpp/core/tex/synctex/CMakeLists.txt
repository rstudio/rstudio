#
# CMakeLists.txt
#
# Copyright (C) 2009-11 by RStudio, Inc.
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

project (SYNCTEX)

# include files
file(GLOB_RECURSE SYNCTEX_HEADER_FILES "*.h*")

# source files
set(SYNCTEX_SOURCE_FILES
   synctex_parser.c
   synctex_parser_utils.c
)

# include directories
set(SYNCTEX_INCLUDE_DIRS ${CMAKE_CURRENT_SOURCE_DIR})
if(WIN32)
    set(SYNCTEX_INCLUDE_DIRS ${SYNCTEX_INCLUDE_DIRS}
                             "${CMAKE_CURRENT_SOURCE_DIR}/../../zlib")
endif()
include_directories(${SYNCTEX_INCLUDE_DIRS})

# define library
add_library(rstudio-core-synctex STATIC
   ${SYNCTEX_SOURCE_FILES}
   ${SYNCTEX_HEADER_FILES})

# link dependencies
target_link_libraries(rstudio-core-synctex

)

