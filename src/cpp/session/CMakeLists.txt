#
# CMakeLists.txt
#
# Copyright (C) 2009-16 by RStudio, Inc.
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

project (SESSION)

add_subdirectory(workers)

# verify that install-dictionaries, install-mathjax, install-pandoc,
# and install-rmarkdown have been run
if(NOT EXISTS "${RSTUDIO_DEPENDENCIES_DIR}/common/dictionaries")
  message(FATAL_ERROR "Dictionaries not found (re-run install-dependencies script to install)")
endif()
if(NOT EXISTS "${RSTUDIO_DEPENDENCIES_DIR}/common/mathjax-26")
  message(FATAL_ERROR "Mathjax 2.6 not found (re-run install-dependencies script to install)")
endif()
if(NOT EXISTS "${RSTUDIO_DEPENDENCIES_DIR}/common/pandoc")
  message(FATAL_ERROR "pandoc not found (re-run install-dependencies script to install)")
endif()
if(NOT EXISTS "${RSTUDIO_DEPENDENCIES_DIR}/common/rmarkdown")
  message(FATAL_ERROR "rmarkdown package not found (re-run install-dependencies script to install)")
endif()
if(NOT EXISTS "${RSTUDIO_DEPENDENCIES_DIR}/common/rsconnect")
  message(FATAL_ERROR "rsconnect package not found (re-run install-dependencies script to install)")
endif()

# verify libclang is installed
if(WIN32)
   set(LIBCLANG_VERSION "3.4")
else()
   set(LIBCLANG_VERSION "3.5")
endif()
set(LIBCLANG_DIR "${RSTUDIO_DEPENDENCIES_DIR}/common/libclang/${LIBCLANG_VERSION}")
if(NOT EXISTS "${LIBCLANG_DIR}")
  message(FATAL_ERROR "libclang ${LIBCLANG_VERSION} not found  (re-run install-dependencies script to install)")
endif()
set(LIBCLANG_HEADERS_DIR "${RSTUDIO_DEPENDENCIES_DIR}/common/libclang/builtin-headers")
if(NOT EXISTS "${LIBCLANG_HEADERS_DIR}")
  message(FATAL_ERROR "libclang builtin-headers not found  (re-run install-dependencies script to install)")
endif()


# include files
file(GLOB_RECURSE SESSION_HEADER_FILES "*.h*")


# source files
set (SESSION_SOURCE_FILES
   SessionAsyncRProcess.cpp
   SessionClientEvent.cpp
   SessionClientEventQueue.cpp
   SessionClientEventService.cpp
   SessionClientInit.cpp
   SessionConsoleInput.cpp
   SessionConsoleProcess.cpp
   SessionConsoleProcessInfo.cpp
   SessionConsoleProcessPersist.cpp
   SessionConsoleProcessSocket.cpp
   SessionContentUrls.cpp
   SessionDirs.cpp
   SessionRpc.cpp
   SessionHttpMethods.cpp
   SessionInit.cpp
   SessionMain.cpp
   SessionMainOverlay.cpp
   SessionMainProcess.cpp
   SessionModuleContext.cpp
   SessionOptions.cpp
   SessionOptionsOverlay.cpp
   SessionPersistentState.cpp
   SessionPostback.cpp
   SessionRUtil.cpp
   SessionSSH.cpp
   SessionSourceDatabase.cpp
   SessionSourceDatabaseSupervisor.cpp
   SessionSuspend.cpp
   SessionUriHandlers.cpp
   SessionUserSettings.cpp
   SessionWorkerContext.cpp
   http/SessionHttpConnectionQueue.cpp
   http/SessionHttpConnectionUtils.cpp
   modules/RStudioAPI.cpp
   modules/SessionAbout.cpp
   modules/SessionAgreement.cpp
   modules/SessionAskPass.cpp
   modules/SessionAsyncPackageInformation.cpp
   modules/SessionAuthoring.cpp
   modules/SessionBreakpoints.cpp
   modules/SessionCodeSearch.cpp
   modules/SessionUserCommands.cpp
   modules/SessionConsole.cpp
   modules/SessionDependencies.cpp
   modules/SessionDiagnostics.cpp
   modules/SessionDirty.cpp
   modules/SessionErrors.cpp
   modules/SessionFiles.cpp
   modules/SessionFilesListingMonitor.cpp
   modules/SessionFilesQuotas.cpp
   modules/SessionFind.cpp
   modules/SessionGit.cpp
   modules/SessionHelp.cpp
   modules/SessionHelpHome.cpp
   modules/SessionHistory.cpp
   modules/SessionHistoryArchive.cpp
   modules/SessionHTMLPreview.cpp
   modules/SessionLibPathsIndexer.cpp
   modules/SessionLimits.cpp
   modules/SessionLists.cpp
   modules/SessionMarkers.cpp
   modules/SessionObjectExplorer.cpp
   modules/SessionPackageProvidedExtension.cpp
   modules/SessionPackages.cpp
   modules/SessionPackrat.cpp
   modules/SessionPath.cpp
   modules/SessionPlots.cpp
   modules/SessionProfiler.cpp
   modules/SessionProjectTemplate.cpp
   modules/SessionRAddins.cpp
   modules/SessionRCompletions.cpp
   modules/SessionRHooks.cpp
   modules/SessionRParser.cpp
   modules/SessionRPubs.cpp
   modules/SessionRSConnect.cpp
   modules/SessionShinyViewer.cpp
   modules/SessionSnippets.cpp
   modules/SessionSource.cpp
   modules/SessionSpelling.cpp
   modules/SessionTerminalShell.cpp
   modules/SessionSVN.cpp
   modules/SessionUpdates.cpp
   modules/SessionVCS.cpp
   modules/SessionWorkbench.cpp
   modules/build/SessionBuild.cpp
   modules/build/SessionBuildEnvironment.cpp
   modules/build/SessionBuildErrors.cpp
   modules/build/SessionSourceCpp.cpp
   modules/clang/CodeCompletion.cpp
   modules/clang/DefinitionIndex.cpp
   modules/clang/Diagnostics.cpp
   modules/clang/FindReferences.cpp
   modules/clang/GoToDefinition.cpp
   modules/clang/RCompilationDatabase.cpp
   modules/clang/RSourceIndex.cpp
   modules/clang/SessionClang.cpp
   modules/connections/ActiveConnections.cpp
   modules/connections/Connection.cpp
   modules/connections/ConnectionHistory.cpp
   modules/connections/ConnectionsIndexer.cpp
   modules/connections/SessionConnections.cpp
   modules/data/SessionData.cpp
   modules/data/DataViewer.cpp
   modules/environment/EnvironmentMonitor.cpp
   modules/environment/EnvironmentUtils.cpp
   modules/environment/SessionEnvironment.cpp
   modules/mathjax/SessionMathJax.cpp
   modules/overlay/SessionOverlay.cpp
   modules/presentation/SessionPresentation.cpp
   modules/presentation/PresentationLog.cpp
   modules/presentation/PresentationState.cpp
   modules/presentation/SlideMediaRenderer.cpp
   modules/presentation/SlideNavigationList.cpp
   modules/presentation/SlideParser.cpp
   modules/presentation/SlideQuizRenderer.cpp
   modules/presentation/SlideRenderer.cpp
   modules/presentation/SlideRequestHandler.cpp
   modules/presentation/Tutorial.cpp
   modules/presentation/TutorialInstaller.cpp
   modules/rmarkdown/SessionRMarkdown.cpp
   modules/rmarkdown/SessionRmdNotebook.cpp
   modules/rmarkdown/SessionExecuteChunkOperation.cpp
   modules/rmarkdown/NotebookAlternateEngines.cpp
   modules/rmarkdown/NotebookCache.cpp
   modules/rmarkdown/NotebookCapture.cpp
   modules/rmarkdown/NotebookChunkDefs.cpp
   modules/rmarkdown/NotebookChunkOptions.cpp
   modules/rmarkdown/NotebookConditions.cpp
   modules/rmarkdown/NotebookData.cpp
   modules/rmarkdown/NotebookDocQueue.cpp
   modules/rmarkdown/NotebookErrors.cpp
   modules/rmarkdown/NotebookExec.cpp
   modules/rmarkdown/NotebookHtmlWidgets.cpp
   modules/rmarkdown/NotebookOutput.cpp
   modules/rmarkdown/NotebookPaths.cpp
   modules/rmarkdown/NotebookPlotReplay.cpp
   modules/rmarkdown/NotebookPlots.cpp
   modules/rmarkdown/NotebookQueue.cpp
   modules/rmarkdown/NotebookQueueUnit.cpp
   modules/rmarkdown/NotebookWorkingDir.cpp
   modules/rmarkdown/RMarkdownPresentation.cpp
   modules/rmarkdown/RMarkdownTemplates.cpp
   modules/shiny/SessionShiny.cpp
   modules/tex/SessionCompilePdf.cpp
   modules/tex/SessionCompilePdfSupervisor.cpp
   modules/tex/SessionPdfLatex.cpp
   modules/tex/SessionRnwConcordance.cpp
   modules/tex/SessionRnwWeave.cpp
   modules/tex/SessionSynctex.cpp
   modules/tex/SessionTexUtils.cpp
   modules/tex/SessionViewPdf.cpp
   modules/vcs/SessionVCSCore.cpp
   modules/vcs/SessionVCSUtils.cpp
   modules/viewer/SessionViewer.cpp
   modules/viewer/ViewerHistory.cpp
   projects/SessionProjects.cpp
   projects/SessionProjectContext.cpp
   projects/SessionProjectFirstRun.cpp
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
   if(APPLE)
      set(SESSION_SOURCE_FILES ${SESSION_SOURCE_FILES}
         SessionModuleContext.mm
      )
   endif()
else()
   set(SESSION_SOURCE_FILES ${SESSION_SOURCE_FILES}
      http/SessionWin32HttpConnectionListener.cpp
      modules/build/SessionInstallRtools.cpp
   )
endif()

# R files
file(GLOB_RECURSE SESSION_R_FILES "modules/*.R")

# test files
if (RSTUDIO_UNIT_TESTS_ENABLED)

  file(GLOB_RECURSE SESSION_TEST_FILES "*Tests.cpp")
  list(APPEND SESSION_SOURCE_FILES ${SESSION_TEST_FILES})

endif()

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
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/session-config.h.in
               ${CMAKE_CURRENT_BINARY_DIR}/session-config.h)

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

# configure the NOTICE file into the resources directory
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/../../../NOTICE
               ${CMAKE_CURRENT_SOURCE_DIR}/resources COPYONLY)

# set include directories
include_directories(
   include
   ${LIBR_INCLUDE_DIRS}
   ${CORE_INCLUDE_DIRS}
   ${EXT_SOURCE_DIR}
   ${MONITOR_SOURCE_DIR}/include
   ${R_SOURCE_DIR}/include
   ${CMAKE_CURRENT_BINARY_DIR}
   ${TESTS_INCLUDE_DIR}
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
      COMMAND "${WINDRES}"
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

# skip libR RPATH at development time
if(RSTUDIO_DEVELOPMENT OR RSTUDIO_RUN_IN_PLACE)
   set_target_properties(rsession PROPERTIES SKIP_BUILD_RPATH TRUE)
endif()

# add origin rpath for suse/sles
if(RSTUDIO_PACKAGE_BUILD_SLES)
   set(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
   set_target_properties(rsession PROPERTIES
                         INSTALL_RPATH \$ORIGIN)
endif()

# set link dependencies
if(WIN32)
   set(RSTUDIO_CORE_ZLIB rstudio-core-zlib)
   set(SESSION_SYSTEM_LIBRARIES ${SESSION_SYSTEM_LIBRARIES} -ladvapi32)
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
   rstudio-monitor
   rstudio-r
   rstudio-session-workers
   ${SESSION_SYSTEM_LIBRARIES}
   ${LIBR_LIBRARIES}
   ${CMAKE_DL_LIBS}
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

   # HTML resources
   file(GLOB HTML_RESOURCE_FILES "resources/*.html")
   install(FILES ${HTML_RESOURCE_FILES}
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)
   # CSS resources
   file(GLOB CSS_RESOURCE_FILES "resources/*.css")
   install(FILES ${CSS_RESOURCE_FILES}
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)
   # templates
   install(DIRECTORY "resources/templates"
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)
   # JS resources
   file(GLOB JS_RESOURCE_FILES "resources/*.js")
   install(FILES ${JS_RESOURCE_FILES}
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)
   # presentation
   install(DIRECTORY "resources/presentation"
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)
   # notice
   install(FILES "resources/NOTICE"
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)

   # citation
   install(FILES "resources/CITATION"
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)

   # R scripts
   file(GLOB R_MODULE_SRC_FILES "${CMAKE_CURRENT_BINARY_DIR}/modules/R/*.R")
   install(FILES ${R_MODULE_SRC_FILES}
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/R/modules)

   # install hunspell dictionaries
   install(DIRECTORY "${RSTUDIO_DEPENDENCIES_DIR}/common/dictionaries"
           DESTINATION "${RSTUDIO_INSTALL_SUPPORTING}/resources")

   # install mathjax for local html preview
   install(DIRECTORY "${RSTUDIO_DEPENDENCIES_DIR}/common/mathjax-26"
           DESTINATION "${RSTUDIO_INSTALL_SUPPORTING}/resources")

   # icons for database connections
   install(DIRECTORY "resources/connections"
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)

   # install pandoc
   if(RSTUDIO_PACKAGE_BUILD_SLES)
      # when producing a SUSE Linux Enterpise build, we want to install the 
      # static Pandoc binaries (see dependencies/common/install-pandoc)
      set(PANDOC_EXTENSION_DIR "/static")
   endif()
   set(PANDOC_BIN "${RSTUDIO_DEPENDENCIES_DIR}/common/pandoc/1.17.2${PANDOC_EXTENSION_DIR}")
   file(GLOB PANDOC_FILES "${PANDOC_BIN}/pandoc*")
   install(FILES ${PANDOC_FILES}
           PERMISSIONS OWNER_READ OWNER_WRITE OWNER_EXECUTE GROUP_READ GROUP_EXECUTE WORLD_READ WORLD_EXECUTE
           DESTINATION  ${RSTUDIO_INSTALL_BIN}/pandoc)

   # install rmarkdown package
   file(GLOB RMARKDOWN_PACKAGE "${RSTUDIO_DEPENDENCIES_DIR}/common/rmarkdown*.tar.gz")
   install(FILES ${RMARKDOWN_PACKAGE}
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/R/packages)

   # install rsconnect package
   file(GLOB RSCONNECT_PACKAGE "${RSTUDIO_DEPENDENCIES_DIR}/common/rsconnect*.tar.gz")
   install(FILES ${RSCONNECT_PACKAGE}
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/R/packages)

   # install PDF.js
   install(DIRECTORY "resources/pdfjs"
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)

   # install DataTables
   install(DIRECTORY "resources/grid"
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)

   # help resources
   install(DIRECTORY "resources/help_resources"
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)

   # install pagedtable
   install(DIRECTORY "resources/pagedtable"
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)

   # install libclang
   if(WIN32)
      file(GLOB LIBCLANG_32_FILES "${LIBCLANG_DIR}/x86/libclang.*")
      install(PROGRAMS ${LIBCLANG_32_FILES}
              DESTINATION  ${RSTUDIO_INSTALL_BIN}/rsclang/x86)
      file(GLOB LIBCLANG_64_FILES "${LIBCLANG_DIR}/x86_64/libclang.*")
      install(PROGRAMS ${LIBCLANG_64_FILES}
              DESTINATION  ${RSTUDIO_INSTALL_BIN}/rsclang/x86_64)
   else()
      file(GLOB_RECURSE LIBCLANG_FILES "${LIBCLANG_DIR}/libclang.*")
      install(PROGRAMS ${LIBCLANG_FILES}
              DESTINATION  ${RSTUDIO_INSTALL_BIN}/rsclang)
   endif()

   # install libclang builtin-headers
   install(DIRECTORY "${RSTUDIO_DEPENDENCIES_DIR}/common/libclang/builtin-headers"
           DESTINATION "${RSTUDIO_INSTALL_SUPPORTING}/resources/libclang")

   # install winpty on windows
   if(WIN32)
      install(PROGRAMS "${WINPTY_BINDIR_32}/winpty.dll"
              DESTINATION "${RSTUDIO_INSTALL_BIN}")
      install(PROGRAMS "${WINPTY_BINDIR_32}/winpty-agent.exe"
              DESTINATION "${RSTUDIO_INSTALL_BIN}")
   endif()

   # install 64 bit binaries if we are on win64
   if(WIN32)
      if(NOT ("$ENV{PROGRAMW6432}" STREQUAL ""))
         file(MAKE_DIRECTORY  "${CMAKE_CURRENT_BINARY_DIR}/x64")
         install(DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/x64"
                 USE_SOURCE_PERMISSIONS
                 DESTINATION  ${RSTUDIO_INSTALL_BIN})
         install(PROGRAMS "${WINPTY_BINDIR_64}/winpty.dll"
                 DESTINATION "${RSTUDIO_INSTALL_BIN}/x64")
         install(PROGRAMS "${WINPTY_BINDIR_64}/winpty-agent.exe"
                 DESTINATION "${RSTUDIO_INSTALL_BIN}/x64")

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
      install(DIRECTORY "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/msys-ssh-1000-18"
              USE_SOURCE_PERMISSIONS
              DESTINATION  ${RSTUDIO_INSTALL_BIN})


      install(PROGRAMS "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/sumatra/3.1.1/SumatraPDF.exe"
              DESTINATION  "${RSTUDIO_INSTALL_BIN}/sumatra")
      install(FILES resources/sumatrapdfrestrict.ini
               DESTINATION  "${RSTUDIO_INSTALL_BIN}/sumatra")

      install(PROGRAMS "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/winutils/1.0/winutils.exe"
              DESTINATION ${RSTUDIO_INSTALL_BIN}/winutils)
      install(PROGRAMS "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/winutils/1.0/x64/winutils.exe"
              DESTINATION ${RSTUDIO_INSTALL_BIN}/winutils/x64)

   endif()

endif()

# # install 64-bit gcc runtime for session win64
# # not needed with static build / link of gcc
# if(RSTUDIO_SESSION_WIN64)
#    get_filename_component(GCC_PATH ${CMAKE_C_COMPILER} PATH CACHE)
#    install(PROGRAMS ${GCC_PATH}/libgcc_s_sjlj-1.dll
#                     ${GCC_PATH}/libstdc++-6.dll
#                     ${GCC_PATH}/libwinpthread-1.dll
#            DESTINATION ${RSTUDIO_INSTALL_BIN})
# endif()

# add overlay if it exists
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/CMakeOverlay.txt")
   include(CMakeOverlay.txt)
endif()
