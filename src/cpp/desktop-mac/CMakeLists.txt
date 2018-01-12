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
   ActivationOverlay.mm
   AppDelegate.mm
   DockTileView.mm
   FileDownloader.mm
   GwtCallbacks.mm
   Main.mm
   MainFrameController.mm
   MainFrameMenu.mm
   WebViewWithKeyEquiv.mm
   Options.mm
   SatelliteController.mm
   SecondaryWindowController.mm
   SessionLauncher.mm
   ScriptCommand.mm
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

# add image resources to the bundle
file(GLOB PNG_FILES ${CMAKE_CURRENT_SOURCE_DIR}/resources/png/*.png)
set_source_files_properties(${PNG_FILES}
   PROPERTIES MACOSX_PACKAGE_LOCATION Resources)

# define bundle name and executable
set(MACOSX_BUNDLE_BUNDLE_NAME "RStudio")

add_executable(RStudio MACOSX_BUNDLE
   ${DESKTOP_MAC_SOURCE_FILES}
   ${DESKTOP_MAC_HEADER_FILES}
   ${ICNS_FILES}
   ${PNG_FILES})


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

install(FILES ${CMAKE_CURRENT_SOURCE_DIR}/RStudio.sdef
                 DESTINATION ${RSTUDIO_INSTALL_SUPPORTING})

find_program(IBTOOL ibtool HINTS "/usr/bin" "${OSX_DEVELOPER_ROOT}/usr/bin")
if (${IBTOOL} STREQUAL "IBTOOL-NOTFOUND")
   message(SEND_ERROR "\
ibtool can not be found and is needed to compile the .xib files. It should have been \
installed with the Apple developer tools. The default system paths were searched in \
addition to ${OSX_DEVELOPER_ROOT}/usr/bin")
else()
   message(STATUS "Detected MacOS ibtool: ${IBTOOL}")
endif()

# Make sure the 'Resources' Directory is correctly created before we build
add_custom_command (TARGET RStudio PRE_BUILD
COMMAND mkdir -p ${CMAKE_CURRENT_BINARY_DIR}/\${CONFIGURATION}/RStudio.app/Contents/Resources)
                      
# Compile the .xib files with destination being app package
foreach(xib ${DESKTOP_MAC_XIBS})
   add_custom_command (TARGET RStudio POST_BUILD
                       COMMAND ${IBTOOL} --errors --warnings --notices --output-format human-readable-text
                               --compile ${CMAKE_CURRENT_BINARY_DIR}/\${CONFIGURATION}/RStudio.app/Contents/Resources/${xib}.nib
                               ${CMAKE_CURRENT_SOURCE_DIR}/${xib}.xib
                       COMMENT "Compiling ${CMAKE_CURRENT_SOURCE_DIR}/${xib}.xib"
   )
endforeach()

# enable developer extras
exec_program(defaults
             ARGS write org.rstudio.RStudio WebKitDeveloperExtras -bool true
             OUTPUT_VARIABLE DEV_EXTRAS)


# we build against OpenSSL, but Apple no longer bundles OpenSSL, so, when doing
# a package build, copy it into our install folder and fix the runtime path so
# that we can load from there.
if(RSTUDIO_PACKAGE_BUILD)
   add_custom_command (TARGET RStudio PRE_BUILD
                       COMMAND mkdir -p ${RSTUDIO_INSTALL_BIN}/openssl)
   find_package(OpenSSL REQUIRED QUIET)

   # copy the libssl and libcrypto libraries to the install folder
   foreach(lib ${OPENSSL_CRYPTO_LIBRARY} ${OPENSSL_SSL_LIBRARY})
      # extract the parent directory and actual filename
      get_filename_component(LIB_DIR ${lib} PATH)
      execute_process(COMMAND readlink ${lib} OUTPUT_VARIABLE LIB_FILE OUTPUT_STRIP_TRAILING_WHITESPACE)

      # create a writable copy of the libraries we will later modify
      file(COPY ${LIB_DIR}/${LIB_FILE} 
           DESTINATION ${CMAKE_CURRENT_BINARY_DIR}
           FILE_PERMISSIONS OWNER_WRITE OWNER_READ)

      # install it read-only
      install(FILES ${CMAKE_CURRENT_BINARY_DIR}/${LIB_FILE}
              PERMISSIONS OWNER_READ GROUP_READ WORLD_READ
              DESTINATION ${RSTUDIO_INSTALL_BIN}/openssl/)

      # save final destination filename
      if(lib STREQUAL ${OPENSSL_CRYPTO_LIBRARY})
         set(LIBCRYPTO ${CMAKE_CURRENT_BINARY_DIR}/${LIB_FILE})
      else()
         set(LIBSSL ${CMAKE_CURRENT_BINARY_DIR}/${LIB_FILE})
      endif()

      # modify the RStudio binaries to look for the installed copies instead of
      # the system copies we linked against
      add_custom_command (TARGET RStudio POST_BUILD
         COMMAND install_name_tool -change ${LIB_DIR}/${LIB_FILE} @executable_path/openssl/${LIB_FILE} ${RSTUDIO_INSTALL_BIN}/RStudio)
   endforeach()
endif()
