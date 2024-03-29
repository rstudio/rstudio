#
# CMakeLists.txt
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

project (POSTBACK)

# include files
file(GLOB_RECURSE POSTBACK_HEADER_FILES "*.h*")

# source files
set(POSTBACK_SOURCE_FILES
   PostbackMain.cpp
   PostbackOptions.cpp
)

# set include directories
include_directories(
   ${CORE_SOURCE_DIR}/include
   ${SESSION_SOURCE_DIR}/include
)

# define executable
add_stripped_executable(rpostback ${POSTBACK_SOURCE_FILES} ${POSTBACK_HEADER_FILES})

# set link dependencies
target_link_libraries(rpostback
   rstudio-core
   ${CRASHPAD_LIBRARIES}
)

if (RSTUDIO_SERVER)
    target_link_libraries(rpostback
       rstudio-server-core
    )
endif()

# configure postback scripts
set(POSTBACK_SCRIPT_DIR ${CMAKE_CURRENT_BINARY_DIR}/postback)
file(MAKE_DIRECTORY ${POSTBACK_SCRIPT_DIR})
configure_file(askpass-passthrough ${POSTBACK_SCRIPT_DIR}/askpass-passthrough)
configure_file(rpostback-askpass   ${POSTBACK_SCRIPT_DIR}/rpostback-askpass)
configure_file(rpostback-browser   ${POSTBACK_SCRIPT_DIR}/rpostback-browser)
configure_file(rpostback-editfile  ${POSTBACK_SCRIPT_DIR}/rpostback-editfile)
configure_file(rpostback-gitssh    ${POSTBACK_SCRIPT_DIR}/rpostback-gitssh)
configure_file(rpostback-pdfviewer ${POSTBACK_SCRIPT_DIR}/rpostback-pdfviewer)

# set RPOSTBACK_EXECUTABLE_PATH for rdesktop-dev
if(WIN32)
    set(RPOSTBACK_EXECUTABLE_PATH "${CMAKE_CURRENT_BINARY_DIR}/rpostback.exe" CACHE INTERNAL "")
else()
    set(RPOSTBACK_EXECUTABLE_PATH "${CMAKE_CURRENT_BINARY_DIR}/rpostback" CACHE INTERNAL "")
endif()

# installation rules
if(NOT RSTUDIO_SESSION_WIN32)
   install(TARGETS rpostback DESTINATION ${RSTUDIO_INSTALL_BIN})
   file(GLOB POSTBACK_SCRIPTS "rpostback-*")
   set(POSTBACK_SCRIPTS ${POSTBACK_SCRIPTS} "askpass-passthrough")
   install(PROGRAMS ${POSTBACK_SCRIPTS}
           DESTINATION ${RSTUDIO_INSTALL_BIN}/postback)
endif()

