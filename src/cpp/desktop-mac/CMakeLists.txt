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

project(DESKTOP_MAC)

# libraries
find_library(APPKIT_LIBRARY NAMES AppKit)
find_library(OPENGL_LIBRARY NAMES OpenGL)
find_library(WEBKIT_LIBRARY NAMES WebKit)

# include files
file(GLOB_RECURSE DESKTOP_MAC_HEADER_FILES "*.h*")

configure_file (${CMAKE_CURRENT_SOURCE_DIR}/desktop-config.h.in
                ${CMAKE_CURRENT_BINARY_DIR}/desktop-config.h)

# source files
set(DESKTOP_MAC_SOURCE_FILES
   AppDelegate.mm
   GwtCallbacks.mm
   Main.mm
   Options.mm
   Utils.mm
   WebViewController.mm
   
)

# include directories
include_directories(
  include
  ${CORE_SOURCE_DIR}/include
  ${CMAKE_CURRENT_BINARY_DIR}
)

# configure Info.plist
configure_file (${CMAKE_CURRENT_SOURCE_DIR}/Info.plist.in
                ${CMAKE_CURRENT_BINARY_DIR}/Info.plist)

# collect list of icns files
file(GLOB ICNS_FILES ${CMAKE_CURRENT_SOURCE_DIR}/resources/icns/*.icns)

# set our icns as the bundle icon
set(MACOSX_BUNDLE_ICON_FILE RStudio.icns)
set_source_files_properties(${ICNS_FILES}
   PROPERTIES MACOSX_PACKAGE_LOCATION Resources)

# define bundle name and executable
set(MACOSX_BUNDLE_BUNDLE_NAME "RStudio")

add_executable(RStudio MACOSX_BUNDLE
   ${DESKTOP_MAC_SOURCE_FILES}
   ${DESKTOP_MAC_HEADER_FILES}
   ${ICNS_FILES})

target_link_libraries(RStudio
   rstudio-core
   ${APPKIT_LIBRARY}
   ${OPENGL_LIBRARY}
   ${WEBKIT_LIBRARY})

# install target (OSX install goes into the bundle)
set_target_properties(RStudio PROPERTIES
  MACOSX_BUNDLE_INFO_PLIST ${CMAKE_CURRENT_BINARY_DIR}/Info.plist)
install(TARGETS RStudio BUNDLE DESTINATION .)

# install mac-terminal script
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/mac-terminal.in
               ${CMAKE_CURRENT_BINARY_DIR}/mac-terminal)
install(PROGRAMS ${CMAKE_CURRENT_BINARY_DIR}/mac-terminal
                 DESTINATION ${RSTUDIO_INSTALL_BIN})
                 
# enable developer extras 
exec_program(defaults 
             ARGS write org.rstudio.RStudio WebKitDeveloperExtras -bool true
             OUTPUT_VARIABLE DEV_EXTRAS)




