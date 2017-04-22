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

cmake_minimum_required(VERSION 2.8.8)

cmake_policy(SET CMP0020 OLD)

project(DESKTOP)

# require Qt 5.4
if(WIN32)
  set(QT_VERSION "5.4.1")
else()
  set(QT_VERSION "5.4.0")
endif()

set(QT_VERSION_SUBDIR "5.4")

# on unix prefer qtsdk installs over system-level libraries. note this
# can be overriden by defining QT_QMAKE_EXECUTABLE when invoking cmake
if(NOT WIN32)
   # prefer rstudio qtsdk install then home qtsdk install
   if(NOT QT_QMAKE_EXECUTABLE)
      if(APPLE)
         set(QT_COMPILER "clang_64")
      else()
         # Qt named compiler folder varies by platform
         if(CMAKE_SIZEOF_VOID_P EQUAL 8)
           set(QT_COMPILER "gcc_64")
         else()
           set(QT_COMPILER "gcc")
         endif()
      endif()

      set(QMAKE_QT50_SDK "/opt/RStudio-QtSDK/Qt${QT_VERSION}/${QT_VERSION_SUBDIR}/${QT_COMPILER}")
      if(EXISTS ${QMAKE_QT50_SDK})
         set(QT_QMAKE_EXECUTABLE "${QMAKE_QT50_SDK}/bin/qmake")
      else()
         set(QMAKE_QT50_SDK "$ENV{HOME}/Qt${QT_VERSION}/${QT_VERSION_SUBDIR}/${QT_COMPILER}")
         if(EXISTS ${QMAKE_QT50_SDK})
            set(QT_QMAKE_EXECUTABLE "${QMAKE_QT50_SDK}/bin/qmake")
         endif()
      endif()
   endif()
else()
   if(NOT QT_QMAKE_EXECUTABLE)
      set(QMAKE_QT50_SDK "C:\\Qt\\Qt${QT_VERSION}\\${QT_VERSION_SUBDIR}\\mingw491_32")
      if(EXISTS ${QMAKE_QT50_SDK})
         set(QT_QMAKE_EXECUTABLE "${QMAKE_QT50_SDK}\\bin\\qmake.exe")
      else()
         message(FATAL_ERROR "Failed to locate Qt ${QT_VERSION} SDK (expected SDK at ${QMAKE_QT50_SDK})")
      endif()
   endif()
endif()

# set the cmake prefix path based on the location of the qmake executable
get_filename_component(QT_BIN_DIR ${QT_QMAKE_EXECUTABLE} PATH)
set(CMAKE_PREFIX_PATH "${QT_BIN_DIR}//..//lib//cmake")

# find and include Qt
add_definitions(-DQT_NO_CAST_FROM_ASCII -DQT_NO_CAST_TO_ASCII)
set(CMAKE_INCLUDE_CURRENT_DIR ON)

find_package(Qt5Core REQUIRED)
find_package(Qt5Widgets REQUIRED)
find_package(Qt5Gui REQUIRED)
find_package(Qt5Network REQUIRED)
find_package(Qt5WebKit REQUIRED)
find_package(Qt5WebKitWidgets REQUIRED)
find_package(Qt5PrintSupport REQUIRED)
find_package(Qt5Quick REQUIRED)
find_package(Qt5Positioning REQUIRED)
find_package(Qt5Sensors REQUIRED)
find_package(Qt5Svg REQUIRED)
find_package(Qt5Xml REQUIRED)
find_package(Qt5XmlPatterns REQUIRED)
find_package(Qt5Qml REQUIRED)
find_package(Qt5Sql REQUIRED)
find_package(Qt5OpenGL REQUIRED)
if(UNIX AND NOT APPLE)
   find_package(Qt5DBus REQUIRED)
endif()



# disable clang warnings for qt sources
if(APPLE)
   add_definitions(-Wno-unused-private-field
                   -Wno-uninitialized)
endif()

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
  ${CMAKE_CURRENT_SOURCE_DIR}/DesktopNetworkIOService.hpp
  ${CMAKE_CURRENT_SOURCE_DIR}/DesktopNetworkProxyFactory.hpp
  ${CMAKE_CURRENT_SOURCE_DIR}/DesktopGwtCallbackOwner.hpp
  ${CMAKE_CURRENT_SOURCE_DIR}/DesktopProgressActivator.hpp
  ${CMAKE_CURRENT_SOURCE_DIR}/DesktopUtils.hpp
  ${CMAKE_CURRENT_SOURCE_DIR}/DesktopWordViewer.hpp
  ${CMAKE_CURRENT_SOURCE_DIR}/desktop-config.h.in
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

configure_file (${CMAKE_CURRENT_SOURCE_DIR}/desktop-config.h.in
                ${CMAKE_CURRENT_BINARY_DIR}/desktop-config.h)

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
  DesktopNetworkIOService.cpp
  DesktopNetworkReply.cpp
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
  DesktopSubMenu.cpp
  DesktopSynctex.cpp
  DesktopNetworkAccessManager.cpp
  3rdparty/qtsingleapplication/qtsingleapplication.cpp
  3rdparty/qtsingleapplication/qtlocalpeer.cpp
)

if(WIN32)
  set(DESKTOP_SOURCE_FILES ${DESKTOP_SOURCE_FILES}
    DesktopChooseRHome.cpp
    DesktopWin32ApplicationLaunch.cpp
    DesktopRVersion.cpp
    DesktopWin32DetectRHome.cpp
    DesktopWin32ProgressActivator.cpp
    DesktopWordViewer.cpp
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

if(APPLE)
  set(DESKTOP_SOURCE_FILES ${DESKTOP_SOURCE_FILES}
    DesktopUtilsMac.mm
  )
endif()

# include directories
include_directories(
  include
  ${CORE_SOURCE_DIR}/include
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${CMAKE_CURRENT_BINARY_DIR}
  ${TESTS_INCLUDE_DIR}
)

# ensure moc files are generated
qt5_wrap_cpp(MOC_DESKTOP_SOURCE_FILES ${MOC_DESKTOP_HEADER_FILES})
qt5_wrap_ui(DESKTOP_UI_SOURCES
   DesktopAboutDialog.ui
   DesktopChooseRHome.ui
   DesktopInputDialog.ui
)

set(DESKTOP_RESOURCES_FILES desktop.qrc)
qt5_add_resources(DESKTOP_RESOURCES_SOURCES ${DESKTOP_RESOURCES_FILES})

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
         COMMAND "${WINDRES}"
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
   find_library(COCOA_LIBRARY NAMES Cocoa)
   find_library(SECURITY_LIBRARY NAMES Security)
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

   set(RSTUDIO_QTMODULES
      Core
      Widgets
      Gui
      Network
      WebKit
      WebKitWidgets
      PrintSupport
      Quick
      Positioning
      Sensors
      Svg
      Xml
      XmlPatterns
      Qml
      Sql
      OpenGL
   )
   if(WIN32)
      qt5_use_modules(rstudio ${RSTUDIO_QTMODULES})
   else()
      qt5_use_modules(rstudio ${RSTUDIO_QTMODULES} DBus)
   endif()

   # add rpath for linux so we can find qt libraries in our bin dir
   if(UNIX AND RSTUDIO_BUNDLE_QT)
      set(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
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

   qt5_use_modules(RStudio Core Widgets Gui Network WebKit WebKitWidgets)

   target_link_libraries(RStudio
      ${QT_LIBRARIES}
      rstudio-core
      ${APPLICATION_SERVICES_LIBRARY}
      ${COCOA_LIBRARY}
      ${SECURITY_LIBRARY})

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
   # set lib suffix for windows
   if(${CMAKE_BUILD_TYPE} STREQUAL "Debug")
      set(LIB_SUFFIX "d")
   else()
      set(LIB_SUFFIX "")
   endif()

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
   if(WIN32)
      set(QT_IMAGEFORMATS_DIR "${Qt5Core_DIR}/../../../plugins/imageformats")
      install(PROGRAMS "${QT_IMAGEFORMATS_DIR}/qgif${LIB_SUFFIX}.dll"
                       "${QT_IMAGEFORMATS_DIR}/qico${LIB_SUFFIX}.dll"
                       "${QT_IMAGEFORMATS_DIR}/qjpeg${LIB_SUFFIX}.dll"
                       "${QT_IMAGEFORMATS_DIR}/qmng${LIB_SUFFIX}.dll"
                       "${QT_IMAGEFORMATS_DIR}/qsvg${LIB_SUFFIX}.dll"
                       "${QT_IMAGEFORMATS_DIR}/qtga${LIB_SUFFIX}.dll"
                       "${QT_IMAGEFORMATS_DIR}/qtiff${LIB_SUFFIX}.dll"
              DESTINATION ${QTPLUGINS_DEST_DIR}/plugins/imageformats)

      set(QT_PLATFORMS_DIR "${Qt5Core_DIR}/../../../plugins/platforms")
      install(PROGRAMS "${QT_PLATFORMS_DIR}/qminimal${LIB_SUFFIX}.dll"
                       "${QT_PLATFORMS_DIR}/qwindows${LIB_SUFFIX}.dll"
              DESTINATION "${QTPLUGINS_DEST_DIR}/plugins/platforms")
   else()
      install(DIRECTORY "${Qt5Core_DIR}/../../../plugins/imageformats"
              DESTINATION ${QTPLUGINS_DEST_DIR}/plugins
              PATTERN "*_debug.*" EXCLUDE
              PATTERN "*.a" EXCLUDE)
      install(DIRECTORY "${Qt5Core_DIR}/../../../plugins/platforms"
              DESTINATION ${QTPLUGINS_DEST_DIR}/plugins
              PATTERN "*_debug.*" EXCLUDE
              PATTERN "*.a" EXCLUDE)
      install(DIRECTORY "${Qt5Core_DIR}/../../../plugins/platformthemes"
              DESTINATION ${QTPLUGINS_DEST_DIR}/plugins
              PATTERN "*_debug.*" EXCLUDE
              PATTERN "*.a" EXCLUDE)
      install(DIRECTORY "${Qt5Core_DIR}/../../../plugins/platforminputcontexts"
              DESTINATION ${QTPLUGINS_DEST_DIR}/plugins
              PATTERN "*_debug.*" EXCLUDE
              PATTERN "*.a" EXCLUDE)
   endif()

   # fixup bundle on osx
   if(APPLE)
      set(APPS "\${CMAKE_INSTALL_PREFIX}/RStudio.app")
      set(DIRS ${QT_LIBRARY_DIRS})
      INSTALL(CODE "
          file(GLOB_RECURSE QTPLUGINS
            \"\${CMAKE_INSTALL_PREFIX}/${QTPLUGINS_DEST_DIR}/plugins/*${CMAKE_SHARED_LIBRARY_SUFFIX}\")
          include(BundleUtilities)
          file(REMOVE \"\${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/MacOS/rsession\")
          file(REMOVE \"\${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/MacOS/rserver\")
          file(REMOVE \"\${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/MacOS/rserver-pam\")
          fixup_bundle(\"${APPS}\" \"\${QTPLUGINS}\" \"${DIRS}\")
          ")

   # copy qt shared objects directly to bin on unix (fixed up rpath above)
   elseif(UNIX)
      # install dependent libraries
      set(QT_LIBRARY_DIR "${Qt5Core_DIR}/../../../lib")
      set(QT_FULL_VERSION ${QT_VERSION})
      install(PROGRAMS ${QT_LIBRARY_DIR}/libQt5Core.so.5
                       ${QT_LIBRARY_DIR}/libQt5Core.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5DBus.so.5
                       ${QT_LIBRARY_DIR}/libQt5DBus.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5Gui.so.5
                       ${QT_LIBRARY_DIR}/libQt5Gui.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5Network.so.5
                       ${QT_LIBRARY_DIR}/libQt5Network.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5Widgets.so.5
                       ${QT_LIBRARY_DIR}/libQt5Widgets.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5WebKit.so.5
                       ${QT_LIBRARY_DIR}/libQt5WebKit.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5WebKitWidgets.so.5
                       ${QT_LIBRARY_DIR}/libQt5WebKitWidgets.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5WebChannel.so.5
                       ${QT_LIBRARY_DIR}/libQt5WebChannel.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5Positioning.so.5
                       ${QT_LIBRARY_DIR}/libQt5Positioning.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5PrintSupport.so.5
                       ${QT_LIBRARY_DIR}/libQt5PrintSupport.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5Sensors.so.5
                       ${QT_LIBRARY_DIR}/libQt5Sensors.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5Svg.so.5
                       ${QT_LIBRARY_DIR}/libQt5Svg.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5Xml.so.5
                       ${QT_LIBRARY_DIR}/libQt5Xml.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5XmlPatterns.so.5
                       ${QT_LIBRARY_DIR}/libQt5XmlPatterns.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5Quick.so.5
                       ${QT_LIBRARY_DIR}/libQt5Quick.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5Qml.so.5
                       ${QT_LIBRARY_DIR}/libQt5Qml.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5Sql.so.5
                       ${QT_LIBRARY_DIR}/libQt5Sql.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libQt5OpenGL.so.5
                       ${QT_LIBRARY_DIR}/libQt5OpenGL.so.${QT_FULL_VERSION}
                       ${QT_LIBRARY_DIR}/libicui18n.so.53
                       ${QT_LIBRARY_DIR}/libicui18n.so.53.1
                       ${QT_LIBRARY_DIR}/libicuuc.so.53
                       ${QT_LIBRARY_DIR}/libicuuc.so.53.1
                       ${QT_LIBRARY_DIR}/libicudata.so.53
                       ${QT_LIBRARY_DIR}/libicudata.so.53.1
              DESTINATION ${RSTUDIO_INSTALL_BIN})
   # copy qt dlls directly to bin on win32
   elseif(WIN32)
      set(QT_BINARY_DIR "${Qt5Core_DIR}/../../../bin")
      install(PROGRAMS ${QT_BINARY_DIR}/Qt5Core${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5Gui${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5Network${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5Widgets${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5WebKit${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5WebKitWidgets${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5WebChannel${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5Positioning${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5PrintSupport${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5Sensors${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5Svg${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5Xml${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5XmlPatterns${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5Quick${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5Qml${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5Sql${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5OpenGL${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5Multimedia${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/Qt5MultimediaWidgets${LIB_SUFFIX}.dll
                       ${QT_BINARY_DIR}/icudt53.dll
                       ${QT_BINARY_DIR}/icuin53.dll
                       ${QT_BINARY_DIR}/icuuc53.dll
                       ${QT_BINARY_DIR}/libwinpthread-1.dll
                       ${QT_BINARY_DIR}/libstdc++-6.dll
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
