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

project(CORE_HUNSPELL)

configure_file (${CMAKE_CURRENT_SOURCE_DIR}/config.h.in
                ${CMAKE_CURRENT_BINARY_DIR}/config.h)


# disable warnings
add_definitions(-w)

# include files
file(GLOB_RECURSE CORE_HUNSPELL_HEADER_FILES "*.h*")


# source files
set(CORE_HUNSPELL_SOURCE_FILES
   affentry.cxx
   affixmgr.cxx
   csutil.cxx
   dictmgr.cxx
   hashmgr.cxx
   suggestmgr.cxx
   license.myspell
   license.hunspell
   phonet.cxx
   filemgr.cxx
   hunzip.cxx
   hunspell.cxx
   replist.cxx
)


# include directories
include_directories(
   ${CMAKE_CURRENT_BINARY_DIR}
)

# define library
add_library(rstudio-core-hunspell STATIC
   ${CORE_HUNSPELL_SOURCE_FILES}
   ${CORE_HUNSPELL_HEADER_FILES})

# link dependencies
target_link_libraries(rstudio-core-hunspell

)
