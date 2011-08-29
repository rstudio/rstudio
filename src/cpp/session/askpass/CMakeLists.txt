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

project(ASKPASS)

add_definitions(-DQT_NO_CAST_FROM_ASCII -DQT_NO_CAST_TO_ASCII)

# include Qt4
find_package(Qt4 4.7.0 COMPONENTS QtCore QtGui QtNetwork QtWebKit REQUIRED)
include(${QT_USE_FILE})

# include files
file(GLOB_RECURSE ASKPASS_HEADER_FILES "*.h*")

set(MOC_ASKPASS_HEADER_FILES ${ASKPASS_HEADER_FILES})
# list(REMOVE_ITEM MOC_ASKPASS_HEADER_FILES
#   ${CMAKE_CURRENT_SOURCE_DIR}/3rdparty/qtsingleapplication/qtlockedfile.h
#   ${CMAKE_CURRENT_SOURCE_DIR}/DesktopDetectRHome.hpp
#   ${CMAKE_CURRENT_SOURCE_DIR}/DesktopOptions.hpp
#   ${CMAKE_CURRENT_SOURCE_DIR}/DesktopRVersion.hpp
#   ${CMAKE_CURRENT_SOURCE_DIR}/DesktopNetworkProxyFactory.hpp
#   ${CMAKE_CURRENT_SOURCE_DIR}/DesktopUtils.hpp
#   ${CMAKE_CURRENT_SOURCE_DIR}/config.h.in
# )

configure_file (${CMAKE_CURRENT_SOURCE_DIR}/config.h.in
                ${CMAKE_CURRENT_BINARY_DIR}/config.h)

# source files
set(ASKPASS_SOURCE_FILES
  AskPassMain.cpp
)

# include directories
include_directories(
  include
  ${Boost_INCLUDE_DIRS}
  ${CORE_SOURCE_DIR}/include
  ${CMAKE_CURRENT_BINARY_DIR}
)

# ensure moc files are generated
qt4_wrap_cpp(MOC_ASKPASS_SOURCE_FILES ${MOC_ASKPASS_HEADER_FILES})
# qt4_wrap_ui(ASKPASS_UI_SOURCES
#    DesktopAboutDialog.ui
#    DesktopChooseRHome.ui
#    DesktopInputDialog.ui
#    DesktopUpdateAvailableDialog.ui
# )
# 
# set(ASKPASS_RESOURCES_FILES askpass.qrc)
# qt4_add_resources(ASKPASS_RESOURCES_SOURCES ${ASKPASS_RESOURCES_FILES})

if(WIN32)

   # configure rstudio.rc
   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/askpass.rc.in
                   ${CMAKE_CURRENT_BINARY_DIR}/askpass.rc)

   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/AskPass.ico
                   ${CMAKE_CURRENT_BINARY_DIR}/AskPass.ico COPYONLY)

   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/askpass.exe.manifest
                   ${CMAKE_CURRENT_BINARY_DIR}/askpass.exe.manifest COPYONLY)

   if(MINGW)
      set(LINK_FLAGS -Wl,-subsystem,windows -lversion)
      add_custom_command(
         OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/res.o"
         COMMAND windres.exe
            -I "."
            -i "askpass.rc"
            -o "${CMAKE_CURRENT_BINARY_DIR}/res.o"
            -Ocoff
         DEPENDS
            "${CMAKE_CURRENT_BINARY_DIR}/askpass.rc"
            "${CMAKE_CURRENT_SOURCE_DIR}/askpass.exe.manifest"
            "${CMAKE_CURRENT_SOURCE_DIR}/AskPass.ico")
      set(ASKPASS_SOURCE_FILES
         ${ASKPASS_SOURCE_FILES}
         "${CMAKE_CURRENT_BINARY_DIR}/res.o")
   endif(MINGW)
elseif(APPLE)
   find_library(APPLICATION_SERVICES_LIBRARY NAMES ApplicationServices)
   set(LINK_FLAGS ${APPLICATION_SERVICES_LIBRARY})
endif(WIN32)


# determine whether we should bundle Qt. we pretty much always want to
# bundle it unless we are building on linux and NOT linking against the
# qtsdk -- in that case we are linking against system versions of the
# qt libraries which should therefore not be bundled
if(APPLE OR WIN32 OR
   RSTUDIO_PACKAGE_BUILD OR
   (${QT_LIBRARY_DIR} MATCHES ".*qtsdk.*"))
  set(RSTUDIO_BUNDLE_QT TRUE)
endif()

# define executable (Windows & Linux)
if(NOT APPLE)

   add_executable(askpass
      ${ASKPASS_SOURCE_FILES}
      ${MOC_ASKPASS_SOURCE_FILES}
      ${ASKPASS_RESOURCES_SOURCES}
      ${ASKPASS_UI_SOURCES}
   )

   # add rpath for linux so we can find qt libraries in our bin dir
   if(UNIX AND RSTUDIO_BUNDLE_QT)
      set_target_properties(askpass PROPERTIES
                            INSTALL_RPATH \$ORIGIN)
   endif()

   # set link dependencies
   target_link_libraries(askpass
      ${QT_LIBRARIES}
      rstudio-core
      ${LINK_FLAGS}
   )

# for OSX we create a bundle
else()

   # configure Info.plist
   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/Info.plist.in
                   ${CMAKE_CURRENT_BINARY_DIR}/Info.plist)

   # collect list of icns files
   set(ICNS_FILES ${CMAKE_CURRENT_SOURCE_DIR}/resources/icns/AskPass.icns)

   # set our icns as the bundle icon
   set(MACOSX_BUNDLE_ICON_FILE AskPass.icns)
   set_source_files_properties(${ICNS_FILES}
      PROPERTIES MACOSX_PACKAGE_LOCATION Resources)

   # define bundle name and executable
   set(MACOSX_BUNDLE_BUNDLE_NAME "AskPass")

   add_executable(AskPass MACOSX_BUNDLE
      ${ASKPASS_SOURCE_FILES}
      ${MOC_ASKPASS_SOURCE_FILES}
      ${ASKPASS_RESOURCES_SOURCES}
      ${ASKPASS_UI_SOURCES}
      ${ICNS_FILES})

   target_link_libraries(AskPass
      ${QT_LIBRARIES}
      rstudio-core
      ${LINK_FLAGS})

endif()

# install target (OSX install goes into the bundle)
if(APPLE)
  set_target_properties(RStudio PROPERTIES
     MACOSX_BUNDLE_INFO_PLIST ${CMAKE_CURRENT_BINARY_DIR}/Info.plist)
  install(TARGETS AskPass BUNDLE DESTINATION .)
else()
  install(TARGETS askpass DESTINATION ${RSTUDIO_INSTALL_BIN})
endif()

# bundle qt dependencies if this is a package build
if(RSTUDIO_BUNDLE_QT)
   # install qt conf
   if(APPLE)
      set(QTCONF_DEST_DIR ${RSTUDIO_INSTALL_SUPPORTING})
      set(QTPLUGINS_DEST_DIR RStudio.app/Contents)
   else()
      set(QTCONF_DEST_DIR ${RSTUDIO_INSTALL_BIN})
      set(QTPLUGINS_DEST_DIR ${RSTUDIO_INSTALL_BIN})
   endif()
   install(FILES ${CMAKE_CURRENT_SOURCE_DIR}/qt.conf
           DESTINATION ${QTCONF_DEST_DIR})

   # copy qt plugins
   set(QT_IMAGEFORMATS_DIR "${QT_PLUGINS_DIR}/imageformats")
   install(DIRECTORY "${QT_PLUGINS_DIR}/imageformats"
           DESTINATION ${QTPLUGINS_DEST_DIR}/plugins
           PATTERN "*.a" EXCLUDE
           PATTERN "*d4.dll" EXCLUDE)

   # fixup bundle on osx
   if(APPLE)
      set(APPS "\${CMAKE_INSTALL_PREFIX}/RStudio.app")
      set(DIRS ${QT_LIBRARY_DIRS})
      INSTALL(CODE "
          file(GLOB_RECURSE QTPLUGINS
            \"\${CMAKE_INSTALL_PREFIX}/${QTPLUGINS_DEST_DIR}/plugins/*${CMAKE_SHARED_LIBRARY_SUFFIX}\")
          include(BundleUtilities)
          file(REMOVE \"\${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/MacOS/rsession\")
          fixup_bundle(\"${APPS}\" \"\${QTPLUGINS}\" \"${DIRS}\")
          ")

   # copy qt shared objects directly to bin on unix (fixed up rpath above)
   elseif(UNIX)
      # compute path to release libs
      get_filename_component(QT_RELEASE_LIBRARY_DIR "${QT_LIBRARY_DIR}/../../lib" REALPATH)

      # install dependent libraries
      set(QT_FULL_VERSION "${QT_VERSION_MAJOR}.${QT_VERSION_MINOR}.${QT_VERSION_PATCH}")
      install(PROGRAMS ${QT_RELEASE_LIBRARY_DIR}/libQtCore.so.4
                       ${QT_RELEASE_LIBRARY_DIR}/libQtCore.so.${QT_FULL_VERSION}
                       ${QT_RELEASE_LIBRARY_DIR}/libQtDBus.so.4
                       ${QT_RELEASE_LIBRARY_DIR}/libQtDBus.so.${QT_FULL_VERSION}
                       ${QT_RELEASE_LIBRARY_DIR}/libQtGui.so.4
                       ${QT_RELEASE_LIBRARY_DIR}/libQtGui.so.${QT_FULL_VERSION}
                       ${QT_RELEASE_LIBRARY_DIR}/libQtNetwork.so.4
                       ${QT_RELEASE_LIBRARY_DIR}/libQtNetwork.so.${QT_FULL_VERSION}
                       ${QT_RELEASE_LIBRARY_DIR}/libQtWebKit.so.4
                       ${QT_RELEASE_LIBRARY_DIR}/libQtWebKit.so.${QT_FULL_VERSION}
                       ${QT_RELEASE_LIBRARY_DIR}/libQtSvg.so.4
                       ${QT_RELEASE_LIBRARY_DIR}/libQtSvg.so.${QT_FULL_VERSION}
                       ${QT_RELEASE_LIBRARY_DIR}/libQtXml.so.4
                       ${QT_RELEASE_LIBRARY_DIR}/libQtXml.so.${QT_FULL_VERSION}
                       ${QT_RELEASE_LIBRARY_DIR}/libQtXmlPatterns.so.4
                       ${QT_RELEASE_LIBRARY_DIR}/libQtXmlPatterns.so.${QT_FULL_VERSION}
                       ${QT_RELEASE_LIBRARY_DIR}/libphonon.so.4
                       ${QT_RELEASE_LIBRARY_DIR}/libphonon.so.4.4.0
              DESTINATION ${RSTUDIO_INSTALL_BIN})

   # copy qt dlls directly to bin on win32
   elseif(WIN32)
      install(PROGRAMS ${QT_BINARY_DIR}/QtCore4.dll
                       ${QT_BINARY_DIR}/QtGui4.dll
                       ${QT_BINARY_DIR}/QtNetwork4.dll
                       ${QT_BINARY_DIR}/QtWebKit4.dll
                       ${QT_BINARY_DIR}/QtSvg4.dll
                       ${QT_BINARY_DIR}/QtXml4.dll
                       ${QT_BINARY_DIR}/phonon4.dll
                       ${QT_BINARY_DIR}/mingwm10.dll
                       ${QT_BINARY_DIR}/libgcc_s_dw2-1.dll
              DESTINATION ${RSTUDIO_INSTALL_BIN})
   endif()
endif(RSTUDIO_BUNDLE_QT)
