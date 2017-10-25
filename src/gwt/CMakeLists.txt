#
# CMakeLists.txt
#
# Copyright (C) 2009-17 by RStudio, Inc.
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

# check for externals
SET(GWT_LIBS "/opt/rstudio-tools/gwt/lib")
if(NOT EXISTS GWT_LIBS)
   SET(GWT_LIBS "${CMAKE_CURRENT_SOURCE_DIR}/lib")
endif()

# define output dirs
set(GWT_OUTPUT_DIR "${CMAKE_CURRENT_BINARY_DIR}/gwt")
set(GWT_BIN_DIR "${GWT_OUTPUT_DIR}/bin")
set(GWT_WWW_DIR "${GWT_OUTPUT_DIR}/www")
set(GWT_EXTRAS "${GWT_OUTPUT_DIR}/extras")

# invoke ant to build
add_custom_target(gwt_build ALL)
add_custom_command(
   TARGET gwt_build
   WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
   COMMAND ant -Dbuild.dir=${GWT_BIN_DIR}
               -Dwww.dir=${GWT_WWW_DIR}
               -Dextras.dir=${GWT_EXTRAS_DIR})

# installation rules
install(DIRECTORY "$GWT_WWW_DIR" DESTINATION ${RSTUDIO_INSTALL_SUPPORTING})
install(DIRECTORY "$GWT_EXTRAS_DIR/rstudio/symbolMaps/"
        DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/www-symbolmaps)
