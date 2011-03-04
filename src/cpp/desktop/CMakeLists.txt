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

# include Qt4
find_package(Qt4 4.7.0 COMPONENTS QtCore QtGui QtNetwork QtWebKit REQUIRED)
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
  ${CMAKE_CURRENT_SOURCE_DIR}/config.h.in
)

configure_file (${CMAKE_CURRENT_SOURCE_DIR}/config.h.in
                ${CMAKE_CURRENT_BINARY_DIR}/config.h)

# source files
set(DESKTOP_SOURCE_FILES
  DesktopAboutDialog.cpp
  DesktopBrowserWindow.cpp
  DesktopCommandInvoker.cpp
  DesktopCRANMirrorDialog.cpp
  DesktopDownloadHelper.cpp
  DesktopGwtCallback.cpp
  DesktopInputDialog.cpp
  DesktopMain.cpp
  DesktopMainWindow.cpp
  DesktopMenuCallback.cpp
  DesktopNetworkProxyFactory.cpp
  DesktopOptions.cpp
  DesktopOptionsDialog.cpp
  DesktopURLDownloader.cpp
  DesktopWebPage.cpp
  DesktopWebView.cpp
  DesktopWindowTracker.cpp
  DesktopSecondaryWindow.cpp
  DesktopSlotBinders.cpp
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
  )
else()
  set(DESKTOP_SOURCE_FILES ${DESKTOP_SOURCE_FILES}
    DesktopPosixApplicationLaunch.cpp
  )
  if(APPLE)
    set(DESKTOP_SOURCE_FILES ${DESKTOP_SOURCE_FILES}
      DesktopMacDetectRHome.cpp
    )
  else()
    set(DESKTOP_SOURCE_FILES ${DESKTOP_SOURCE_FILES}
      DesktopPosixDetectRHome.cpp
    )
  endif(APPLE)
endif(WIN32)

# include directories
include_directories(
  include
  ${Boost_INCLUDE_DIRS}
  ${CORE_SOURCE_DIR}/include
  ${CMAKE_CURRENT_BINARY_DIR}
)

# ensure moc files are generated
qt4_wrap_cpp(MOC_DESKTOP_SOURCE_FILES ${MOC_DESKTOP_HEADER_FILES})
qt4_wrap_ui(DESKTOP_UI_SOURCES
   DesktopAboutDialog.ui
   DesktopChooseRHome.ui
   DesktopCRANMirrorDialog.ui
   DesktopInputDialog.ui
   DesktopOptionsDialog.ui
   DesktopUpdateAvailableDialog.ui
)

set(DESKTOP_RESOURCES_FILES desktop.qrc)
qt4_add_resources(DESKTOP_RESOURCES_SOURCES ${DESKTOP_RESOURCES_FILES})

if(WIN32)

   # configure rdesktop.rc
   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/rdesktop.rc.in
                   ${CMAKE_CURRENT_BINARY_DIR}/rdesktop.rc)

   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/RStudio.ico
                   ${CMAKE_CURRENT_BINARY_DIR}/RStudio.ico COPYONLY)

   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/rdesktop.exe.manifest
                   ${CMAKE_CURRENT_BINARY_DIR}/rdesktop.exe.manifest COPYONLY)

   if(MINGW)
      set(LINK_FLAGS -Wl,-subsystem,windows -lversion)
      add_custom_command(
         OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/res.o"
         COMMAND windres.exe
            -I "."
            -i "rdesktop.rc"
            -o "${CMAKE_CURRENT_BINARY_DIR}/res.o"
            -Ocoff
         DEPENDS
            "${CMAKE_CURRENT_BINARY_DIR}/rdesktop.rc"
            "${CMAKE_CURRENT_SOURCE_DIR}/rdesktop.exe.manifest"
            "${CMAKE_CURRENT_SOURCE_DIR}/RStudio.ico")
      set(DESKTOP_SOURCE_FILES
         ${DESKTOP_SOURCE_FILES}
         "${CMAKE_CURRENT_BINARY_DIR}/res.o")
   endif(MINGW)
elseif(APPLE)
   find_library(APPLICATION_SERVICES_LIBRARY NAMES ApplicationServices)
   set(LINK_FLAGS ${APPLICATION_SERVICES_LIBRARY})
endif(WIN32)


# define executable
add_executable(rdesktop
   ${DESKTOP_SOURCE_FILES}
   ${MOC_DESKTOP_SOURCE_FILES}
   ${DESKTOP_RESOURCES_SOURCES}
   ${DESKTOP_UI_SOURCES}
)

# add rpath for linux so we can find qt libraries in our bin dir
if(UNIX AND NOT APPLE)
   set_target_properties(rdesktop PROPERTIES
                         INSTALL_RPATH ${CMAKE_INSTALL_PREFIX}/bin)
endif()

# set link dependencies
target_link_libraries(rdesktop
  ${QT_LIBRARIES}
  rstudio-core
  ${LINK_FLAGS}
)

# for OSX we also create a bundled executable for the installer. note that
# we still build the desktop/rdesktop exe on apple (for dev/debug) however
# we don't install it
if(APPLE)

   # configure Info.plist
   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/Info.plist.in
                   ${CMAKE_CURRENT_BINARY_DIR}/Info.plist)

   # set our icns as the bundle icon
   set(MACOSX_BUNDLE_ICON_FILE RStudio.icns)
   set_source_files_properties(${CMAKE_CURRENT_SOURCE_DIR}/RStudio.icns
      PROPERTIES MACOSX_PACKAGE_LOCATION Resources)

   # define bundle name and executable
   set(MACOSX_BUNDLE_BUNDLE_NAME "RStudio")

   add_executable(RStudio MACOSX_BUNDLE
      ${DESKTOP_SOURCE_FILES}
      ${MOC_DESKTOP_SOURCE_FILES}
      ${DESKTOP_RESOURCES_SOURCES}
      ${DESKTOP_UI_SOURCES}
      RStudio.icns)

   target_link_libraries(RStudio
      ${QT_LIBRARIES}
      rstudio-core
      ${LINK_FLAGS})
endif()

# install target (OSX install goes into the bundle)
if(APPLE)
  install(TARGETS RStudio BUNDLE DESTINATION .)
  install(FILES ${CMAKE_CURRENT_BINARY_DIR}/Info.plist
          DESTINATION RStudio.app/Contents)
else()
  install(TARGETS rdesktop DESTINATION ${RSTUDIO_INSTALL_BIN})
endif()

# bundle qt dependencies
   
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


# install desktop integration files on linux
if (UNIX AND NOT APPLE)

   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/resources/rstudio.desktop.in
                   ${CMAKE_CURRENT_BINARY_DIR}/resources/rstudio.desktop)

   install(FILES ${CMAKE_CURRENT_BINARY_DIR}/resources/rstudio.desktop DESTINATION /usr/share/applications)
   install(FILES resources/logo/rstudio_logo_32.png DESTINATION /usr/share/pixmaps)
endif()





