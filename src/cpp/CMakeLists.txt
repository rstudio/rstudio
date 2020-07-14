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

# set minimum version
cmake_minimum_required(VERSION 3.4.3)

# read release name
include("${CMAKE_CURRENT_SOURCE_DIR}/../../CMakeRelease.txt")

# set project name
project("RStudio [${RSTUDIO_RELEASE_NAME}]")

# tools for reading executable file pre-requisites
include(GetPrerequisites)

# set compiler
include("${CMAKE_CURRENT_SOURCE_DIR}/../../CMakeCompiler.txt")

# include globals. normally these are included at the root but we also
# include them here to support configuring at the src level (i.e. for
# cpp-development-configurations)
include("${CMAKE_CURRENT_SOURCE_DIR}/../../CMakeGlobals.txt")

# global directives
add_definitions(-DBOOST_ENABLE_ASSERT_HANDLER)
add_definitions(-DBOOST_BIND_GLOBAL_PLACEHOLDERS)

# explicitly do not use new c++ 11 features for websocketpp
# they currently do not work with our source
add_definitions(-D_WEBSOCKETPP_NO_CPP11_MEMORY_=1)

# the non-strict masking option was identified as a potentially crashing 
# issue due to reliance on undefined C++ behavior, and was removed entirely in 
# websocketpp 0.7 (RStudio is using 0.5.1 at this time)
add_definitions(-DWEBSOCKETPP_STRICT_MASKING)

# make websockets build with MSVC
if(WIN32 AND MSVC)
   add_definitions(
      -D_WEBSOCKETPP_NOEXCEPT_
      -D_WEBSOCKETPP_CPP11_CHRONO
   )
endif()

# test directory
set(TESTS_INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/tests/cpp" CACHE STRING "Test includes")

# enable c++11
if(NOT MSVC)
   include(CheckCXXCompilerFlag)
   CHECK_CXX_COMPILER_FLAG("-std=c++11" COMPILER_SUPPORTS_CXX11)
   if(NOT COMPILER_SUPPORTS_CXX11)
      message(FATAL_ERROR "The compiler ${CMAKE_CXX_COMPILER} has no C++11 support. Please use a different C++ compiler.")
   else()
      set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")
   endif()
endif()

# UNIX specific global directivies
if(UNIX)
   # cmake modules
   include(CheckFunctionExists REQUIRED)
   include(CheckSymbolExists REQUIRED)

   # compiler flags
   add_definitions(-Wall -pthread -Werror=return-type)

   if(RSTUDIO_ASAN)
      message(STATUS "Compiling with Address Sanitizer (ASAN) support")
      add_definitions(-fsanitize=address -fno-omit-frame-pointer -fno-optimize-sibling-calls -O1)
      set(CMAKE_EXE_LINKER_FLAGS  "${CMAKE_EXE_LINKER_FLAGS} -fsanitize=address")
   endif()

   if(APPLE)
      add_definitions(-Wno-unknown-warning-option -Wsign-compare -Wno-unused-local-typedefs
                      -Wno-deprecated-declarations -fvisibility=hidden)

      find_library(APPLICATION_SERVICES_LIBRARY NAMES ApplicationServices)
      find_library(COCOA_LIBRARY NAMES Cocoa)
      find_library(SECURITY_LIBRARY NAMES Security)

      # additional frameworks needed for crashpad
      find_library(IOKIT_LIBRARY NAMES IOKit)
      find_library(CORE_FOUNDATION_LIBRARY NAMES CoreFoundation)
      find_library(FOUNDATION_LIBRARY NAMES Foundation)
      find_library(BSM_LIBRARY NAMES bsm)
   endif()

   # workaround boost bug (https://svn.boost.org/trac/boost/ticket/4568)
   # by disabling kqueue support. note that this bug was fixed in boost 1.45
   add_definitions(-DBOOST_ASIO_DISABLE_KQUEUE)

   if(APPLE)

      # if present, set osx deployment target variables from environment vars
      if(NOT $ENV{CMAKE_OSX_SYSROOT} STREQUAL "")
         set(CMAKE_OSX_SYSROOT $ENV{CMAKE_OSX_SYSROOT})
         message(STATUS "Set CMAKE_OSX_SYSROOT to ${CMAKE_OSX_SYSROOT}")
      endif()

      if(NOT $ENV{CMAKE_OSX_DEPLOYMENT_TARGET} STREQUAL "")
         set(CMAKE_OSX_DEPLOYMENT_TARGET $ENV{CMAKE_OSX_DEPLOYMENT_TARGET})
         message(STATUS "Set CMAKE_OSX_DEPLOYMENT_TARGET to ${CMAKE_OSX_DEPLOYMENT_TARGET}")
      endif()

      # Figure out what version of Mac OS X this is. Unfortunately 
      # CMAKE_SYSTEM_VERSION does not match uname -r, so get the Mac OS
      # version number another way.
      execute_process(COMMAND /usr/bin/sw_vers -productVersion OUTPUT_VARIABLE MACOSX_VERSION OUTPUT_STRIP_TRAILING_WHITESPACE)

      # When compiling on macOS Mojave (or greater), set a deployment target.
      if(NOT MACOSX_VERSION VERSION_LESS 10.14 AND NOT CMAKE_OSX_DEPLOYMENT_TARGET)
         set(CMAKE_OSX_DEPLOYMENT_TARGET 10.12)
         message(STATUS "Mac OS X deployment target: ${CMAKE_OSX_DEPLOYMENT_TARGET}")
      endif()

      # notify the user of our selection
      message(STATUS "Mac OS X version: ${MACOSX_VERSION}")

   endif()

   # gcc hardending options (see: http://wiki.debian.org/Hardening)
   if(NOT APPLE)
      add_definitions(-Wformat -Wformat-security)
      add_definitions(-D_FORTIFY_SOURCE=2)
      add_definitions(-fstack-protector --param ssp-buffer-size=4)
      add_definitions(-fPIC)
      if(CMAKE_BUILD_TYPE STREQUAL Debug)
         add_definitions(-O0)
         add_definitions(-D_GLIBCXX_ASSERTIONS)
      endif()
      set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -pie -Wl,-z,relro,-z,now")
   endif()

   # other useful gcc diagnostics
   if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
      add_definitions(-Wlogical-op)
      if(CMAKE_CXX_COMPILER_VERSION VERSION_GREATER 7.0.0)
         add_definitions(-Wduplicated-cond)
         add_definitions(-Wduplicated-branches)
         add_definitions(-Wrestrict)
         add_definitions(-Wnull-dereference)
      endif()
   endif()

# Win32 specific global directives
else()

   # be large address aware
   foreach(TYPE EXE MODULE)
      set(CMAKE_${TYPE}_LINKER_FLAGS "${CMAKE_${TYPE}_LINKER_FLAGS} /largeaddressaware")
   endforeach()

   # increase stack size to 16mb
   set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} /stack:0x1000000")

   # allow for large number of sections
   set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} /bigobj")
   set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /bigobj")

   # require Windows-7 SP1
   add_definitions(
      -DNOMINMAX
      -DWINVER=0x601
      -D_WIN32_WINNT=0x601
      -D_WIN32_IE=0x601
      -DWIN32_LEAN_AND_MEAN
      -DBOOST_USE_WINDOWS_H)

   # ensure we use 64-bit APIs for 64-bit builds
   if(NOT RSTUDIO_SESSION_WIN32)
      add_definitions(-D_WIN64)
   endif()
endif()

if(RSTUDIO_PACKAGE_BUILD)
   add_definitions(-DRSTUDIO_PACKAGE_BUILD)
endif()

# determine whether we should statically link boost. we always do this
# unless we are building a non-packaged build on linux (in which case
# boost dynamic libraries are presumed to be installed on the system ldpath)
if(APPLE OR WIN32 OR RSTUDIO_PACKAGE_BUILD)
   set(Boost_USE_STATIC_LIBS ON)
endif()

# default Boost versions
if(NOT RSTUDIO_BOOST_REQUESTED_VERSION)
   set(RSTUDIO_BOOST_REQUESTED_VERSION 1.69.0)
endif()

# disable system boost if we're not using it.
if(NOT RSTUDIO_USE_SYSTEM_BOOST)
   set(Boost_NO_SYSTEM_PATHS ON)
endif()

# disable Boost Signals deprecation warning
add_definitions(-DBOOST_SIGNALS_NO_DEPRECATION_WARNING)

list(APPEND BOOST_LIBS
   atomic
   chrono
   date_time
   filesystem
   iostreams
   program_options
   random
   regex
   system
   thread
)

# UNIX BOOST
if(UNIX)
   # prefer static link to our custom built version
   set(RSTUDIO_TOOLS_BOOST /opt/rstudio-tools/boost/boost_1_69_0)
   if(NOT RSTUDIO_USE_SYSTEM_BOOST AND EXISTS ${RSTUDIO_TOOLS_BOOST})
      add_definitions(-DRSTUDIO_BOOST_NAMESPACE=rstudio_boost)

      # find headers
      set(Boost_USE_STATIC_LIBS ON)
      set(BOOST_INCLUDEDIR  ${RSTUDIO_TOOLS_BOOST}/include)
      find_package(Boost ${RSTUDIO_BOOST_REQUESTED_VERSION} EXACT REQUIRED)
      if(NOT Boost_VERSION LESS 106900)
         list(REMOVE_ITEM BOOST_LIBS signals)
      endif()

      # define library list manually (find_package doesn't always pick them up)
      set(BOOST_LIB_DIR ${RSTUDIO_TOOLS_BOOST}/lib)
      foreach(BOOST_LIB ${BOOST_LIBS})
         list(APPEND Boost_LIBRARIES ${BOOST_LIB_DIR}/libboost_${BOOST_LIB}.a)
      endforeach()
      message(STATUS "Using RStudio-provided Boost ${RSTUDIO_BOOST_REQUESTED_VERSION}")
   else()
      add_definitions(-DRSTUDIO_BOOST_NAMESPACE=boost)
      find_package(Boost ${RSTUDIO_BOOST_REQUESTED_VERSION} REQUIRED)
      if(NOT Boost_VERSION LESS 106900)
         list(REMOVE_ITEM BOOST_LIBS signals)
      endif()
      find_package(Boost ${RSTUDIO_BOOST_REQUESTED_VERSION} REQUIRED COMPONENTS ${BOOST_LIBS})
   endif()

# WIN32 BOOST
else()
   # hard-code to our own prebuilt boost libs
   add_definitions(-DRSTUDIO_BOOST_NAMESPACE=rstudio_boost)
   if(RSTUDIO_SESSION_WIN32)
      set(BOOST_ARCH "32")
   else()
      set(BOOST_ARCH "64")
   endif()
   if(NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
      set(BOOST_SUFFIX "vc141-mt-x${BOOST_ARCH}-1_69.lib")
      set(BOOST_ROOT "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/boost-1.69.0-win-msvc141-release-static/boost${BOOST_ARCH}")
   else()
      set(BOOST_SUFFIX "vc141-mt-gd-x${BOOST_ARCH}-1_69.lib")
      set(BOOST_ROOT "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/boost-1.69.0-win-msvc141-debug-static/boost${BOOST_ARCH}")
   endif()
   set(BOOST_INCLUDEDIR "${BOOST_ROOT}/include/boost-1_69_0")
   find_package(Boost ${RSTUDIO_BOOST_REQUESTED_VERSION} EXACT REQUIRED)
   set(BOOST_LIBRARYDIR "${BOOST_ROOT}/lib")
   foreach(BOOST_LIB ${BOOST_LIBS})
      list(APPEND Boost_LIBRARIES "${BOOST_LIBRARYDIR}/librstudio_boost_${BOOST_LIB}-${BOOST_SUFFIX}")
   endforeach()
   message(STATUS "Using RStudio-provided Boost ${RSTUDIO_BOOST_REQUESTED_VERSION}: ${BOOST_ROOT}")
endif()

# allow opt-in to using Boost.Signals2
# TODO: remove this in RStudio v1.3 and port to signals2
if(NOT RSTUDIO_BOOST_SIGNALS_VERSION)
   if (Boost_VERSION LESS 106900)
      set(RSTUDIO_BOOST_SIGNALS_VERSION 1)
   else()
      set(RSTUDIO_BOOST_SIGNALS_VERSION 2)
   endif()
endif()

message(STATUS "Using Boost.Signals version ${RSTUDIO_BOOST_SIGNALS_VERSION}")
add_definitions(-DRSTUDIO_BOOST_SIGNALS_VERSION=${RSTUDIO_BOOST_SIGNALS_VERSION})

# add boost as system include directory
include_directories(SYSTEM ${Boost_INCLUDE_DIRS})

# Crashpad
if (NOT DEFINED RSTUDIO_CRASHPAD_ENABLED OR RSTUDIO_CRASHPAD_ENABLED)
   if(UNIX)
      set(RSTUDIO_TOOLS_CRASHPAD "/opt/rstudio-tools/crashpad")
      set(CRASHPAD_INCLUDE_DIR "${RSTUDIO_TOOLS_CRASHPAD}")
      set(CRASHPAD_INNER_DIR "${RSTUDIO_TOOLS_CRASHPAD}/crashpad")
      include_directories(SYSTEM ${CRASHPAD_INCLUDE_DIR})
      include_directories(SYSTEM ${CRASHPAD_INNER_DIR})
      include_directories(SYSTEM "${CRASHPAD_INNER_DIR}/third_party/mini_chromium/mini_chromium")
      set(CRASHPAD_OUTPUT_DIR "${CRASHPAD_INNER_DIR}/out/Default")
      set(CRASHPAD_LIBRARY_DIR "${CRASHPAD_OUTPUT_DIR}/obj")
   else()
      if(CMAKE_BUILD_TYPE STREQUAL "Debug")
         set(RSTUDIO_TOOLS_CRASHPAD "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/crashpad-debug")
      else()
         set(RSTUDIO_TOOLS_CRASHPAD "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/crashpad-release")
      endif()
      if(RSTUDIO_SESSION_WIN32)
         set(CRASHPAD_ARCH "x86")
      else()
         set(CRASHPAD_ARCH "x64")
      endif()
      set(CRASHPAD_INCLUDE_DIR "${RSTUDIO_TOOLS_CRASHPAD}/include")
      include_directories(SYSTEM ${CRASHPAD_INCLUDE_DIR})
      include_directories(SYSTEM "${CRASHPAD_INCLUDE_DIR}/crashpad")
      include_directories(SYSTEM "${CRASHPAD_INCLUDE_DIR}/mini_chromium")
      set(CRASHPAD_OUTPUT_DIR "${RSTUDIO_TOOLS_CRASHPAD}/bin")
      set(CRASHPAD_LIBRARY_DIR "${RSTUDIO_TOOLS_CRASHPAD}/${CRASHPAD_ARCH}/lib_md")
   endif()

   # find crashpad libraries, ensuring the correct link order
   if(UNIX)
      file(GLOB_RECURSE CRASHPAD_LIBCLIENT "${CRASHPAD_LIBRARY_DIR}/*libclient.a")
      file(GLOB_RECURSE CRASHPAD_LIBUTIL   "${CRASHPAD_LIBRARY_DIR}/*libutil.a")
      file(GLOB_RECURSE CRASHPAD_LIBBASE   "${CRASHPAD_LIBRARY_DIR}/*libbase.a")
      list (APPEND CRASHPAD_LIBRARIES ${CRASHPAD_LIBCLIENT} ${CRASHPAD_LIBUTIL} ${CRASHPAD_LIBBASE})

      if(APPLE)
         # add necessary macos frameworks so crashpad applications can link correctly
         list(APPEND CRASHPAD_LIBRARIES ${APPLICATION_SERVICES_LIBRARY})
         list(APPEND CRASHPAD_LIBRARIES ${CORE_FOUNDATION_LIBRARY})
         list(APPEND CRASHPAD_LIBRARIES ${FOUNDATION_LIBRARY})
         list(APPEND CRASHPAD_LIBRARIES ${IOKIT_LIBRARY})
         list(APPEND CRASHPAD_LIBRARIES ${SECURITY_LIBRARY})
         list(APPEND CRASHPAD_LIBRARIES ${BSM_LIBRARY})
      endif()
   else()
      file(GLOB_RECURSE CRASHPAD_LIBCLIENT "${CRASHPAD_LIBRARY_DIR}/*client.lib")
      file(GLOB_RECURSE CRASHPAD_LIBUTIL   "${CRASHPAD_LIBRARY_DIR}/*util.lib")
      file(GLOB_RECURSE CRASHPAD_LIBBASE   "${CRASHPAD_LIBRARY_DIR}/*base.lib")
      list (APPEND CRASHPAD_LIBRARIES ${CRASHPAD_LIBCLIENT} ${CRASHPAD_LIBUTIL} ${CRASHPAD_LIBBASE})
   endif()
   list(LENGTH CRASHPAD_LIBRARIES CRASHPAD_LIB_COUNT)

   if (CRASHPAD_LIB_COUNT EQUAL 0)
      message(STATUS "No Crashpad libraries found under ${CRASHPAD_LIBRARY_DIR}. Crashpad integration disabled.")
   else()
      message(STATUS "Crashpad libraries found under ${CRASHPAD_LIBRARY_DIR}. Crashpad integration enabled.")
      add_definitions(-DRSTUDIO_CRASHPAD_ENABLED=1)

      # ensure the desired crashpad binaries are installed with the installation package
      if(UNIX)
         install(PROGRAMS "${CRASHPAD_OUTPUT_DIR}/crashpad_handler" DESTINATION ${RSTUDIO_INSTALL_BIN})
         install(PROGRAMS "${CRASHPAD_OUTPUT_DIR}/crashpad_http_upload" DESTINATION ${RSTUDIO_INSTALL_BIN})
      elseif(NOT RSTUDIO_SESSION_WIN32)
         install(PROGRAMS "${CRASHPAD_OUTPUT_DIR}/crashpad_handler.com" DESTINATION ${RSTUDIO_INSTALL_BIN})
         install(PROGRAMS "${CRASHPAD_OUTPUT_DIR}/crashpad_handler.exe" DESTINATION ${RSTUDIO_INSTALL_BIN})
         install(PROGRAMS "${CRASHPAD_OUTPUT_DIR}/crashpad_http_upload.exe" DESTINATION ${RSTUDIO_INSTALL_BIN})
      endif()
   endif()
else()
   message(STATUS "Crashpad integration disabled via build parameters")
endif()

# SOCI
if(UNIX)
   set(RSTUDIO_TOOLS_SOCI "/opt/rstudio-tools/soci")
   set(SOCI_INCLUDE_BUILD_DIR "${RSTUDIO_TOOLS_SOCI}/build/include")
else()
   set(RSTUDIO_TOOLS_SOCI "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/install-soci/soci")
   if(RSTUDIO_SESSION_WIN32)
      set(SOCI_ARCH "x86")
   else()
      set(SOCI_ARCH "x64")
   endif()
   set(SOCI_INCLUDE_BUILD_DIR "${RSTUDIO_TOOLS_SOCI}/build/${SOCI_ARCH}/include")
endif()

set(SOCI_INCLUDE_DIR "${RSTUDIO_TOOLS_SOCI}/include")
if(NOT RSTUDIO_USE_SYSTEM_SOCI)
   include_directories(SYSTEM ${SOCI_INCLUDE_DIR})
   include_directories(SYSTEM ${SOCI_INCLUDE_BUILD_DIR})
endif()

# database library includes
if (UNIX)
   if (APPLE)
      # We assume that libpq is available under the default brew install location
      # since we install postgresql via brew in install-dependencies-osx
      include_directories(SYSTEM "/usr/local/opt/postgresql/include")
      include_directories(SYSTEM "/usr/local/opt/postgresql/include/libpq")
   else()
      include_directories(SYSTEM "/usr/include/postgresql")
      include_directories(SYSTEM "/usr/include/pgsql")
   endif()
elseif(WIN32)
   include_directories(SYSTEM "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/install-soci/sqlite/sqlite-amalgamation-3310100")
   include_directories(SYSTEM "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/install-soci/postgresql/include")
endif()

# find SOCI libraries
if(UNIX)
   set(SOCI_LIBRARY_DIR "${RSTUDIO_TOOLS_SOCI}/build/lib")
   if(NOT APPLE AND RSTUDIO_USE_SYSTEM_SOCI)
      set(SOCI_LIBRARY_DIR "/usr/lib")
   endif()
   find_library(SOCI_CORE_LIB NAMES "libsoci_core.a" "soci_core" PATHS "${SOCI_LIBRARY_DIR}" NO_DEFAULT_PATH)
   find_library(SOCI_SQLITE_LIB NAMES "libsoci_sqlite3.a" "soci_sqlite3" PATHS "${SOCI_LIBRARY_DIR}" NO_DEFAULT_PATH)
   find_library(SOCI_POSTGRESQL_LIB NAMES "libsoci_postgresql.a" "soci_postgresql" PATHS "${SOCI_LIBRARY_DIR}" NO_DEFAULT_PATH)
   find_library(DL_LIB "dl")
   find_library(SQLITE_LIB "sqlite3")
   get_filename_component(SQLITE_LIB "${SQLITE_LIB}" REALPATH)
   find_library(PQ_LIB "pq")
   get_filename_component(PQ_LIB "${PQ_LIB}" REALPATH)
   list(APPEND SOCI_DEPENDENCIES ${SQLITE_LIB} ${PQ_LIB})
   list(APPEND SOCI_LIBRARIES ${SOCI_SQLITE_LIB} ${SOCI_POSTGRESQL_LIB} ${SQLITE_LIB} ${PQ_LIB} ${SOCI_CORE_LIB} ${DL_LIB})
else()
   set(SOCI_LIBRARY_DIR "${RSTUDIO_TOOLS_SOCI}/build/${SOCI_ARCH}/lib")
   if(CMAKE_BUILD_TYPE STREQUAL "Debug")
       file(GLOB_RECURSE SOCI_LIBRARIES "${SOCI_LIBRARY_DIR}/Debug/*.lib")
       list(APPEND SOCI_LIBRARIES "Wldap32")
       list(APPEND SOCI_LIBRARIES "Secur32")
       list(APPEND SOCI_LIBRARIES "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/install-soci/sqlite/sqlite3-debug-${SOCI_ARCH}.lib")
       list(APPEND SOCI_LIBRARIES "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/install-soci/postgresql/lib/${SOCI_ARCH}/Debug/libpq.lib")
       list(APPEND SOCI_LIBRARIES "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/install-soci/postgresql/lib/${SOCI_ARCH}/Debug/libpgcommon.lib")
       list(APPEND SOCI_LIBRARIES "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/install-soci/postgresql/lib/${SOCI_ARCH}/Debug/libpgport.lib")
   else()
       file(GLOB_RECURSE SOCI_LIBRARIES "${SOCI_LIBRARY_DIR}/Release/*.lib")
       list(APPEND SOCI_LIBRARIES "Wldap32")
       list(APPEND SOCI_LIBRARIES "Secur32")
       list(APPEND SOCI_LIBRARIES "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/install-soci/sqlite/sqlite3-release-${SOCI_ARCH}.lib")
       list(APPEND SOCI_LIBRARIES "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/install-soci/postgresql/lib/${SOCI_ARCH}/Release/libpq.lib")
       list(APPEND SOCI_LIBRARIES "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/install-soci/postgresql/lib/${SOCI_ARCH}/Release/libpgcommon.lib")
       list(APPEND SOCI_LIBRARIES "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/install-soci/postgresql/lib/${SOCI_ARCH}/Release/libpgport.lib")
   endif()
endif()
list(LENGTH SOCI_LIBRARIES SOCI_LIB_COUNT)

if (SOCI_LIB_COUNT EQUAL 0)
   message(FATAL_ERROR "No SOCI libraries found under ${SOCI_LIBRARY_DIR}. Ensure the SOCI dependency is installed and try again.")
endif()

message(STATUS "SOCI libraries found under ${SOCI_LIBRARY_DIR}")

if(WIN32)
     # winpty - pseudoterminal support on Windows
     if(RSTUDIO_SESSION_WIN32)
        set(WINPTY_ARCH "32")
     else()
        set(WINPTY_ARCH "64")
     endif()
     set(WINPTY_ROOT "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/winpty-0.4.3-msys2-2.7.0")
     set(WINPTY_INCLUDEDIR "${WINPTY_ROOT}/${WINPTY_ARCH}/include")
     set(WINPTY_BINDIR_32 "${WINPTY_ROOT}/32/bin")
     set(WINPTY_BINDIR_64 "${WINPTY_ROOT}/64/bin")

     # add winpty as system include directory
     include_directories(SYSTEM ${WINPTY_INCLUDEDIR})

     # openssl for Windows
     if(CMAKE_BUILD_TYPE STREQUAL "Debug")
        set(OPENSSL_BUILD_TYPE "debug")
     else()
        set(OPENSSL_BUILD_TYPE "release")
     endif()

     if(RSTUDIO_SESSION_WIN32)
        set(OPENSSL_BITNESS "32")
     else()
        set(OPENSSL_BITNESS "64")
     endif()

     set(OPENSSL_NAME "openssl-1.1.1d")
     set(OPENSSL_ROOT_DIR "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/${OPENSSL_NAME}/${OPENSSL_NAME}-${OPENSSL_BUILD_TYPE}-${OPENSSL_BITNESS}")
     set(OPENSSL_INCLUDE_DIR "${OPENSSL_ROOT_DIR}/include")
     set(OPENSSL_LIBRARY_DIR "${OPENSSL_ROOT_DIR}/lib")
     set(OPENSSL_CRYPTO_LIBRARY "${OPENSSL_LIBRARY_DIR}/libcrypto.lib")
     set(OPENSSL_SSL_LIBRARY "${OPENSSL_LIBRARY_DIR}/libssl.lib")
     list(APPEND OPENSSL_LIBRARIES "${OPENSSL_CRYPTO_LIBRARY}")
     list(APPEND OPENSSL_LIBRARIES "${OPENSSL_SSL_LIBRARY}")

     include_directories(SYSTEM ${OPENSSL_INCLUDE_DIR})
endif()

# automatically build addins found in the addins subdirectory
# (if another path wasn't already specified)
if(NOT RSTUDIO_ADDINS_PATH)
   set(RSTUDIO_DEFAULT_ADDINS_PATH "${CMAKE_CURRENT_SOURCE_DIR}/addins")
   if(EXISTS ${RSTUDIO_DEFAULT_ADDINS_PATH})
      set(RSTUDIO_ADDINS_PATH ${RSTUDIO_DEFAULT_ADDINS_PATH})
   endif()
endif()

# external libraries
add_subdirectory(ext)

# shared library
add_subdirectory(shared_core)

# core library
add_subdirectory(core)

# server core library
if (RSTUDIO_SERVER)
   add_subdirectory(server_core)
endif()

# echo configuration modes
if(RSTUDIO_DEVELOPMENT)
   message(STATUS "Configured to build DEVELOPMENT")
endif()
if(RSTUDIO_SERVER)
   message(STATUS "Configured to build SERVER")
endif()
if (RSTUDIO_DESKTOP)
   message(STATUS "Configured to build DESKTOP")
endif()
if(RSTUDIO_CONFIG_CORE_DEV)
   message(STATUS "Configured to build CORE_DEV")
endif()
if(RSTUDIO_SESSION_WIN32)
   message(STATUS "Configured to build SESSION_WIN32")
endif()
if(RSTUDIO_CONFIG_MONITOR_ONLY)
   message(STATUS "Configured to build MONITOR_ONLY")
elseif(RSTUDIO_CONFIG_SESSION_ONLY)
   message(STATUS, "Configured to build SESSION_ONLY")
endif()


# are we in CORE_DEV mode? if so then just add the core/dev project
# otherwise, add the rest of our projects
if(RSTUDIO_CONFIG_CORE_DEV)

   add_subdirectory(core/dev)

else()

   # monitor library
   add_subdirectory(monitor)

   # add overlay if it exists
   if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/CMakeOverlay.txt")
      include(CMakeOverlay.txt)
   endif()

   # proceed if we aren't building rstudio monitor-only
   if(NOT RSTUDIO_CONFIG_MONITOR_ONLY)

      # find LibR
      if(RSTUDIO_SESSION_WIN32)
         set(LIBR_FIND_WINDOWS_32BIT TRUE)
      endif()
      find_package(LibR REQUIRED)

      # verify we got the required R version
      if(LIBR_FOUND AND RSTUDIO_VERIFY_R_VERSION)
         set(CMAKE_REQUIRED_INCLUDES ${LIBR_INCLUDE_DIRS})
         execute_process(
             COMMAND "${LIBR_EXECUTABLE}" "--vanilla" "--slave" "-e" "cat(as.character(getRversion()))"
             OUTPUT_VARIABLE LIBR_VERSION)
         if (LIBR_VERSION VERSION_LESS "3.0.1")
            message(FATAL_ERROR "Minimum R version (${RSTUDIO_R_MAJOR_VERSION_REQUIRED}.${RSTUDIO_R_MINOR_VERSION_REQUIRED}.${RSTUDIO_R_PATCH_VERSION_REQUIRED}) not found.")
         endif()
      endif()

      # r library
      add_subdirectory(r)

      # initialize subdirectories
      file(MAKE_DIRECTORY conf)

      # test runner script
      if(NOT WIN32)
         configure_file(rstudio-tests.in ${CMAKE_CURRENT_BINARY_DIR}/rstudio-tests)
      endif()

      # add desktop subprojects if we aren't building in server only mode
      if(RSTUDIO_DESKTOP)
         add_subdirectory(diagnostics)
         add_subdirectory(desktop)
      endif()

      # add this after desktop so it is not included in fixup_bundle
      # processing which we do in desktop
      add_subdirectory(session)

      # configure desktop files
      if(RSTUDIO_DESKTOP)
         configure_file(conf/rdesktop-dev.conf ${CMAKE_CURRENT_BINARY_DIR}/conf/rdesktop-dev.conf)
         if(NOT WIN32)
            if(NOT APPLE)
               configure_file(rstudio-dev.in ${CMAKE_CURRENT_BINARY_DIR}/rstudio-dev)
            else()
               configure_file(rstudio-mac.in ${CMAKE_CURRENT_BINARY_DIR}/rstudio-dev)
            endif()
         else()
            configure_file(rstudio.bat.in ${CMAKE_CURRENT_BINARY_DIR}/rstudio.bat @ONLY)
            configure_file(rstudio-tests.bat.in ${CMAKE_CURRENT_BINARY_DIR}/rstudio-tests.bat @ONLY)
         endif()
      endif()

      # add server subprojects if we aren't building in desktop only mode
      if(RSTUDIO_SERVER)
         add_subdirectory(server)
         configure_file(rserver-dev.in ${CMAKE_CURRENT_BINARY_DIR}/rserver-dev)
         configure_file(conf/rserver-dev.conf ${CMAKE_CURRENT_BINARY_DIR}/conf/rserver-dev.conf)
         configure_file(conf/rsession-dev.conf ${CMAKE_CURRENT_BINARY_DIR}/conf/rsession-dev.conf)
         configure_file(conf/database-dev.conf ${CMAKE_CURRENT_BINARY_DIR}/conf/database-dev.conf)
      endif()
   endif()

endif()

