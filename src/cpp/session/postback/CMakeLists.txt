#
# CMakeLists.txt
#
# Copyright (C) 2009-12 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
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
add_executable(rpostback ${POSTBACK_SOURCE_FILES} ${POSTBACK_HEADER_FILES})

# set link dependencies
target_link_libraries(rpostback
   rstudio-core
)

# configure postback scripts for development mode
set(POSTBACK_SCRIPT_DIR ${CMAKE_CURRENT_BINARY_DIR}/postback)
file(MAKE_DIRECTORY ${POSTBACK_SCRIPT_DIR})
configure_file(rpostback-editfile ${POSTBACK_SCRIPT_DIR}/rpostback-editfile)
configure_file(rpostback-pdfviewer ${POSTBACK_SCRIPT_DIR}/rpostback-pdfviewer)
configure_file(rpostback-gitssh ${POSTBACK_SCRIPT_DIR}/rpostback-gitssh)
configure_file(rpostback-askpass ${POSTBACK_SCRIPT_DIR}/rpostback-askpass)
configure_file(askpass-passthrough ${POSTBACK_SCRIPT_DIR}/askpass-passthrough)

# installation rules
if(NOT RSTUDIO_SESSION_WIN64)
   install(TARGETS rpostback DESTINATION ${RSTUDIO_INSTALL_BIN})
   file(GLOB POSTBACK_SCRIPTS "rpostback-*")
   set(POSTBACK_SCRIPTS ${POSTBACK_SCRIPTS} "askpass-passthrough")
   install(PROGRAMS ${POSTBACK_SCRIPTS}
           DESTINATION ${RSTUDIO_INSTALL_BIN}/postback)
endif()



