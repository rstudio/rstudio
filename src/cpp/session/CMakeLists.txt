#
# CMakeLists.txt
#
# Copyright (C) 2020 by RStudio, PBC
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
# and install-rmarkdown have been run as required
if(NOT EXISTS "${RSTUDIO_DEPENDENCIES_DIR}/common/dictionaries")
  message(FATAL_ERROR "Dictionaries not found (re-run install-dependencies script to install)")
endif()
if(NOT EXISTS "${RSTUDIO_DEPENDENCIES_DIR}/common/mathjax-27")
  message(FATAL_ERROR "Mathjax 2.7 not found (re-run install-dependencies script to install)")
endif()
if(NOT EXISTS "${RSTUDIO_DEPENDENCIES_DIR}/common/pandoc")
  message(FATAL_ERROR "pandoc not found (re-run install-dependencies script to install)")
endif()

# verify embedded packages are available
foreach(PKG ${RSTUDIO_EMBEDDED_PACKAGES})
   file(GLOB PKG_FILES "${RSTUDIO_DEPENDENCIES_DIR}/common/${PKG}*.tar.gz")
   if(NOT PKG_FILES)
      message(FATAL_ERROR "${PKG} package not found (re-run install-dependencies script to install)")
   endif()
endforeach()

# verify libclang is installed (Windows only)
if(WIN32)
   set(LIBCLANG_VERSION "5.0.2")
   set(LIBCLANG_DIR "${RSTUDIO_DEPENDENCIES_DIR}/common/libclang/${LIBCLANG_VERSION}")
   if(NOT EXISTS "${LIBCLANG_DIR}")
      message(FATAL_ERROR "libclang ${LIBCLANG_VERSION} not found  (re-run install-dependencies script to install)")
   endif()

   set(LIBCLANG_HEADERS_DIR "${RSTUDIO_DEPENDENCIES_DIR}/common/libclang/builtin-headers")
   if(NOT EXISTS "${LIBCLANG_HEADERS_DIR}")
      message(FATAL_ERROR "libclang builtin-headers not found  (re-run install-dependencies script to install)")
   endif()
endif()

# include files
file(GLOB_RECURSE SESSION_HEADER_FILES "*.h*")


# source files
set (SESSION_SOURCE_FILES
   SessionAsyncRProcess.cpp
   SessionAsyncDownloadFile.cpp
   SessionClientEvent.cpp
   SessionClientEventQueue.cpp
   SessionClientEventService.cpp
   SessionClientInit.cpp
   SessionConsoleInput.cpp
   SessionConsoleProcess.cpp
   SessionConsoleProcessApi.cpp
   SessionConsoleProcessInfo.cpp
   SessionConsoleProcessPersist.cpp
   SessionConsoleProcessSocket.cpp
   SessionConsoleProcessSocketPacket.cpp
   SessionConsoleProcessTable.cpp
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
   SessionPandoc.cpp
   SessionPasswordManager.cpp
   SessionPersistentState.cpp
   SessionPostback.cpp
   SessionSSH.cpp
   SessionSourceDatabase.cpp
   SessionSourceDatabaseSupervisor.cpp
   SessionSuspend.cpp
   SessionUriHandlers.cpp
   SessionUrlPorts.cpp
   SessionWorkerContext.cpp
   http/SessionHttpConnectionQueue.cpp
   http/SessionHttpConnectionUtils.cpp
   modules/RStudioAPI.cpp
   modules/SessionAbout.cpp
   modules/SessionApiPrefs.cpp
   modules/SessionAskPass.cpp
   modules/SessionAskSecret.cpp
   modules/SessionAsyncPackageInformation.cpp
   modules/SessionAuthoring.cpp
   modules/SessionBreakpoints.cpp
   modules/SessionCodeSearch.cpp
   modules/SessionConfigFile.cpp
   modules/SessionCRANMirrors.cpp
   modules/SessionCrashHandler.cpp
   modules/SessionUserCommands.cpp
   modules/SessionConsole.cpp
   modules/SessionDependencies.cpp
   modules/SessionDependencyList.cpp
   modules/SessionDiagnostics.cpp
   modules/SessionDirty.cpp
   modules/SessionErrors.cpp
   modules/SessionFiles.cpp
   modules/SessionFilesListingMonitor.cpp
   modules/SessionFilesQuotas.cpp
   modules/SessionFind.cpp
   modules/SessionFonts.cpp
   modules/SessionGit.cpp
   modules/SessionGraphics.cpp
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
   modules/SessionPlumberViewer.cpp
   modules/SessionProfiler.cpp
   modules/SessionProjectTemplate.cpp
   modules/SessionPythonEnvironments.cpp
   modules/SessionRAddins.cpp
   modules/SessionRCompletions.cpp
   modules/SessionRenv.cpp
   modules/SessionReticulate.cpp
   modules/SessionRHooks.cpp
   modules/SessionRParser.cpp
   modules/SessionRPubs.cpp
   modules/SessionRSConnect.cpp
   modules/SessionRUtil.cpp
   modules/SessionRVersions.cpp
   modules/SessionShinyViewer.cpp
   modules/SessionSnippets.cpp
   modules/SessionSource.cpp
   modules/SessionSpelling.cpp
   modules/SessionTerminal.cpp
   modules/SessionTerminalShell.cpp
   modules/SessionTests.cpp
   modules/SessionThemes.cpp
   modules/SessionTutorial.cpp
   modules/SessionSVN.cpp
   modules/SessionUpdates.cpp
   modules/SessionVCS.cpp
   modules/SessionWorkbench.cpp
   modules/SessionUserPrefs.cpp
   modules/SessionUserPrefsMigration.cpp
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
   modules/customsource/SessionCustomSource.cpp
   modules/data/SessionData.cpp
   modules/data/DataViewer.cpp
   modules/environment/EnvironmentMonitor.cpp
   modules/environment/EnvironmentUtils.cpp
   modules/environment/SessionEnvironment.cpp
   modules/jobs/AsyncRJobManager.cpp
   modules/jobs/SessionJobs.cpp
   modules/jobs/ScriptJob.cpp
   modules/jobs/Job.cpp
   modules/jobs/JobsApi.cpp
   modules/mathjax/SessionMathJax.cpp
   modules/panmirror/SessionPanmirror.cpp
   modules/panmirror/SessionPanmirrorBibliography.cpp
   modules/panmirror/SessionPanmirrorCrossref.cpp
   modules/panmirror/SessionPanmirrorDataCite.cpp
   modules/panmirror/SessionPanmirrorDOI.cpp
   modules/panmirror/SessionPanmirrorPandoc.cpp
   modules/panmirror/SessionPanmirrorPubMed.cpp
   modules/panmirror/SessionPanmirrorUtils.cpp
   modules/overlay/SessionOverlay.cpp
   modules/plumber/SessionPlumber.cpp
   modules/presentation/SessionPresentation.cpp
   modules/presentation/PresentationLog.cpp
   modules/presentation/PresentationState.cpp
   modules/presentation/SlideMediaRenderer.cpp
   modules/presentation/SlideNavigationList.cpp
   modules/presentation/SlideParser.cpp
   modules/presentation/SlideQuizRenderer.cpp
   modules/presentation/SlideRenderer.cpp
   modules/presentation/SlideRequestHandler.cpp
   modules/preview/SessionPreview.cpp
   modules/rmarkdown/SessionBlogdown.cpp
   modules/rmarkdown/SessionBookdown.cpp
   modules/rmarkdown/SessionBookdownXRefs.cpp
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
   modules/shiny/ShinyAsyncJob.cpp
   modules/sql/SessionSql.cpp
   modules/stan/SessionStan.cpp
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
   modules/zotero/ZoteroBetterBibTeX.cpp
   modules/zotero/ZoteroCollections.cpp
   modules/zotero/ZoteroCollectionsWeb.cpp
   modules/zotero/ZoteroCollectionsLocal.cpp
   modules/zotero/ZoteroCSL.cpp
   modules/zotero/ZoteroUtil.cpp
   modules/zotero/SessionZotero.cpp
   prefs/PrefLayer.cpp
   prefs/Preferences.cpp
   prefs/UserPrefValues.cpp
   prefs/UserPrefValuesNative.cpp
   prefs/UserPrefs.cpp
   prefs/UserPrefsComputedLayer.cpp
   prefs/UserPrefsDefaultLayer.cpp
   prefs/UserPrefsLayer.cpp
   prefs/UserPrefsProjectLayer.cpp
   prefs/UserPrefsSystemLayer.cpp
   prefs/UserState.cpp
   prefs/UserStateComputedLayer.cpp
   prefs/UserStateDefaultLayer.cpp
   prefs/UserStateLayer.cpp
   prefs/UserStateValues.cpp
   projects/SessionProjects.cpp
   projects/SessionProjectContext.cpp
   projects/SessionProjectFirstRun.cpp
   "${CMAKE_CURRENT_BINARY_DIR}/SessionAddins.cpp"
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
      "${CMAKE_CURRENT_BINARY_DIR}/rsession.rc"
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
set(CORE_INCLUDE_DIRS
    ${CORE_SOURCE_DIR}/include
    ${SERVER_CORE_SOURCE_DIR}/include
    ${SHARED_CORE_SOURCE_DIR}/include
)

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
   ${SERVER_CORE_INCLUDE_DIRS}
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

   if(NOT RSTUDIO_SESSION_WIN32)
      add_subdirectory(consoleio)
   endif()
endif()

# define executable
add_stripped_executable(rsession ${SESSION_SOURCE_FILES} ${SESSION_HEADER_FILES})

# skip libR RPATH at development time
if(RSTUDIO_DEVELOPMENT OR RSTUDIO_RUN_IN_PLACE)
   set_target_properties(rsession PROPERTIES SKIP_BUILD_RPATH TRUE)
endif()

# set link dependencies
if(WIN32)
   set(RSTUDIO_CORE_ZLIB rstudio-core-zlib)
   set(SESSION_SYSTEM_LIBRARIES ${SESSION_SYSTEM_LIBRARIES} AdvAPI32)
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
   ${CMAKE_DL_LIBS}
   ${CRASHPAD_LIBRARIES}
)

if(RSTUDIO_PACKAGE_BUILD AND APPLE)
   target_link_libraries(rsession "-undefined dynamic_lookup")
else()
   target_link_libraries(rsession "${LIBR_LIBRARIES}")
endif()

if (RSTUDIO_SERVER)
   target_link_libraries(rsession
      rstudio-server-core
   )
endif()

# configure and install r-ldpaths script
if(UNIX)

   if(APPLE)
     set(LIBRARY_PATH_ENVVAR "DYLD_FALLBACK_LIBRARY_PATH")
   else()
     set(LIBRARY_PATH_ENVVAR "LD_LIBRARY_PATH")
   endif()

   configure_file(${CMAKE_CURRENT_SOURCE_DIR}/r-ldpath.in
                  ${CMAKE_CURRENT_BINARY_DIR}/r-ldpath
                  @ONLY)
   install(PROGRAMS ${CMAKE_CURRENT_BINARY_DIR}/r-ldpath
           DESTINATION ${RSTUDIO_INSTALL_BIN})
endif()

# install binary
install(TARGETS rsession DESTINATION ${RSTUDIO_INSTALL_BIN})

# include resources, R scripts and binaries if this isn't a session 32-bit build
if (NOT RSTUDIO_SESSION_WIN32)

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
   # Lua resources
   file(GLOB LUA_RESOURCE_FILES "resources/*.lua")
   install(FILES ${LUA_RESOURCE_FILES}
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)
   # CSL resources
   file(GLOB CSL_RESOURCE_FILES "resources/*.csl")
   install(FILES ${CSL_RESOURCE_FILES}
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)
   # presentation
   install(DIRECTORY "resources/presentation"
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)
   # notice
   install(FILES "resources/NOTICE"
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)

   # citation
   configure_file("resources/CITATION.in"
                  "${CMAKE_CURRENT_BINARY_DIR}/CITATION")
   install(FILES "${CMAKE_CURRENT_BINARY_DIR}/CITATION"
           DESTINATION "${RSTUDIO_INSTALL_SUPPORTING}/resources")

   # themes
   file(GLOB THEME_RESOURCE_FILES "resources/themes/*.rstheme" "resources/themes/*.R")
   install(FILES ${THEME_RESOURCE_FILES}
      DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources/themes)
   file(GLOB THEME_CSS_FILES "resources/themes/css/*.css")
   install(FILES ${THEME_CSS_FILES}
      DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources/themes/css)

   # R scripts
   file(GLOB R_MODULE_SRC_FILES "${CMAKE_CURRENT_BINARY_DIR}/modules/R/*.R")
   install(FILES ${R_MODULE_SRC_FILES}
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/R/modules)

   # install hunspell dictionaries
   install(DIRECTORY "${RSTUDIO_DEPENDENCIES_DIR}/common/dictionaries"
           DESTINATION "${RSTUDIO_INSTALL_SUPPORTING}/resources")

   # install mathjax for local html preview
   install(DIRECTORY "${RSTUDIO_DEPENDENCIES_DIR}/common/mathjax-27"
           DESTINATION "${RSTUDIO_INSTALL_SUPPORTING}/resources")

   # icons for database connections
   install(DIRECTORY "resources/connections"
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)

   # JSON schema files
   install(DIRECTORY "resources/schema"
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)

   # dependency data
   install(DIRECTORY "resources/dependencies"
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)

   # install pandoc
   set(PANDOC_VERSION "2.9.2.1" CACHE INTERNAL "Pandoc version")

   set(PANDOC_BIN "${RSTUDIO_DEPENDENCIES_DIR}/common/pandoc/${PANDOC_VERSION}")
   file(GLOB PANDOC_FILES "${PANDOC_BIN}/pandoc*")
   install(FILES ${PANDOC_FILES}
           PERMISSIONS OWNER_READ OWNER_WRITE OWNER_EXECUTE GROUP_READ GROUP_EXECUTE WORLD_READ WORLD_EXECUTE
           DESTINATION  ${RSTUDIO_INSTALL_BIN}/pandoc)

   # install embedded packages
   foreach(PKG ${RSTUDIO_EMBEDDED_PACKAGES})
      file(GLOB PKG_FILES "${RSTUDIO_DEPENDENCIES_DIR}/common/${PKG}*.tar.gz")
      install(
         FILES "${PKG_FILES}"
         DESTINATION "${RSTUDIO_INSTALL_SUPPORTING}/R/packages")
   endforeach()

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

   # install profiler
   install(DIRECTORY "resources/profiler"
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)

   # install tutorials
   install(DIRECTORY "resources/tutorial_resources"
           DESTINATION ${RSTUDIO_INSTALL_SUPPORTING}/resources)

   # install libclang
   if(WIN32)
      file(GLOB LIBCLANG_32_FILES "${LIBCLANG_DIR}/x86/*")
      install(PROGRAMS ${LIBCLANG_32_FILES}
              DESTINATION  ${RSTUDIO_INSTALL_BIN}/rsclang/x86)
      file(GLOB LIBCLANG_64_FILES "${LIBCLANG_DIR}/x86_64/*")
      install(PROGRAMS ${LIBCLANG_64_FILES}
              DESTINATION  ${RSTUDIO_INSTALL_BIN}/rsclang/x86_64)

      # install libclang builtin-headers
      install(DIRECTORY "${RSTUDIO_DEPENDENCIES_DIR}/common/libclang/builtin-headers"
              DESTINATION "${RSTUDIO_INSTALL_SUPPORTING}/resources/libclang")
   endif()

   # install winpty on windows
   if(WIN32)
      install(PROGRAMS "${WINPTY_BINDIR_64}/winpty.dll"
              DESTINATION "${RSTUDIO_INSTALL_BIN}")
      install(PROGRAMS "${WINPTY_BINDIR_64}/winpty-agent.exe"
              DESTINATION "${RSTUDIO_INSTALL_BIN}")

      # install 32 bit binaries
      file(MAKE_DIRECTORY  "${CMAKE_CURRENT_BINARY_DIR}/x86")
      install(DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/x86"
              USE_SOURCE_PERMISSIONS
              DESTINATION  ${RSTUDIO_INSTALL_BIN})
      install(PROGRAMS "${WINPTY_BINDIR_32}/winpty.dll"
              DESTINATION "${RSTUDIO_INSTALL_BIN}/x86")
      install(PROGRAMS "${WINPTY_BINDIR_32}/winpty-agent.exe"
              DESTINATION "${RSTUDIO_INSTALL_BIN}/x86")
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

      install(PROGRAMS "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/sumatra/3.1.2/SumatraPDF.exe"
              DESTINATION  "${RSTUDIO_INSTALL_BIN}/sumatra")
      install(FILES resources/sumatrapdfrestrict.ini
              DESTINATION  "${RSTUDIO_INSTALL_BIN}/sumatra")

      install(PROGRAMS "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/winutils/1.0/winutils.exe"
              DESTINATION ${RSTUDIO_INSTALL_BIN}/winutils)
      install(PROGRAMS "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/winutils/1.0/x64/winutils.exe"
              DESTINATION ${RSTUDIO_INSTALL_BIN}/winutils/x64)
   endif()
endif()

# install 32-bit MSVC runtime for session win32
if(RSTUDIO_SESSION_WIN32)
   include(InstallRequiredSystemLibraries)
   install(FILES ${CMAKE_INSTALL_SYSTEM_RUNTIME_LIBS} DESTINATION ${RSTUDIO_INSTALL_BIN})
endif()

# add overlay if it exists
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/CMakeOverlay.txt")
   include(CMakeOverlay.txt)
endif()

if(APPLE)

   # if doing a package build on MacOS, reroute dependent libraries to our
   # bundled copies
   if (RSTUDIO_PACKAGE_BUILD)
      find_package(OpenSSL REQUIRED QUIET)
      foreach(lib ${OPENSSL_CRYPTO_LIBRARY} ${OPENSSL_SSL_LIBRARY})
         # get the real path to the library (with all symlinks resolved) and filename
         get_filename_component(LIB_PATH "${lib}" REALPATH)
         get_filename_component(LIB_FILE "${LIB_PATH}" NAME)
         # this command does the following:
         # 1. runs otool -L to find the path at which the OpenSSL library is
         #    linked into the executable (this path is unfortunately not canonical
         #    and includes layers of both resolved and unresolved symlinks)
         # 2. extracts the path from the output using grep and awk
         # 3. runs install_name_tool and tells it to replace the path with one that
         #    points to a bundled copy of the same file
         add_custom_command (TARGET rsession POST_BUILD
            COMMAND install_name_tool -change `otool -L ${CMAKE_CURRENT_BINARY_DIR}/rsession | grep ${LIB_FILE} | awk '{ print $$1 }'` @executable_path/../Frameworks/${LIB_FILE} ${CMAKE_CURRENT_BINARY_DIR}/rsession)
      endforeach()

      # do something similar to the above to fix paths to SOCI libraries
      foreach(lib ${SOCI_DEPENDENCIES})
         get_filename_component(LIB_NAME "${lib}" NAME_WE)
         add_custom_command (TARGET rsession POST_BUILD
             COMMAND install_name_tool -change `otool -L ${CMAKE_CURRENT_BINARY_DIR}/rsession | grep ${LIB_NAME} | awk '{ print $$1 }'` @executable_path/../Frameworks/$$\(otool -L ${CMAKE_CURRENT_BINARY_DIR}/rsession | grep ${LIB_NAME} | awk '{ print $$1 }' | xargs basename\) ${CMAKE_CURRENT_BINARY_DIR}/rsession)
      endforeach()

   endif()

endif()
