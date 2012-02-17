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

project(RSPDFLATEX)

# include files
file(GLOB_RECURSE RSPDFLATEX_HEADER_FILES "*.h*")

# set include directories
include_directories(
   ${Boost_INCLUDE_DIRS}
   ${CORE_SOURCE_DIR}/include
)

set(RSPDFLATEX_SOURCE_FILES
  RsPdfLatexMain.cpp
)


# configure rspdflatex.rc
if(WIN32)
   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/rspdflatex.rc.in
                   ${CMAKE_CURRENT_BINARY_DIR}/rspdflatex.rc)


   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/rspdflatex.exe.manifest
                   ${CMAKE_CURRENT_BINARY_DIR}/rspdflatex.exe.manifest COPYONLY)

   set(LINK_FLAGS -Wl,-subsystem,windows -lversion)
   add_custom_command(
      OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/res.o"
      COMMAND windres.exe
         -I "."
         -i "rspdflatex.rc"
         -o "${CMAKE_CURRENT_BINARY_DIR}/res.o"
         -Ocoff
      DEPENDS
         "${CMAKE_CURRENT_BINARY_DIR}/rspdflatex.rc"
         "${CMAKE_CURRENT_SOURCE_DIR}/rspdflatex.exe.manifest")
   set(RSPDFLATEX_SOURCE_FILES
      ${RSPDFLATEX_SOURCE_FILES}
      "${CMAKE_CURRENT_BINARY_DIR}/res.o")
endif()

add_executable(rspdflatex
  ${RSPDFLATEX_SOURCE_FILES}
)

# set link dependencies
target_link_libraries(rspdflatex
   rstudio-core
)

# installation rules
if(NOT RSTUDIO_SESSION_WIN64)
   install(TARGETS rspdflatex DESTINATION ${RSTUDIO_INSTALL_BIN})
endif()

