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

project(DESKTOP)

# require Qt 4.8
set(QT_VERSION 4.8.0)

# on unix prefer qtsdk installs over system-level libraries. note this
# can be overriden by defining QT_QMAKE_EXECUTABLE when invoking cmake
if(NOT WIN32)
   # prefer rstudio qtsdk install then home qtsdk install
   if(NOT QT_QMAKE_EXECUTABLE)
      set(QMAKE_QT48_SDK "/opt/RStudio-QtSDK/Desktop/Qt/4.8.0/gcc/bin/qmake")
      if(EXISTS ${QMAKE_QT48_SDK})
         set(QT_QMAKE_EXECUTABLE ${QMAKE_QT48_SDK})
      else()
         set(QMAKE_QT48_SDK "$ENV{HOME}/QtSDK/Desktop/Qt/4.8.0/gcc/bin/qmake")
         if(EXISTS ${QMAKE_QT48_SDK})
            set(QT_QMAKE_EXECUTABLE ${QMAKE_QT48_SDK})
         endif()
      endif()
   endif()
endif()

# find and include Qt
add_definitions(-DQT_NO_CAST_FROM_ASCII -DQT_NO_CAST_TO_ASCII)
list(APPEND RSTUDIO_QT_COMPONENTS QtCore QtGui QtNetwork QtWebKit)
if(UNIX AND NOT APPLE)
   list(APPEND RSTUDIO_QT_COMPONENTS QtDBus)
endif()
find_package(Qt4 ${QT_VERSION} COMPONENTS ${RSTUDIO_QT_COMPONENTS} REQUIRED)
include(${QT_USE_FILE})

# configure NOTICE file into build directory so it can be read at dev time
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/../../../NOTICE
               ${CMAKE_CURRENT_BINARY_DIR}/.. COPYONLY)

# include files
file(GLOB_RECURSE DESKTOP_HEADER_FILES "*.h*")
if (NOT WIN32)
  list(REMOVE_ITEM DESKTOP_HEADER_FILES ${CMAKE_CURRENT_SOURCE_DIR}/DesktopChooseRHome.hpp)
endif (NOT WIN32)

set(MOC_DESKTOP_HEADER_FILES ${DESKTOP_HEADER_FILES})
list(REMOVE_ITEM MOC_DESKTOP_HEADER_FILES
  ${CMAKE_CURRENT_SOURCE_DIR}/3rdparty/qtsingleapplication/qtlockedfile.h
  ${CMAKE_CURRENT_SOURCE_DIR}/DesktopDetectRHome.hpp
  ${CMAKE_CURRENT_SOURCE_DIR}/DesktopOptions.hpp
  ${CMAKE_CURRENT_SOURCE_DIR}/DesktopRVersion.hpp
  ${CMAKE_CURRENT_SOURCE_DIR}/DesktopNetworkProxyFactory.hpp
  ${CMAKE_CURRENT_SOURCE_DIR}/DesktopGwtCallbackOwner.hpp
  ${CMAKE_CURRENT_SOURCE_DIR}/DesktopUtils.hpp
  ${CMAKE_CURRENT_SOURCE_DIR}/config.h.in
)

if(WIN32)
   list(REMOVE_ITEM MOC_DESKTOP_HEADER_FILES
     ${CMAKE_CURRENT_SOURCE_DIR}/DesktopPosixApplication.hpp)
endif()
if(WIN32 OR APPLE)
   list(REMOVE_ITEM MOC_DESKTOP_HEADER_FILES
     ${CMAKE_CURRENT_SOURCE_DIR}/synctex/evince/EvinceDaemon.hpp
     ${CMAKE_CURRENT_SOURCE_DIR}/synctex/evince/EvinceWindow.hpp
     ${CMAKE_CURRENT_SOURCE_DIR}/synctex/evince/EvinceSynctex.hpp
   )
endif()
if(NOT WIN32)
   list(REMOVE_ITEM MOC_DESKTOP_HEADER_FILES
     ${CMAKE_CURRENT_SOURCE_DIR}/synctex/sumatra/SumatraSynctex.hpp
   )
endif()

configure_file (${CMAKE_CURRENT_SOURCE_DIR}/config.h.in
                ${CMAKE_CURRENT_BINARY_DIR}/config.h)

# source files
set(DESKTOP_SOURCE_FILES
  DesktopAboutDialog.cpp
  DesktopBrowserWindow.cpp
  DesktopCommandInvoker.cpp
  DesktopDataUriNetworkReply.cpp
  DesktopDownloadHelper.cpp
  DesktopGwtCallback.cpp
  DesktopGwtWindow.cpp
  DesktopInputDialog.cpp
  DesktopMain.cpp
  DesktopMainWindow.cpp
  DesktopMenuCallback.cpp
  DesktopNetworkProxyFactory.cpp
  DesktopOptions.cpp
  DesktopURLDownloader.cpp
  DesktopUtils.cpp
  DesktopWebPage.cpp
  DesktopWebView.cpp
  DesktopWindowTracker.cpp
  DesktopSatelliteWindow.cpp
  DesktopSecondaryWindow.cpp
  DesktopSessionLauncher.cpp
  DesktopSlotBinders.cpp
  DesktopSynctex.cpp
  DesktopNetworkAccessManager.cpp
  DesktopUpdateAvailableDialog.cpp
  DesktopUpdateChecker.cpp
  3rdparty/qtsingleapplication/qtsingleapplication.cpp
  3rdparty/qtsingleapplication/qtlocalpeer.cpp
)

if(WIN32)
  set(DESKTOP_SOURCE_FILES ${DESKTOP_SOURCE_FILES}
    DesktopChooseRHome.cpp
    DesktopWin32ApplicationLaunch.cpp
    DesktopRVersion.cpp
    DesktopWin32DetectRHome.cpp
    synctex/sumatra/SumatraSynctex.cpp
  )
  list(REMOVE_ITEM MOC_DESKTOP_HEADER_FILES
    ${CMAKE_CURRENT_SOURCE_DIR}/DesktopPosixApplication.hpp
  )
else()
  set(DESKTOP_SOURCE_FILES ${DESKTOP_SOURCE_FILES}
    DesktopPosixApplication.cpp
    DesktopPosixApplicationLaunch.cpp
    DesktopPosixDetectRHome.cpp
  )
  if(NOT APPLE)
    set(DESKTOP_SOURCE_FILES ${DESKTOP_SOURCE_FILES}
      synctex/evince/EvinceDaemon.cpp
      synctex/evince/EvinceSynctex.cpp
      synctex/evince/EvinceWindow.cpp
    )
  endif()
endif(WIN32)

# include directories
include_directories(
  include
  ${CORE_SOURCE_DIR}/include
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${CMAKE_CURRENT_BINARY_DIR}
)

# ensure moc files are generated
qt4_wrap_cpp(MOC_DESKTOP_SOURCE_FILES ${MOC_DESKTOP_HEADER_FILES})
qt4_wrap_ui(DESKTOP_UI_SOURCES
   DesktopAboutDialog.ui
   DesktopChooseRHome.ui
   DesktopInputDialog.ui
   DesktopUpdateAvailableDialog.ui
)

set(DESKTOP_RESOURCES_FILES desktop.qrc)
qt4_add_resources(DESKTOP_RESOURCES_SOURCES ${DESKTOP_RESOURCES_FILES})

if(WIN32)

   # configure rstudio.rc
   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/rstudio.rc.in
                   ${CMAKE_CURRENT_BINARY_DIR}/rstudio.rc)

   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/RStudio.ico
                   ${CMAKE_CURRENT_BINARY_DIR}/RStudio.ico COPYONLY)

   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/RProject.ico
                   ${CMAKE_CURRENT_BINARY_DIR}/RProject.ico COPYONLY)

   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/rstudio.exe.manifest
                   ${CMAKE_CURRENT_BINARY_DIR}/rstudio.exe.manifest COPYONLY)

   if(MINGW)
      set(LINK_FLAGS -Wl,-subsystem,windows -lversion)
      add_custom_command(
         OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/res.o"
         COMMAND windres.exe
            -I "."
            -i "rstudio.rc"
            -o "${CMAKE_CURRENT_BINARY_DIR}/res.o"
            -Ocoff
         DEPENDS
            "${CMAKE_CURRENT_BINARY_DIR}/rstudio.rc"
            "${CMAKE_CURRENT_SOURCE_DIR}/rstudio.exe.manifest"
            "${CMAKE_CURRENT_SOURCE_DIR}/RStudio.ico"
            "${CMAKE_CURRENT_SOURCE_DIR}/RProject.ico")
      set(DESKTOP_SOURCE_FILES
         ${DESKTOP_SOURCE_FILES}
         "${CMAKE_CURRENT_BINARY_DIR}/res.o")
   endif(MINGW)

   if(NOT RSTUDIO_SESSION_WIN64)
      add_subdirectory(urlopener)
      add_subdirectory(synctex/rsinverse)
   endif()

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
   (${QT_LIBRARY_DIR} MATCHES ".*QtSDK.*"))
  set(RSTUDIO_BUNDLE_QT TRUE)
endif()

# define executable (Windows & Linux)
if(NOT APPLE)

   add_executable(rstudio
      ${DESKTOP_SOURCE_FILES}
      ${MOC_DESKTOP_SOURCE_FILES}
      ${DESKTOP_RESOURCES_SOURCES}
      ${DESKTOP_UI_SOURCES}
   )

   # add rpath for linux so we can find qt libraries in our bin dir
   if(UNIX AND RSTUDIO_BUNDLE_QT)
      set_target_properties(rstudio PROPERTIES
                            INSTALL_RPATH \$ORIGIN)
   endif()

   # set link dependencies
   target_link_libraries(rstudio
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
   file(GLOB ICNS_FILES ${CMAKE_CURRENT_SOURCE_DIR}/resources/icns/*.icns)

   # set our icns as the bundle icon
   set(MACOSX_BUNDLE_ICON_FILE RStudio.icns)
   set_source_files_properties(${ICNS_FILES}
      PROPERTIES MACOSX_PACKAGE_LOCATION Resources)

   # define bundle name and executable
   set(MACOSX_BUNDLE_BUNDLE_NAME "RStudio")

   add_executable(RStudio MACOSX_BUNDLE
      ${DESKTOP_SOURCE_FILES}
      ${MOC_DESKTOP_SOURCE_FILES}
      ${DESKTOP_RESOURCES_SOURCES}
      ${DESKTOP_UI_SOURCES}
      ${ICNS_FILES})

   target_link_libraries(RStudio
      ${QT_LIBRARIES}
      rstudio-core
      ${LINK_FLAGS})

endif()

# install target (OSX install goes into the bundle)
if(APPLE)
  set_target_properties(RStudio PROPERTIES
     MACOSX_BUNDLE_INFO_PLIST ${CMAKE_CURRENT_BINARY_DIR}/Info.plist)
  install(TARGETS RStudio BUNDLE DESTINATION .)
else()
  install(TARGETS rstudio DESTINATION ${RSTUDIO_INSTALL_BIN})
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
      # install dependent libraries
      set(QT_FULL_VERSION "${QT_VERSION_MAJOR}.${QT_VERSION_MINOR}.${QT_VERSION_PATCH}")
      install(PROGRAMS ${QT_LIBRARY_DIR}/libQtCore.so.4
                       ${QT_LIBRARY_DIR}/libQtCore.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQtDBus.so.4
                       ${QT_LIBRARY_DIR}/libQtDBus.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQtGui.so.4
                       ${QT_LIBRARY_DIR}/libQtGui.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQtNetwork.so.4
                       ${QT_LIBRARY_DIR}/libQtNetwork.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQtWebKit.so.4
                       ${QT_LIBRARY_DIR}/libQtWebKit.so.4.9.0
                       ${QT_LIBRARY_DIR}/libQtSvg.so.4
                       ${QT_LIBRARY_DIR}/libQtSvg.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQtXml.so.4
                       ${QT_LIBRARY_DIR}/libQtXml.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQtXmlPatterns.so.4
                       ${QT_LIBRARY_DIR}/libQtXmlPatterns.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libphonon.so.4
                       ${QT_LIBRARY_DIR}/libphonon.so.4.4.0
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

if (UNIX AND NOT APPLE)
   # add rstudio icon to root (so people installing from source or tar.gz can find it
   install(FILES ${CMAKE_CURRENT_SOURCE_DIR}/resources/freedesktop/icons/48x48/rstudio.png
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING})

   # install configured backtrace utility on linux
   configure_file(${CMAKE_CURRENT_SOURCE_DIR}/rstudio-backtrace.sh.in
                  ${CMAKE_CURRENT_BINARY_DIR}/rstudio-backtrace.sh)
   install(PROGRAMS ${CMAKE_CURRENT_BINARY_DIR}/rstudio-backtrace.sh
                    DESTINATION ${RSTUDIO_INSTALL_BIN})
endif()

# install mac-terminal script on apple
if(APPLE)
   configure_file(${CMAKE_CURRENT_SOURCE_DIR}/mac-terminal.in
                  ${CMAKE_CURRENT_BINARY_DIR}/mac-terminal)
   install(PROGRAMS ${CMAKE_CURRENT_BINARY_DIR}/mac-terminal
                    DESTINATION ${RSTUDIO_INSTALL_BIN})
endif()


# install desktop integration files on linux
if(RSTUDIO_INSTALL_FREEDESKTOP)

   # define freedesktop dirs
   set(RSTUDIO_FREEDESKTOP_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/resources/freedesktop)
   set(RSTUDIO_FREEDESKTOP_BINARY_DIR ${CMAKE_CURRENT_BINARY_DIR}/resources/freedesktop)

   # desktop file (dynamically configured with paths)
   configure_file (${RSTUDIO_FREEDESKTOP_SOURCE_DIR}/rstudio.desktop.in
                   ${RSTUDIO_FREEDESKTOP_BINARY_DIR}/rstudio.desktop)
   install(FILES ${RSTUDIO_FREEDESKTOP_BINARY_DIR}/rstudio.desktop
           DESTINATION /usr/share/applications)

   # mime types
   install(FILES ${RSTUDIO_FREEDESKTOP_SOURCE_DIR}/rstudio.xml
           DESTINATION /usr/share/mime/packages)

   # define icon dirs
   set(RSTUDIO_ICONS_16 ${RSTUDIO_FREEDESKTOP_SOURCE_DIR}/icons/16x16)
   set(RSTUDIO_ICONS_24 ${RSTUDIO_FREEDESKTOP_SOURCE_DIR}/icons/24x24)
   set(RSTUDIO_ICONS_32 ${RSTUDIO_FREEDESKTOP_SOURCE_DIR}/icons/32x32)
   set(RSTUDIO_ICONS_48 ${RSTUDIO_FREEDESKTOP_SOURCE_DIR}/icons/48x48)
   set(RSTUDIO_ICONS_256 ${RSTUDIO_FREEDESKTOP_SOURCE_DIR}/icons/256x256)

   # application icon
   install(FILES ${RSTUDIO_ICONS_32}/rstudio.png
           DESTINATION /usr/share/pixmaps)
   install(FILES ${RSTUDIO_ICONS_16}/rstudio.png
           DESTINATION /usr/share/icons/hicolor/16x16/apps)
   install(FILES ${RSTUDIO_ICONS_24}/rstudio.png
           DESTINATION /usr/share/icons/hicolor/24x24/apps)
   install(FILES ${RSTUDIO_ICONS_32}/rstudio.png
           DESTINATION /usr/share/icons/hicolor/32x32/apps)
   install(FILES ${RSTUDIO_ICONS_48}/rstudio.png
           DESTINATION /usr/share/icons/hicolor/48x48/apps)
   install(FILES ${RSTUDIO_ICONS_256}/rstudio.png
           DESTINATION /usr/share/icons/hicolor/256x256/apps)

   # .RData icon
   install(FILES ${RSTUDIO_ICONS_16}/application-x-r-data.png
           DESTINATION /usr/share/icons/hicolor/16x16/mimetypes)
   install(FILES ${RSTUDIO_ICONS_24}/application-x-r-data.png
           DESTINATION /usr/share/icons/hicolor/24x24/mimetypes)
   install(FILES ${RSTUDIO_ICONS_32}/application-x-r-data.png
           DESTINATION /usr/share/icons/hicolor/32x32/mimetypes)
   install(FILES ${RSTUDIO_ICONS_48}/application-x-r-data.png
           DESTINATION /usr/share/icons/hicolor/48x48/mimetypes)
   install(FILES ${RSTUDIO_ICONS_256}/application-x-r-data.png
           DESTINATION /usr/share/icons/hicolor/256x256/mimetypes)

   # .Rproj icon
   install(FILES ${RSTUDIO_ICONS_16}/application-x-r-project.png
           DESTINATION /usr/share/icons/hicolor/16x16/mimetypes)
   install(FILES ${RSTUDIO_ICONS_24}/application-x-r-project.png
           DESTINATION /usr/share/icons/hicolor/24x24/mimetypes)
   install(FILES ${RSTUDIO_ICONS_32}/application-x-r-project.png
           DESTINATION /usr/share/icons/hicolor/32x32/mimetypes)
   install(FILES ${RSTUDIO_ICONS_48}/application-x-r-project.png
           DESTINATION /usr/share/icons/hicolor/48x48/mimetypes)
   install(FILES ${RSTUDIO_ICONS_256}/application-x-r-project.png
           DESTINATION /usr/share/icons/hicolor/256x256/mimetypes)

endif()
