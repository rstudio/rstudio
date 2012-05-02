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

project (SESSION)

add_subdirectory(workers)

# verify that install-dictionaries has been run
if(NOT EXISTS "${RSTUDIO_DEPENDENCIES_DIR}/common/dictionaries")
  message(FATAL_ERROR "Dictionaries not found (re-run install-dependencies script to install)")
endif()

# include files
file(GLOB_RECURSE SESSION_HEADER_FILES "*.h*")


# source files
set (SESSION_SOURCE_FILES
   SessionClientEvent.cpp
   SessionClientEventQueue.cpp
   SessionClientEventService.cpp
   SessionSSH.cpp
   SessionMain.cpp
   SessionModuleContext.cpp
   SessionOptions.cpp
   SessionPersistentState.cpp
   SessionPostback.cpp
   SessionSourceDatabase.cpp
   SessionSourceDatabaseSupervisor.cpp
   SessionUserSettings.cpp
   SessionWorkerContext.cpp
   http/SessionHttpConnectionQueue.cpp
   modules/SessionAgreement.cpp
   modules/SessionAskPass.cpp
   modules/SessionAuthoring.cpp
   modules/SessionCodeSearch.cpp
   modules/SessionConsole.cpp
   modules/SessionConsoleProcess.cpp
   modules/SessionContentUrls.cpp
   modules/SessionData.cpp
   modules/SessionFiles.cpp
   modules/SessionFilesListingMonitor.cpp
   modules/SessionFilesQuotas.cpp
   modules/SessionFind.cpp
   modules/SessionGit.cpp
   modules/SessionHelp.cpp
   modules/SessionHistory.cpp
   modules/SessionHTMLPreview.cpp
   modules/SessionLimits.cpp
   modules/SessionLists.cpp
   modules/SessionPackages.cpp
   modules/SessionPath.cpp
   modules/SessionPlots.cpp
   modules/SessionSource.cpp
   modules/SessionSpelling.cpp
   modules/SessionSVN.cpp
   modules/SessionVCS.cpp
   modules/SessionWorkbench.cpp
   modules/SessionWorkspace.cpp
   modules/tex/SessionCompilePdf.cpp
   modules/tex/SessionCompilePdfSupervisor.cpp
   modules/tex/SessionPdfLatex.cpp
   modules/tex/SessionRnwConcordance.cpp
   modules/tex/SessionRnwWeave.cpp
   modules/tex/SessionSynctex.cpp
   modules/tex/SessionTexi2Dvi.cpp
   modules/tex/SessionTexUtils.cpp
   modules/tex/SessionViewPdf.cpp
   modules/vcs/SessionVCSCore.cpp
   modules/vcs/SessionVCSUtils.cpp
   projects/SessionProjects.cpp
   projects/SessionProjectContext.cpp
   ${CMAKE_CURRENT_BINARY_DIR}/SessionAddins.cpp
)

# platform specific source files
if(UNIX)
   set(SESSION_SOURCE_FILES ${SESSION_SOURCE_FILES}
      http/SessionPosixHttpConnectionListener.cpp
   )
   if(RSTUDIO_SERVER)
      set(SESSION_SOURCE_FILES ${SESSION_SOURCE_FILES}
         modules/SessionCrypto.cpp
      )
   endif()
else()
   set(SESSION_SOURCE_FILES ${SESSION_SOURCE_FILES}
      http/SessionWin32HttpConnectionListener.cpp
   )
endif()

# R files
file(GLOB_RECURSE SESSION_R_FILES "modules/*.R")

# define core include dirs
set(CORE_INCLUDE_DIRS ${CORE_SOURCE_DIR}/include)

# include addins
if(RSTUDIO_ADDINS_PATH)
   # search for addins (then remove special core directory)
   file(GLOB RSTUDIO_ADDINS ${RSTUDIO_ADDINS_PATH}/*)
   list(REMOVE_ITEM RSTUDIO_ADDINS "core")

   # incorporate all addins found
   foreach(RSTUDIO_ADDIN ${RSTUDIO_ADDINS})
      set(SESSION_ADDIN_PATH  ${RSTUDIO_ADDIN}/session)
      if(EXISTS ${SESSION_ADDIN_PATH})
         # glob the hpp, cpp, and R files
         file(GLOB_RECURSE ADDIN_HEADER_FILES "${SESSION_ADDIN_PATH}/*.h*")
         list(APPEND SESSION_HEADER_FILES ${ADDIN_HEADER_FILES})
         file(GLOB_RECURSE ADDIN_SOURCE_FILES "${SESSION_ADDIN_PATH}/*.c*")
         list(APPEND SESSION_SOURCE_FILES ${ADDIN_SOURCE_FILES})
         file(GLOB_RECURSE ADDIN_R_FILES "${SESSION_ADDIN_PATH}/*.R")
         list(APPEND SESSION_R_FILES ${ADDIN_R_FILES})

         # generate an initialize call
         get_filename_component(ADDIN_NAME ${RSTUDIO_ADDIN} NAME)
         set(SESSION_ADDIN_DECLARATIONS
            "${SESSION_ADDIN_DECLARATIONS}namespace ${ADDIN_NAME} { Error initialize(); }\n" )
         set(SESSION_ADDIN_INITIALIZATIONS
            "${SESSION_ADDIN_INITIALIZATIONS}(${ADDIN_NAME}::initialize) ")
      endif()
   endforeach()

   # add to core include dirs if appropriate
   set(CORE_ADDINS_INCLUDE_DIR ${RSTUDIO_ADDINS_PATH}/core/include)
   if(EXISTS ${CORE_ADDINS_INCLUDE_DIR})
      list(APPEND CORE_INCLUDE_DIRS ${CORE_ADDINS_INCLUDE_DIR})
   endif()

endif()

# config file
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.in
               ${CMAKE_CURRENT_BINARY_DIR}/config.h)

# always configure the addins bootstrap file
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/SessionAddins.cpp.in
               ${CMAKE_CURRENT_BINARY_DIR}/SessionAddins.cpp)

# configure R files into the binary directory
foreach(SESSION_R_FILE ${SESSION_R_FILES})
   get_filename_component(R_FILE_NAME ${SESSION_R_FILE} NAME)
   configure_file(${SESSION_R_FILE}
                  "${CMAKE_CURRENT_BINARY_DIR}/modules/R/${R_FILE_NAME}"
                  COPYONLY)
endforeach()

# set include directories
include_directories(
   include
   ${Boost_INCLUDE_DIRS}
   ${LIBR_INCLUDE_DIRS}
   ${CORE_INCLUDE_DIRS}
   ${R_SOURCE_DIR}/include
   ${CMAKE_CURRENT_BINARY_DIR}
)

# link directories
link_directories(${R_GRAPHICS_HANDLER_SYSTEM_LIBRARY_DIRS})

if(WIN32)
   # configure rsession.rc
   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/rsession.rc.in
                   ${CMAKE_CURRENT_BINARY_DIR}/rsession.rc)


   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/rsession.exe.manifest
                   ${CMAKE_CURRENT_BINARY_DIR}/rsession.exe.manifest COPYONLY)

   add_custom_command(
      OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/res.o"
      COMMAND windres.exe
         -I "."
         -i "rsession.rc"
         -o "${CMAKE_CURRENT_BINARY_DIR}/res.o"
         -Ocoff
      DEPENDS
         "${CMAKE_CURRENT_BINARY_DIR}/rsession.rc"
         "${CMAKE_CURRENT_SOURCE_DIR}/rsession.exe.manifest")
   set(SESSION_SOURCE_FILES
      ${SESSION_SOURCE_FILES}
      "${CMAKE_CURRENT_BINARY_DIR}/res.o")
   if(NOT RSTUDIO_SESSION_WIN64)
      add_subdirectory(consoleio)
   endif()
endif()

# define executable
add_executable(rsession ${SESSION_SOURCE_FILES} ${SESSION_HEADER_FILES})

# set link dependencies
if(WIN32)
   set(RSTUDIO_CORE_ZLIB rstudio-core-zlib)
endif()
if(APPLE)
   find_library(MAC_APPKIT_LIBRARY NAMES AppKit)
   set (SESSION_SYSTEM_LIBRARIES
        ${SESSION_SYSTEM_LIBRARIES}
        ${MAC_APPKIT_LIBRARY})
endif()
target_link_libraries(rsession
   rstudio-core
   rstudio-core-hunspell
   rstudio-core-synctex
   ${RSTUDIO_CORE_ZLIB}
   rstudio-r
   rstudio-session-workers
   ${SESSION_SYSTEM_LIBRARIES}
)

# configure and install r-ldpaths script
if(UNIX AND NOT APPLE)
   configure_file(${CMAKE_CURRENT_SOURCE_DIR}/r-ldpath.in
                  ${CMAKE_CURRENT_BINARY_DIR}/r-ldpath)
   install(PROGRAMS ${CMAKE_CURRENT_BINARY_DIR}/r-ldpath
           DESTINATION ${RSTUDIO_INSTALL_BIN})
endif()

# install binary
install(TARGETS rsession DESTINATION ${RSTUDIO_INSTALL_BIN})

# include resources, R scripts and 64bit binaries if this isn't a session 64bit build
if (NOT RSTUDIO_SESSION_WIN64)

   # postback
   add_subdirectory(postback)

   # rspdflatex
   add_subdirectory(rspdflatex)

   # HTML resources
   file(GLOB HTML_RESOURCE_FILES "resources/*.htm")
   install(FILES ${HTML_RESOURCE_FILES}
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)
   # CSS resources
   file(GLOB CSS_RESOURCE_FILES "resources/*.css")
   install(FILES ${CSS_RESOURCE_FILES}
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)

   # JS resources
   file(GLOB JS_RESOURCE_FILES "resources/*.js")
   install(FILES ${JS_RESOURCE_FILES}
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)

   # R scripts
   file(GLOB R_MODULE_SRC_FILES "${CMAKE_CURRENT_BINARY_DIR}/modules/R/*.R")
   install(FILES ${R_MODULE_SRC_FILES}
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/R/modules)
           
   # install 64 bit binaries if we are on win64
   if(WIN32)
      if(NOT ("$ENV{PROGRAMW6432}" STREQUAL ""))
         file(MAKE_DIRECTORY  "${CMAKE_CURRENT_BINARY_DIR}/x64")
         install(DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/x64"
                 USE_SOURCE_PERMISSIONS
                 DESTINATION  ${RSTUDIO_INSTALL_BIN})
      endif()
   endif()

   # install gnudiff, mysys_ssh, and sumatra-pdf on windows
   if(WIN32)
      install(DIRECTORY "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/gnudiff"
              USE_SOURCE_PERMISSIONS
              DESTINATION  ${RSTUDIO_INSTALL_BIN})
      install(DIRECTORY "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/gnugrep"
              USE_SOURCE_PERMISSIONS
              DESTINATION  ${RSTUDIO_INSTALL_BIN})
      install(DIRECTORY "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/msys_ssh"
              USE_SOURCE_PERMISSIONS
              DESTINATION  ${RSTUDIO_INSTALL_BIN})


      install(PROGRAMS "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/sumatra/SumatraPDF.exe"
              DESTINATION  "${RSTUDIO_INSTALL_BIN}/sumatra")
      install(FILES resources/sumatrapdfrestrict.ini
               DESTINATION  "${RSTUDIO_INSTALL_BIN}/sumatra")
   endif()
endif()

# install hunspell dictionaries
install(DIRECTORY "${RSTUDIO_DEPENDENCIES_DIR}/common/dictionaries"
        DESTINATION "${RSTUDIO_INSTALL_SUPPORTING}/resources")

# add gpl for desktop
if(RSTUDIO_DESKTOP)
   install(FILES resources/agpl-3.0-standalone.html
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)
endif()

 # install 64-bit gcc runtime for session win64
if(RSTUDIO_SESSION_WIN64)
   get_filename_component(GCC_PATH ${CMAKE_C_COMPILER} PATH CACHE)
   install(PROGRAMS ${GCC_PATH}/libgcc_s_sjlj-1.dll
           DESTINATION ${RSTUDIO_INSTALL_BIN})
endif()


