#
# CMakeLists.txt
#
# Copyright (C) 2009-19 by RStudio, Inc.
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

cmake_minimum_required(VERSION 3.4.3)
project (RSTUDIO_GWT)

# check for externals
set(GWT_LIB_DIR "/opt/rstudio-tools/dependencies/common/gwt/lib")
if(NOT EXISTS "${GWT_LIB_DIR}")
   set(GWT_LIB_DIR "lib")
endif()

# define output dirs
if("${GWT_BIN_DIR}" STREQUAL "")
   set(GWT_BIN_DIR "bin")
endif()
if("${GWT_WWW_DIR}" STREQUAL "")
   set(GWT_WWW_DIR "www")
endif()
if("${GWT_EXTRAS_DIR}" STREQUAL "")
   set(GWT_EXTRAS_DIR "extras")
endif()

# invoke ant to build
set(GWT_BUILD Yes)
if(DEFINED ENV{GWT_BUILD})
   set(GWT_BUILD $ENV{GWT_BUILD})
endif()

if(GWT_BUILD)
   add_custom_target(gwt_build ALL)
   add_custom_command(
      TARGET gwt_build
      WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}"
      COMMAND ant -Dbuild.dir="${GWT_BIN_DIR}"
                  -Dwww.dir="${GWT_WWW_DIR}"
                  -Dextras.dir="${GWT_EXTRAS_DIR}"
                  -Dlib.dir="${GWT_LIB_DIR}")
endif()

# create test script and copy to binary directory with executable permissions
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/gwt-unit-tests.sh.in
               ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/gwt-unit-tests.sh)
file(COPY ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/gwt-unit-tests.sh
          DESTINATION ${CMAKE_CURRENT_BINARY_DIR}
          FILE_PERMISSIONS OWNER_READ OWNER_WRITE OWNER_EXECUTE GROUP_READ
          GROUP_EXECUTE WORLD_READ WORLD_EXECUTE)

# installation rules
install(DIRECTORY www DESTINATION ${RSTUDIO_INSTALL_SUPPORTING})
install(DIRECTORY "${GWT_WWW_DIR}" DESTINATION ${RSTUDIO_INSTALL_SUPPORTING})
install(DIRECTORY "${GWT_EXTRAS_DIR}/rstudio/symbolMaps/"
        DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/www-symbolmaps)
