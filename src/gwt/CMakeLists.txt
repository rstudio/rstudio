#
# CMakeLists.txt
#
# Copyright (C) 2009-12 by RStudio, Inc.
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

cmake_minimum_required(VERSION 2.6)
project (RSTUDIO_GWT)

# build desktop variation only if appropriate
if (NOT RSTUDIO_SERVER)
   set(ANT_ARGS "desktop")
endif()

# invoke ant to build
add_custom_target(gwt_build ALL)
add_custom_command(
   TARGET gwt_build
   WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
   COMMAND ant
   ARGS ${ANT_ARGS})

# installation rules
install(DIRECTORY www DESTINATION ${RSTUDIO_INSTALL_SUPPORTING})
