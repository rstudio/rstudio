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

project(CORE_HUNSPELL)

configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.in
               ${CMAKE_CURRENT_BINARY_DIR}/config.h)

if(NOT MSVC)
   add_definitions(-w)
endif()
add_definitions(-DHUNSPELL_STATIC)

if(MSVC)
  # disable C4267 warning (typical example is assigning size_t to int)
  # we don't want to try fixing these in this third-party code
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /wd4267")
endif()

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
define_source_file_names(rstudio-core-hunspell)

# link dependencies
target_link_libraries(rstudio-core-hunspell

)
