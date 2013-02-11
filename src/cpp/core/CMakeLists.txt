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

project (CORE)

add_subdirectory(spelling/hunspell)
add_subdirectory(tex/synctex)

# include files
file(GLOB_RECURSE CORE_HEADER_FILES "*.h*")

# source files
set (CORE_SOURCE_FILES
   Assert.cpp
   Base64.cpp
   BoostErrors.cpp
   ConfigUtils.cpp
   DateTime.cpp
   Error.cpp 
   Exec.cpp
   FileInfo.cpp 
   FileLock.cpp
   FileLogWriter.cpp
   FilePath.cpp
   FileSerializer.cpp
   FileUtils.cpp
   GitGraph.cpp
   Hash.cpp
   HtmlUtils.cpp
   Log.cpp
   LogWriter.cpp
   PerformanceTimer.cpp
   ProgramOptions.cpp
   RegexUtils.cpp
   SafeConvert.cpp
   Settings.cpp
   StderrLogWriter.cpp
   StringUtils.cpp
   Thread.cpp
   WaitUtils.cpp
   gwt/GwtFileHandler.cpp
   gwt/GwtLogHandler.cpp
   json/Json.cpp
   json/JsonRpc.cpp
   json/spirit/json_spirit_reader.cpp
   json/spirit/json_spirit_value.cpp
   json/spirit/json_spirit_writer.cpp
   http/Cookie.cpp
   http/Header.cpp
   http/Message.cpp
   http/MultipartRelated.cpp
   http/Request.cpp
   http/RequestParser.cpp
   http/Response.cpp
   http/URL.cpp
   http/UriHandler.cpp
   http/Util.cpp
   markdown/Markdown.cpp
   markdown/MathJax.cpp
   markdown/sundown/autolink.c
   markdown/sundown/buffer.c
   markdown/sundown/houdini_href_e.c
   markdown/sundown/houdini_html_e.c
   markdown/sundown/html.c
   markdown/sundown/html_smartypants.c
   markdown/sundown/markdown.c
   markdown/sundown/stack.c
   r_util/RPackageInfo.cpp
   r_util/RProjectFile.cpp
   r_util/RTokenizer.cpp
   r_util/RSourceIndex.cpp
   r_util/RTokenizerTests.cpp
   spelling/HunspellCustomDictionaries.cpp
   spelling/HunspellDictionaryManager.cpp
   spelling/HunspellSpellingEngine.cpp
   system/Environment.cpp
   system/Process.cpp
   system/ShellUtils.cpp
   system/System.cpp
   system/file_monitor/FileMonitor.cpp
   tex/TexLogParser.cpp
   tex/TexMagicComment.cpp
   tex/TexSynctex.cpp
   text/DcfParser.cpp
   text/TemplateFilter.cpp
)

# UNIX specific
if (UNIX)

   include (CheckCXXSourceCompiles)
   CHECK_CXX_SOURCE_COMPILES (
   "# include <dirent.h>
   int func (const char *d, dirent ***list, void *sort)
   {
      int n = scandir(d, list, 0, (int(*)(const dirent **, const dirent **))sort);
      return n;
   }

   int main (int, char **)
   {
      return 0;
   }
   "
   HAVE_SCANDIR_POSIX)

   # platform introspection
   check_symbol_exists(SA_NOCLDWAIT "signal.h" HAVE_SA_NOCLDWAIT)
   check_symbol_exists(SO_PEERCRED "sys/socket.h" HAVE_SO_PEERCRED)
   check_function_exists(inotify_init1 HAVE_INOTIFY_INIT1)
   check_function_exists(getpeereid HAVE_GETPEEREID)
   check_function_exists(setresuid HAVE_SETRESUID)
   if(EXISTS "/proc/self")
      set(HAVE_PROCSELF TRUE)
   endif()
   configure_file (${CMAKE_CURRENT_SOURCE_DIR}/config.h.in
                   ${CMAKE_CURRENT_BINARY_DIR}/config.h)

   # find packages and libraries
   find_library(PTHREAD_LIBRARIES pthread)
   if(NOT APPLE)
      find_library(UTIL_LIBRARIES util)
      find_library(UUID_LIBRARIES uuid)
      find_library(RT_LIBRARIES rt)
   endif()
   find_package(ZLIB REQUIRED QUIET)

   # find apple frameworks we depend on
   if(APPLE)
      find_library(CORE_SERVICES_LIBRARY NAMES CoreServices)
   endif()

   # include directories and libraries
   set (CORE_SYSTEM_LIBRARIES
      ${PTHREAD_LIBRARIES}
      ${UTIL_LIBRARIES}
      ${UUID_LIBRARIES}
      ${RT_LIBRARIES}
      ${ZLIB_LIBRARIES}
      ${CORE_SERVICES_LIBRARY}
   )

   if(RSTUDIO_SERVER)
      # require openssl (for crypto)
      find_package(OpenSSL REQUIRED QUIET)
      set (CORE_SYSTEM_LIBRARIES
         ${CORE_SYSTEM_LIBRARIES}
         ${OPENSSL_LIBRARIES}
      )
      set(CORE_INCLUDE_DIRS ${OPENSSL_INCLUDE_DIRS})
   endif()

   # source files
   set(CORE_SOURCE_FILES ${CORE_SOURCE_FILES}
      ${DIRECTORY_MONITOR_CPP}
      PosixStringUtils.cpp
      r_util/REnvironmentPosix.cpp
      SyslogLogWriter.cpp
      system/PosixEnvironment.cpp
      system/PosixFileScanner.cpp
      system/PosixLibraryLoader.cpp
      system/PosixParentProcessMonitor.cpp
      system/PosixOutputCapture.cpp
      system/PosixShellUtils.cpp
      system/PosixSystem.cpp
      system/PosixUser.cpp
      system/PosixChildProcess.cpp
   )

   if(RSTUDIO_SERVER)
      set(CORE_SOURCE_FILES ${CORE_SOURCE_FILES}
         system/PosixCrypto.cpp
      )
   endif()

   if(APPLE)
      set(CORE_SOURCE_FILES ${CORE_SOURCE_FILES}
         system/file_monitor/MacFileMonitor.cpp
         system/recycle_bin/MacRecycleBin.cpp
      )
   else()
      set(CORE_SOURCE_FILES ${CORE_SOURCE_FILES}
         system/file_monitor/LinuxFileMonitor.cpp
         system/recycle_bin/LinuxRecycleBin.cpp
      )
   endif()

# Win32 specific
else()

   # embedded version of zlib
   add_subdirectory(zlib)

   # system libraries
   set (CORE_SYSTEM_LIBRARIES -lws2_32 -lmswsock -lrpcrt4 -lShlwapi)

   # source files
   set(CORE_SOURCE_FILES ${CORE_SOURCE_FILES}
      Win32StringUtils.cpp
      system/Win32FileScanner.cpp
      system/RegistryKey.cpp
      system/Win32Environment.cpp
      system/Win32LibraryLoader.cpp
      system/Win32ParentProcessMonitor.cpp
      system/Win32OutputCapture.cpp
      system/Win32ShellUtils.cpp
      system/Win32System.cpp
      system/Win32ChildProcess.cpp
      system/file_monitor/Win32FileMonitor.cpp
      system/recycle_bin/Win32RecycleBin.cpp
      r_util/RToolsInfo.cpp
   )

endif()

# define include dirs
set(CORE_INCLUDE_DIRS ${CORE_INCLUDE_DIRS} include)

# search for core addins
if(RSTUDIO_ADDINS_PATH)
   set(CORE_ADDIN_PATH ${RSTUDIO_ADDINS_PATH}/core)
   if(EXISTS ${CORE_ADDIN_PATH})
      file(GLOB_RECURSE ADDIN_HEADER_FILES "${CORE_ADDIN_PATH}/*.h*")
      list(APPEND CORE_HEADER_FILES ${ADDIN_HEADER_FILES})
      file(GLOB_RECURSE ADDIN_SOURCE_FILES "${CORE_ADDIN_PATH}/*.c*")
      list(APPEND CORE_SOURCE_FILES ${ADDIN_SOURCE_FILES})
      list(APPEND CORE_INCLUDE_DIRS ${CORE_ADDIN_PATH}/include)
   endif()
endif()


# include directories
include_directories(
   ${CORE_INCLUDE_DIRS}
   ${CMAKE_CURRENT_BINARY_DIR}
)

# define shared library
add_library(rstudio-core STATIC ${CORE_SOURCE_FILES} ${CORE_HEADER_FILES})

# link dependencies
target_link_libraries(
   rstudio-core
   ${Boost_LIBRARIES}
   ${CORE_SYSTEM_LIBRARIES}
)
